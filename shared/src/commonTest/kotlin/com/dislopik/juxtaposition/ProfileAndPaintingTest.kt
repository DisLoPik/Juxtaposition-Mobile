package com.dislopik.juxtaposition

import com.dislopik.juxtaposition.data.JuxtHtml
import com.dislopik.juxtaposition.data.Painting
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Profile markup captured from juxt.pretendo.network, plus the painting encoder.
 * The notification fixture is hand-built from the site's notificationListView component,
 * since that page needs a signed-in session to fetch.
 */
class ProfileAndPaintingTest {

    private val profileHtml = """<div class="title-line"><a href="/users/1057484826" class="header-icon-container"><img src="https://r2-cdn.pretendo.cc/mii/1057484826/normal_face.png" class="header-icon"/></a><div class="title">Dajori_10 @Dajori_10</div><a href="#" role="button" aria-pressed="false" class="follow-button" onclick="follow(this)" data-url="/users/follow" data-community-id="1057484826" title="Follow"><span class="ui-icon" role="img" aria-label="heart icon" style="line-height:0.7"><svg></svg></span></a></div><div class="description">Private</div><div class="stat-boxes cols-2"><div><div class="value">ES</div><div class="name">Country</div></div><div><div class="value">Jun 27th</div><div class="name">Birthday</div></div><div><div class="value">Expert</div><div class="name">Game Experience</div></div><div><div class="value" id="followers">2</div><div class="name">Followers</div></div></div></div></div><div class="page-infobox-buttons"></div>"""

    private val notificationsHtml = """
<ul class="list-content-with-icon-and-text arrow-list" id="news-list-content">
  <li><div class="hover">
    <a href="/posts/abc123" class="icon-container notify"><img src="https://cdn/mii/1234/normal_face.png" class="icon"/></a>
    <a class="body" href="/posts/abc123"><span class="text"><span class="link">Someone Yeahed your post</span><span class="timestamp"> 2 hours ago</span></span></a>
  </div></li>
  <li><div class="hover">
    <a href="/users/5678" class="icon-container notify"><img src="https://cdn/mii/5678/normal_face.png" class="icon"/></a>
    <a class="body" href="/users/5678"><span class="text"><span class="link">Someone followed you</span><span class="timestamp"> 5 days ago</span></span></a>
  </div></li>
</ul>
"""

    @Test
    fun parsesAUserProfile() {
        val profile = JuxtHtml.parseUserProfile(profileHtml)

        assertNotNull(profile)
        assertEquals(1057484826L, profile.pid)
        assertEquals("Dajori_10", profile.miiName)
        assertEquals("Dajori_10", profile.username)
        assertTrue(profile.avatarUrl!!.endsWith("/mii/1057484826/normal_face.png"))
        assertTrue(!profile.isFollowing)
        // Another user's profile renders a follow button; the signed-in user's own does not.
        assertTrue(!profile.isSelf)
    }

    @Test
    fun readsTheLabelledProfileStats() {
        val stats = JuxtHtml.parseUserProfile(profileHtml)!!.stats
        val byLabel = stats.associate { it.label to it.value }

        assertEquals("ES", byLabel["Country"])
        assertEquals("Jun 27th", byLabel["Birthday"])
        assertEquals("Expert", byLabel["Game Experience"])
        assertEquals("2", byLabel["Followers"])
    }

    @Test
    fun parsesNotificationsAndTheirTargets() {
        val items = JuxtHtml.parseNotifications(notificationsHtml)

        assertEquals(2, items.size)
        assertEquals("Someone Yeahed your post", items[0].text)
        assertEquals("2 hours ago", items[0].timeText)
        assertEquals("abc123", items[0].postId)
        assertEquals(null, items[0].userPid)

        assertEquals(5678L, items[1].userPid)
        assertEquals(null, items[1].postId)
    }

    @Test
    fun paintingTgaHasTheHeaderTheServerExpects() {
        val pixels = IntArray(Painting.WIDTH * Painting.HEIGHT) { 0xFFFFFFFF.toInt() }
        val tga = Painting.toTga(pixels, Painting.WIDTH, Painting.HEIGHT)

        assertEquals(18 + Painting.WIDTH * Painting.HEIGHT * 4, tga.size)
        assertEquals(2, tga[2].toInt())            // uncompressed true-colour
        assertEquals(32, tga[16].toInt())          // 32 bits per pixel
        assertEquals(0x20, tga[17].toInt())        // top-left origin
        assertEquals(Painting.WIDTH, (tga[12].toInt() and 0xFF) or ((tga[13].toInt() and 0xFF) shl 8))
        assertEquals(Painting.HEIGHT, (tga[14].toInt() and 0xFF) or ((tga[15].toInt() and 0xFF) shl 8))
    }

    @Test
    fun paintingIsReducedToPureBlackAndWhite() {
        // one white, one black, one mid-grey, one fully transparent
        val pixels = intArrayOf(0xFFFFFFFF.toInt(), 0xFF000000.toInt(), 0xFF808080.toInt(), 0x00000000)
        val tga = Painting.toTga(pixels, 2, 2)

        fun channel(i: Int) = tga[18 + i * 4].toInt() and 0xFF
        assertEquals(255, channel(0))
        assertEquals(0, channel(1))
        assertEquals(255, channel(2))   // 0x80 average is above the threshold
        assertEquals(255, channel(3))   // undrawn pixels are paper, not ink
        assertEquals(255, tga[18 + 3].toInt() and 0xFF)
    }
}
