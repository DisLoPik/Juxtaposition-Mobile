package com.dislopik.juxtaposition.data

import com.dislopik.juxtaposition.model.Community
import com.dislopik.juxtaposition.model.CommunityDetail
import com.dislopik.juxtaposition.model.JuxtNotification
import com.dislopik.juxtaposition.model.Post
import com.dislopik.juxtaposition.model.PostPage
import com.dislopik.juxtaposition.model.PostThread
import com.dislopik.juxtaposition.model.ProfileStat
import com.dislopik.juxtaposition.model.SelfUser
import com.dislopik.juxtaposition.model.UserProfile
import com.dislopik.juxtaposition.model.UserRef
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element

/**
 * Juxtaposition's web frontend is server-rendered HTML, so the app reads it the same way a
 * browser does. Every selector here mirrors a component in juxtaposition-ui's `views/web`
 * tree, so if the site markup changes this file is the only place that needs updating.
 */
object JuxtHtml {

    fun parsePostPage(html: String): PostPage {
        val doc = Ksoup.parse(html)
        val posts = doc.select("div.posts-wrapper").mapNotNull { parsePost(it) }
        val nextLink = doc.selectFirst("button#load-more")?.attr("data-href")?.takeIf { it.isNotBlank() }
        return PostPage(posts, nextLink)
    }

    fun parseThread(html: String): PostThread? {
        val doc = Ksoup.parse(html)
        // The post page renders the main post first, then every reply, all as .posts-wrapper.
        val wrappers = doc.select("div.posts-wrapper")
        val main = wrappers.firstOrNull()?.let { parsePost(it) } ?: return null
        val replies = wrappers.drop(1).mapNotNull { parsePost(it) }
        return PostThread(
            post = main,
            replies = replies,
            communityId = main.communityId,
            communityName = main.communityName
        )
    }

    fun parseCommunityList(html: String): List<Community> {
        val doc = Ksoup.parse(html)
        return doc.select("a.community-list-wrapper").mapNotNull { parseCommunityItem(it) }
    }

    fun parseCommunityDetail(html: String, fallbackId: String): CommunityDetail {
        val doc = Ksoup.parse(html)
        val name = doc.selectFirst(".title-line .title")?.text()?.trim().orEmpty()
        val icon = doc.selectFirst(".title-line img.header-icon")?.attr("src")?.takeIf { it.isNotBlank() }
        val description = doc.selectFirst(".description")?.text()?.trim()?.takeIf { it.isNotBlank() }
        val banner = doc.selectFirst(".infobox-banner, .infobox img.banner")?.attr("src")?.takeIf { it.isNotBlank() }

        // Stat boxes render as value/name pairs: followers, posts, tags.
        val statValues = doc.select(".stat-boxes .value").map { it.text().trim() }
        val followers = doc.selectFirst("#followers")?.text()?.trim() ?: statValues.getOrNull(0)
        val postCount = statValues.getOrNull(1)

        val followButton = doc.selectFirst("[data-button-follow-title], .follow-button")
        val isFollowing = followButton?.let {
            it.hasClass("selected") || it.attr("aria-pressed") == "true" ||
                it.text().contains("Unfollow", ignoreCase = true)
        } ?: false

        // Communities that are closed to new posts render the "closed" headline instead of a post list.
        val isOpen = doc.selectFirst(".headline h2") == null
        val canPost = isOpen

        return CommunityDetail(
            community = Community(
                id = fallbackId,
                name = name.ifBlank { fallbackId },
                iconUrl = icon,
                followersText = followers
            ),
            description = description,
            bannerUrl = banner,
            followerCount = followers,
            postCount = postCount,
            isFollowing = isFollowing,
            canPost = canPost,
            isOpen = isOpen
        )
    }


    /**
     * The navbar links to /users/me with the signed-in user's Mii, and the image path
     * carries their PID; the only place the page names the signed-in account.
     */
    fun parseSelf(html: String): SelfUser? {
        val img = Ksoup.parse(html).selectFirst("header#nav-menu a[href='/users/me'] img") ?: return null
        val src = img.attr("src")
        val pid = MII_PID.find(src)?.groupValues?.get(1)?.toLongOrNull() ?: return null
        return SelfUser(pid = pid, avatarUrl = src.takeIf { it.isNotBlank() })
    }

    fun parseUserProfile(html: String): UserProfile? {
        val doc = Ksoup.parse(html)
        val titleLine = doc.selectFirst(".title-line") ?: return null

        val avatar = titleLine.selectFirst("img.header-icon")?.attr("src")?.takeIf { it.isNotBlank() }
        // The title reads "MiiName @username".
        val title = titleLine.selectFirst(".title")?.text()?.trim().orEmpty()
        val miiName = title.substringBefore(" @").trim().ifBlank { title }
        val username = title.substringAfter(" @", "").trim().takeIf { it.isNotEmpty() }

        // The follow button is absent on the signed-in user's own profile, which distinguishes it.
        val followButton = doc.selectFirst("a.follow-button[data-url='/users/follow']")
        val pid = followButton?.attr("data-community-id")?.toLongOrNull()
            ?: avatar?.let { MII_PID.find(it)?.groupValues?.get(1)?.toLongOrNull() }
            ?: return null
        val isFollowing = followButton?.let {
            it.attr("aria-pressed") == "true" || it.hasClass("selected")
        } ?: false

        val comment = doc.selectFirst(".description")?.text()?.trim()?.takeIf { it.isNotEmpty() }

        val stats = doc.select(".stat-boxes > div").mapNotNull { box ->
            val label = box.selectFirst(".name")?.text()?.trim().orEmpty()
            val value = box.selectFirst(".value")?.text()?.trim().orEmpty()
            if (label.isBlank() || value.isBlank()) null else ProfileStat(label, value)
        }

        return UserProfile(
            pid = pid,
            miiName = miiName.ifBlank { "Unknown" },
            username = username,
            avatarUrl = avatar,
            comment = comment,
            stats = stats,
            isFollowing = isFollowing,
            isSelf = followButton == null
        )
    }

    fun parseNotifications(html: String): List<JuxtNotification> {
        val doc = Ksoup.parse(html)
        return doc.select("#news-list-content > li").mapNotNull { item ->
            val body = item.selectFirst("a.body") ?: return@mapNotNull null
            val textNode = body.selectFirst("span.text") ?: return@mapNotNull null

            val timeText = textNode.selectFirst("span.timestamp")?.text()?.trim().orEmpty()
            // The timestamp is nested inside the text, so take it back out again.
            val full = textNode.text().trim()
            val text = full.removeSuffix(timeText).trim().ifBlank { full }

            JuxtNotification(
                text = text,
                timeText = timeText,
                iconUrl = item.selectFirst("img.icon")?.attr("src")?.takeIf { it.isNotBlank() },
                link = body.attr("href").takeIf { it.isNotBlank() }
            )
        }
    }

    /** The login page shows failures in a toast div rather than an HTTP error status. */
    fun parseLoginError(html: String): String? {
        val doc = Ksoup.parse(html)
        val toast = doc.selectFirst("#toast[data-show='true']") ?: return null
        return toast.text().trim().takeIf { it.isNotEmpty() }
    }

    fun isLoginPage(html: String): Boolean =
        Ksoup.parse(html).selectFirst("form.account[action='/login']") != null

    private val MII_PID = Regex("""/mii/(\d+)/""")

    private fun parseCommunityItem(el: Element): Community? {
        val href = el.attr("href")
        val id = href.removePrefix("/titles/").substringBefore('/').takeIf { it.isNotBlank() } ?: return null
        val name = el.selectFirst(".community-list-title")?.text()?.trim().orEmpty()
        val icon = el.selectFirst("img")?.attr("src")?.takeIf { it.isNotBlank() }
        val followers = el.selectFirst(".community-list-followers")?.text()?.trim()
        return Community(id = id, name = name.ifBlank { id }, iconUrl = icon, followersText = followers)
    }

    private fun parsePost(el: Element): Post? {
        val id = el.id().takeIf { it.isNotBlank() } ?: return null

        val authorLink = el.selectFirst(".post-user-info-wrapper a[href*='/users/show']")
        val pid = authorLink?.attr("href")
            ?.substringAfter("pid=", "")
            ?.takeWhile { it.isDigit() }
            ?.toLongOrNull() ?: 0L
        val avatar = el.selectFirst(".post-user-info-wrapper img.user-icon")
            ?.attr("src")?.takeIf { it.isNotBlank() }
        val name = el.selectFirst(".post-meta-wrapper h3 a")?.text()?.trim().orEmpty()

        val extra = el.selectFirst("p.extra-info")
        val timeText = extra?.selectFirst("a[href^='/posts/']")?.text()?.trim().orEmpty()
        val communityLink = extra?.selectFirst("a[href^='/titles/']")
        val communityId = communityLink?.attr("href")
            ?.removePrefix("/titles/")?.substringBefore('/')?.takeIf { it.isNotBlank() }
        val communityName = communityLink?.text()?.trim()

        val content = el.selectFirst("div.post-content")
        val body = content?.selectFirst("p")?.text()?.trim()?.takeIf { it.isNotEmpty() }
        val image = content?.selectFirst("img.screenshot, img.painting")
            ?.attr("src")?.takeIf { it.isNotBlank() }

        val yeahButton = el.selectFirst("[data-button-yeah-post]")
        val yeahed = yeahButton?.hasClass("selected") == true
        val empathyCount = yeahButton?.selectFirst("h4")?.text()?.trim()?.toIntOrNull() ?: 0
        val replyCount = el.selectFirst("a.reply-button h4")?.text()?.trim()?.toIntOrNull() ?: 0

        return Post(
            id = id,
            author = UserRef(pid = pid, name = name.ifBlank { "Unknown" }, avatarUrl = avatar),
            body = body,
            imageUrl = image,
            communityId = communityId,
            communityName = communityName,
            timeText = timeText,
            empathyCount = empathyCount,
            replyCount = replyCount,
            yeahed = yeahed,
            isSpoiler = el.selectFirst(".spoiler-overlay") != null,
            removed = el.hasClass("posts-wrapper-removed")
        )
    }
}
