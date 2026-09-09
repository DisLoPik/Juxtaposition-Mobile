package com.dislopik.juxtaposition

import com.dislopik.juxtaposition.data.JuxtApi
import com.dislopik.juxtaposition.data.JuxtHtml
import com.dislopik.juxtaposition.data.createJuxtHttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A live check against juxt.pretendo.network, kept out of normal runs because it needs the
 * network and a healthy upstream site.
 *
 * Run it by hand when the app suddenly shows empty feeds, to tell apart the two failure modes
 * this app is exposed to: Cloudflare turning the client away (a non-200 status), or the site's
 * markup changing (a 200 with nothing parsed).
 *
 *     ./gradlew :shared:testAndroidHostTest --tests "*LiveSmokeTest*" -i
 *
 * It reads the community list, which Juxtaposition serves to signed-out visitors.
 */
@Ignore("Hits the live site; run manually")
class LiveSmokeTest {

    @Test
    fun reachesTheLiveSiteAndParsesCommunities() = runBlocking {
        val client = createJuxtHttpClient()
        val response = client.get("${JuxtApi.BASE_URL}/titles/all") {
            header(HttpHeaders.UserAgent, JuxtApi.USER_AGENT)
            header(HttpHeaders.Accept, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            header(HttpHeaders.AcceptLanguage, "en-US,en;q=0.9")
            header(HttpHeaders.Referrer, JuxtApi.BASE_URL)
        }
        println("status: ${response.status}")

        val communities = JuxtHtml.parseCommunityList(response.bodyAsText())
        println("communities parsed: ${communities.size}")
        println("first: ${communities.firstOrNull()}")

        assertTrue(
            communities.size > 10,
            "Expected the live community list but parsed ${communities.size} entries " +
                "(status ${response.status}). Either Cloudflare blocked the client or the markup changed."
        )
    }
}
