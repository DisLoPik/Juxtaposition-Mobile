package com.dislopik.juxtaposition.data

import com.dislopik.juxtaposition.model.Community
import com.dislopik.juxtaposition.model.CommunityDetail
import com.dislopik.juxtaposition.model.CommunitySort
import com.dislopik.juxtaposition.model.Feeling
import com.dislopik.juxtaposition.model.FeedType
import com.dislopik.juxtaposition.model.JuxtNotification
import com.dislopik.juxtaposition.model.PostPage
import com.dislopik.juxtaposition.model.PostThread
import com.dislopik.juxtaposition.model.ProfileTab
import com.dislopik.juxtaposition.model.ReportReason
import com.dislopik.juxtaposition.model.SelfUser
import com.dislopik.juxtaposition.model.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.decodeURLQueryComponent
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Thrown when the stored session is missing, expired or rejected. */
class JuxtAuthException(message: String = "Signed out") : Exception(message)

/** Thrown for expected, user-facing failures (rate limits, automod rejections, ...). */
class JuxtException(message: String) : Exception(message)

data class EmpathyResult(val postId: String, val count: Int, val yeahed: Boolean)

/**
 * Client for the Juxtaposition web frontend.
 *
 * Juxtaposition's own JSON API is reachable only over gRPC from its web server, so, exactly
 * like the Juxtaposition-Enhancer userscript does in the browser, this drives the public web
 * app: HTML form posts for writes, and server-rendered HTML for reads.
 */
class JuxtApi(
    private val client: HttpClient,
    private val tokenStore: TokenStore
) {
    var accessToken: String? = tokenStore.load()
        private set

    val isSignedIn: Boolean get() = accessToken != null

    private val json = Json { ignoreUnknownKeys = true }

    // ---------------------------------------------------------------- auth

    suspend fun login(username: String, password: String) {
        val response = client.post("$BASE_URL/login") {
            browserHeaders()
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("username", username)
                        append("password", password)
                        append("grant_type", "password")
                        append("redirect", "/")
                    }
                )
            )
        }

        val token = response.setCookie("access_token")
        if (token != null) {
            accessToken = token
            tokenStore.save(token)
            return
        }

        // A failed login re-renders the login page with the reason in a toast.
        val message = JuxtHtml.parseLoginError(response.bodyAsText())
        throw JuxtAuthException(message ?: "Login failed. Check your username and password.")
    }

    fun logout() {
        accessToken = null
        tokenStore.save(null)
    }

    // ---------------------------------------------------------------- reads

    suspend fun feed(type: FeedType): PostPage =
        JuxtHtml.parsePostPage(getHtml("${type.path}?pjax=true&offset=0"))

    suspend fun communityPosts(communityId: String, sort: CommunitySort): PostPage =
        JuxtHtml.parsePostPage(getHtml("/titles/$communityId/${sort.segment}?pjax=true&offset=0"))

    /** Follows the "load more" link the server itself rendered, so paging stays in sync with it. */
    suspend fun morePosts(nextLink: String): PostPage = JuxtHtml.parsePostPage(getHtml(nextLink))

    suspend fun communities(): List<Community> = JuxtHtml.parseCommunityList(getHtml("/titles/all"))

    suspend fun communityDetail(communityId: String): CommunityDetail =
        JuxtHtml.parseCommunityDetail(getHtml("/titles/$communityId/new"), communityId)

    /** The signed-in user's PID, read from the navbar's link to their own page. */
    suspend fun loadSelf(): SelfUser? = JuxtHtml.parseSelf(getHtml("/titles"))

    /** A profile page carries both the profile and that user's first page of posts. */
    suspend fun userProfile(pid: Long?): Pair<UserProfile, PostPage> {
        val html = getHtml(if (pid == null) "/users/me" else "/users/$pid")
        val profile = JuxtHtml.parseUserProfile(html)
            ?: throw JuxtException("That profile is not available.")
        return profile to JuxtHtml.parsePostPage(html)
    }

    suspend fun userPosts(pid: Long?, tab: ProfileTab): PostPage {
        val base = if (pid == null) "/users/me/" else "/users/$pid/"
        return JuxtHtml.parsePostPage(getHtml("$base${tab.segment}?pjax=true&offset=0"))
    }

    /** The notifications page marks everything read as a side effect, same as the website. */
    suspend fun notifications(): List<JuxtNotification> =
        JuxtHtml.parseNotifications(getHtml("/news/my_news?pjax=true"))

    suspend fun thread(postId: String): PostThread =
        JuxtHtml.parseThread(getHtml("/posts/$postId"))
            ?: throw JuxtException("This post is no longer available.")

    // ---------------------------------------------------------------- writes

    /** Toggles the "Yeah" on a post. The server decides add vs remove from the current state. */
    suspend fun toggleYeah(postId: String, currentlyYeahed: Boolean): EmpathyResult {
        val response = postForm("/posts/empathy") {
            append("postID", postId)
        }
        val obj = runCatching { json.parseToJsonElement(response.bodyAsText()).jsonObject }.getOrNull()
            ?: throw JuxtException("Could not register your Yeah.")

        when (obj["status"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0) {
            200 -> Unit
            423 -> throw JuxtException("You are doing that too fast. Try again in a moment.")
            else -> throw JuxtException("Could not register your Yeah.")
        }

        val count = obj["count"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
        return EmpathyResult(postId = postId, count = count, yeahed = !currentlyYeahed)
    }

    suspend fun createPost(
        communityId: String,
        body: String,
        feeling: Feeling,
        spoiler: Boolean,
        painting: String? = null
    ) = submitPost("/posts/new", communityId, body, feeling, spoiler, painting)

    suspend fun reply(
        parentPostId: String,
        communityId: String,
        body: String,
        feeling: Feeling,
        spoiler: Boolean,
        painting: String? = null
    ) = submitPost("/posts/$parentPostId/new", communityId, body, feeling, spoiler, painting)

    suspend fun toggleFollowUser(pid: Long): Boolean {
        val response = postForm("/users/follow") {
            append("id", pid.toString())
        }
        val obj = runCatching { json.parseToJsonElement(response.bodyAsText()).jsonObject }.getOrNull()
            ?: throw JuxtException("Could not update follow state.")
        return obj["action"]?.jsonPrimitive?.content == "follow"
    }

    suspend fun reportPost(postId: String, reason: ReportReason, message: String) {
        // `api=true` makes the server answer 200 instead of redirecting back to the post.
        val response = postForm("/posts/$postId/report?api=true") {
            append("post_id", postId)
            append("reason", reason.id.toString())
            append("message", message.trim())
        }
        if (!response.status.isSuccess()) {
            throw JuxtException("Could not send the report (${response.status.value}).")
        }
    }

    /**
     * Deletes a post owned by the signed-in account. The server checks ownership itself,
     * and answers with the path to go to next.
     */
    suspend fun deletePost(postId: String) {
        requireToken()
        val response = client.delete(absolute("/posts/$postId")) { browserHeaders(auth = true) }
        checkSession(response)
        if (response.status.value == 404) {
            throw JuxtException("That post is already gone.")
        }
        if (!response.status.isSuccess()) {
            throw JuxtException("Could not delete the post (${response.status.value}).")
        }
    }

    suspend fun toggleFollowCommunity(communityId: String): Boolean {
        val response = postForm("/titles/follow") {
            append("id", communityId)
        }
        val obj = runCatching { json.parseToJsonElement(response.bodyAsText()).jsonObject }.getOrNull()
            ?: throw JuxtException("Could not update follow state.")
        return obj["action"]?.jsonPrimitive?.content == "follow"
    }

    private suspend fun submitPost(
        path: String,
        communityId: String,
        body: String,
        feeling: Feeling,
        spoiler: Boolean,
        painting: String?
    ) {
        // The server accepts a post with text, a drawing, or both, but not neither.
        if (body.isBlank() && painting == null) throw JuxtException("Write or draw something first.")
        if (body.length > MAX_POST_LENGTH) {
            throw JuxtException("Posts are limited to $MAX_POST_LENGTH characters.")
        }

        val response = postForm(path) {
            append("community_id", communityId)
            append("body", body)
            append("_post_type", if (painting != null) "painting" else "body")
            append("feeling_id", feeling.id.toString())
            append("spoiler", spoiler.toString())
            append("is_app_jumpable", "false")
            if (painting != null) {
                append("painting", painting)
                append("bmp", "false") // the blob is TGA-zlib, not BMP
            }
        }

        // Success redirects to the post or community; a rejection bounces back to the composer.
        val location = response.headers[HttpHeaders.Location]
        if (location != null && location.contains("/create")) {
            val reason = location.substringAfter("error-text=", "")
                .takeIf { it.isNotBlank() }
                ?.let { runCatching { it.decodeURLQueryComponent(plusIsSpace = true) }.getOrDefault(it) }
            throw JuxtException(reason ?: "Your post was rejected. Try rewording it.")
        }
        if (location == null && !response.status.isSuccess()) {
            throw JuxtException("Could not publish your post (${response.status.value}).")
        }
    }

    // ---------------------------------------------------------------- plumbing

    private suspend fun getHtml(path: String): String {
        requireToken()
        val response = client.get(absolute(path)) { browserHeaders(auth = true) }
        checkSession(response)
        if (!response.status.isSuccess()) {
            throw JuxtException("Juxtaposition returned ${response.status.value}.")
        }
        val body = response.bodyAsText()
        // A stale cookie can still render the login page with a 200.
        if (JuxtHtml.isLoginPage(body)) {
            logout()
            throw JuxtAuthException("Your session expired. Please sign in again.")
        }
        return body
    }

    private suspend fun postForm(path: String, build: ParametersBuilder.() -> Unit): HttpResponse {
        requireToken()
        val response = client.post(absolute(path)) {
            browserHeaders(auth = true)
            setBody(FormDataContent(Parameters.build(build)))
        }
        checkSession(response)
        return response
    }

    private fun requireToken() {
        if (accessToken == null) throw JuxtAuthException()
    }

    private fun checkSession(response: HttpResponse) {
        val location = response.headers[HttpHeaders.Location]
        if (location != null && location.startsWith("/login")) {
            logout()
            throw JuxtAuthException("Your session expired. Please sign in again.")
        }
        if (response.status.value == 401 || response.status.value == 403) {
            throw JuxtException("Juxtaposition refused the request (${response.status.value}).")
        }
    }

    private fun absolute(path: String): String = when {
        path.startsWith("http") -> path
        path.startsWith("/") -> BASE_URL + path
        else -> "$BASE_URL/$path"
    }

    private fun HttpRequestBuilder.browserHeaders(auth: Boolean = false) {
        header(HttpHeaders.UserAgent, USER_AGENT)
        header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
        header(HttpHeaders.Referrer, BASE_URL)
        if (auth) accessToken?.let { header(HttpHeaders.Cookie, "access_token=$it") }
    }

    private fun HttpResponse.setCookie(name: String): String? =
        headers.getAll(HttpHeaders.SetCookie)
            ?.firstOrNull { it.startsWith("$name=") }
            ?.substringAfter("$name=")
            ?.substringBefore(';')
            ?.takeIf { it.isNotBlank() }

    companion object {
        const val BASE_URL = "https://juxt.pretendo.network"
        const val MAX_POST_LENGTH = 280

        /** Cloudflare fronts juxt.pretendo.network and challenges non-browser clients. */
        const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/126.0.6478.71 Mobile Safari/537.36"
    }
}
