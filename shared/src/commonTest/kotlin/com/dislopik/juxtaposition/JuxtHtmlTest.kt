package com.dislopik.juxtaposition

import com.dislopik.juxtaposition.data.JuxtHtml
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * These fixtures are real markup captured from juxt.pretendo.network, trimmed only by
 * replacing inline <svg> bodies. They exist so a change to the site's markup fails here
 * rather than silently emptying the app's feeds.
 */
class JuxtHtmlTest {

    private val feedHtml = """<div class="posts-wrapper" id="16przDIh2t7zyaDtBLXMn"><div class="post-user-info-wrapper"><a href="/users/show?pid=1057484826"><img class="user-icon" src="https://r2-cdn.pretendo.cc/mii/1057484826/wink_left.png"/></a><div class="post-meta-wrapper"><h3><a href="/users/show?pid=1057484826">Dajori_10</a></h3><p class="extra-info"><a href="/posts/16przDIh2t7zyaDtBLXMn">9 hours ago</a> - <a href="/titles/3333298316">Mario &amp; Luigi Series</a></p></div></div><div class="post-content" id="post-content-16przDIh2t7zyaDtBLXMn" onclick="location.href=&#x27;/posts/16przDIh2t7zyaDtBLXMn&#x27;"><p>¿¿Algún consejo para el superstar saga original??</p></div><div class="post-buttons-wrapper"><span data-button-yeah-post="16przDIh2t7zyaDtBLXMn" class="post-button empathy-button" role="button" aria-pressed="false"><span class="ui-icon" role="img" aria-label="heart icon" style="line-height:0.7"><svg></svg></span><h4 id="count-16przDIh2t7zyaDtBLXMn">1</h4></span><a href="/posts/16przDIh2t7zyaDtBLXMn" class="post-button reply-button" role="button"><span class="ui-icon" role="img" aria-label="reply icon" style="line-height:0.7"><svg></svg></span><h4>1</h4></a><span class="post-button post-hamburger-button" aria-haspopup="menu" aria-expanded="false"><span class="ui-icon" role="img" aria-label="menu icon" style="line-height:0.7"><svg></svg>
</span><ul class="post-hamburger" role="menu" data-post="16przDIh2t7zyaDtBLXMn"><li role="menuitem" data-action="report"><span class="ui-icon" role="img" aria-label="flag icon" style="line-height:0.7"><svg></svg></span> Report post</li><li role="menuitem" data-action="copy"><span class="ui-icon" role="img" aria-label="share icon" style="line-height:0.7"><svg></svg></span> Copy link</li></ul></span></div></div><div class="posts-wrapper" id="GH3srGtGrZvIy3EcXPZgP"><div class="post-user-info-wrapper"><a href="/users/show?pid=1677326722"><img class="user-icon" src="https://r2-cdn.pretendo.cc/mii/1677326722/normal_face.png"/></a><div class="post-meta-wrapper"><h3><a href="/users/show?pid=1677326722">Nate</a></h3><p class="extra-info"><a href="/posts/GH3srGtGrZvIy3EcXPZgP">17 hours ago</a> - <a href="/titles/3333298316">Mario &amp; Luigi Series</a></p></div></div><div class="post-content" id="post-content-GH3srGtGrZvIy3EcXPZgP" onclick="location.href=&#x27;/posts/GH3srGtGrZvIy3EcXPZgP&#x27;"><img id="GH3srGtGrZvIy3EcXPZgP" class="painting" src="https://r2-cdn.pretendo.cc/paintings/1677326722/GH3srGtGrZvIy3EcXPZgP.png"/></div><div class="post-buttons-wrapper"><span data-button-yeah-post="GH3srGtGrZvIy3EcXPZgP" class="post-button empathy-button" role="button" aria-pressed="false"><span class="ui-icon" role="img" aria-label="heart icon" style="line-height:0.7"><svg></svg></span><h4 id="count-GH3srGtGrZvIy3EcXPZgP">2</h4></span><a href="/posts/GH3srGtGrZvIy3EcXPZgP" class="post-button reply-button" role="button"><span class="ui-icon" role="img" aria-label="reply icon" style="line-height:0.7"><svg></svg></span><h4>0</h4></a><span class="post-button post-hamburger-button" aria-haspopup="menu" aria-expanded="false"><span class="ui-icon" role="img" aria-label="menu icon" style="line-height:0.7"><svg></svg>
</span><ul class="post-hamburger" role="menu" data-post="GH3srGtGrZvIy3EcXPZgP"><li role="menuitem" data-action="report"><span class="ui-icon" role="img" aria-label="flag icon" style="line-height:0.7"><svg></svg></span> Report post</li><li role="menuitem" data-action="copy"><span class="ui-icon" role="img" aria-label="share icon" style="line-height:0.7"><svg></svg></span> Copy link</li></ul></span></div></div><div id="wrapper" class="bottom"><button id="load-more" data-href="/titles/3333298316/new?offset=10&amp;pjax=true">Load More Posts</button></div>"""

    private val communityListHtml = """<a class="community-list-wrapper" href="/titles/2642035489/new" data-search-term="sonicallstarsracingtransformed"><div class="community-list-icon-container"><img src="https://r2-cdn.pretendo.cc/icons/2642035489/128.png" class="community-list-icon"/></div><div class="community-list-info"><h2 class="community-list-title">Sonic &amp; All Stars Racing: Transformed</h2><p class="community-list-followers">85 followers</p></div></a><a class="community-list-wrapper" href="/titles/4170429567/new" data-search-term="streetpassmiiplaza"><div class="community-list-icon-container"><img src="https://r2-cdn.pretendo.cc/icons/4170429567/128.png" class="community-list-icon"/></div><div class="community-list-info"><h2 class="community-list-title">StreetPass Mii Plaza</h2><p class="community-list-followers">304 followers</p></div></a>"""

    private val threadHtml = """<div class="posts-wrapper" id="16przDIh2t7zyaDtBLXMn"><div class="post-user-info-wrapper"><a href="/users/show?pid=1057484826"><img class="user-icon" src="https://r2-cdn.pretendo.cc/mii/1057484826/wink_left.png"/></a><div class="post-meta-wrapper"><h3><a href="/users/show?pid=1057484826">Dajori_10</a></h3><p class="extra-info"><a href="/posts/16przDIh2t7zyaDtBLXMn">9 hours ago</a> - <a href="/titles/3333298316">Mario &amp; Luigi Series</a></p></div></div><div class="post-content" id="post-content-16przDIh2t7zyaDtBLXMn" onclick="location.href=&#x27;/posts/16przDIh2t7zyaDtBLXMn&#x27;"><p>¿¿Algún consejo para el superstar saga original??</p></div><div class="post-buttons-wrapper"><span data-button-yeah-post="16przDIh2t7zyaDtBLXMn" class="post-button empathy-button" role="button" aria-pressed="false"><span class="ui-icon" role="img" aria-label="heart icon" style="line-height:0.7"><svg></svg></span><h4 id="count-16przDIh2t7zyaDtBLXMn">1</h4></span><a href="/posts/16przDIh2t7zyaDtBLXMn" class="post-button reply-button" role="button"><span class="ui-icon" role="img" aria-label="reply icon" style="line-height:0.7"><svg></svg></span><h4>1</h4></a><span class="post-button post-hamburger-button" aria-haspopup="menu" aria-expanded="false"><span class="ui-icon" role="img" aria-label="menu icon" style="line-height:0.7"><svg></svg>
</span><ul class="post-hamburger" role="menu" data-post="16przDIh2t7zyaDtBLXMn"><li role="menuitem" data-action="report"><span class="ui-icon" role="img" aria-label="flag icon" style="line-height:0.7"><svg></svg></span> Report post</li><li role="menuitem" data-action="copy"><span class="ui-icon" role="img" aria-label="share icon" style="line-height:0.7"><svg></svg></span> Copy link</li></ul></span></div><div class="yeah-text"><span class="feeling">1</span> person gave this post a yeah.</div><div class="yeah-list"><a href="/users/1746921963" class="mii-icon-container"><img src="https://r2-cdn.pretendo.cc/mii/1746921963/normal_face.png" class="mii-icon"/></a></div></div><div class="reply-control-bar"><p class="reply-control-title">Replies</p><a class="reply-control-item" href="/posts/16przDIh2t7zyaDtBLXMn?sort=newest-first"><span class="reply-icon"><span class="ui-icon" role="img" aria-label="up-down icon" style="line-height:0.7"><svg></svg></span></span>Sort by oldest</a></div><span class="replies-line"></span><div><div class="posts-wrapper" id="wUwP2iaSRZsfqIizVMuM9"><div class="post-user-info-wrapper"><a href="/users/show?pid=1153406478"><img class="user-icon" src="https://r2-cdn.pretendo.cc/mii/1153406478/normal_face.png"/></a><div class="post-meta-wrapper"><h3><a href="/users/show?pid=1153406478">Pablo</a></h3><p class="extra-info"><a href="/posts/wUwP2iaSRZsfqIizVMuM9">18 minutes ago</a> - <a href="/titles/3333298316">Mario &amp; Luigi Series</a></p></div></div><div class="post-content" id="post-content-wUwP2iaSRZsfqIizVMuM9" onclick="location.href=&#x27;/posts/wUwP2iaSRZsfqIizVMuM9&#x27;"><p>la insignia champiñon</p></div><div class="post-buttons-wrapper"><span data-button-yeah-post="wUwP2iaSRZsfqIizVMuM9" class="post-button empathy-button" role="button" aria-pressed="false"><span class="ui-icon" role="img" aria-label="heart icon" style="line-height:0.7"><svg></svg></span><h4 id="count-wUwP2iaSRZsfqIizVMuM9">0</h4></span><a href="/posts/wUwP2iaSRZsfqIizVMuM9" class="post-button reply-button" role="button"><span class="ui-icon" role="img" aria-label="reply icon" style="line-height:0.7"><svg></svg></span><h4>0</h4></a><span class="post-button post-hamburger-button" aria-haspopup="menu" aria-expanded="false"><span class="ui-icon" role="img" aria-label="menu icon" style="line-height:0.7"><svg></svg>
</span><ul class="post-hamburger" role="menu" data-post="wUwP2iaSRZsfqIizVMuM9"><li role="menuitem" data-action="report"><span class="ui-icon" role="img" aria-label="flag icon" style="line-height:0.7"><svg></svg></span> Report post</li><li role="menuitem" data-action="copy"><span class="ui-icon" role="img" aria-label="share icon" style="line-height:0.7"><svg></svg></span> Copy link</li></ul></span></div></div><span class="replies-line"></span></div></div></div></div>"""

    private val communityDetailHtml = """<div class="title-line"><div class="header-icon-container"><img src="https://r2-cdn.pretendo.cc/icons/3333298316/128.png" class="header-icon"/></div><div class="title">Mario &amp; Luigi Series</div><a href="#" role="button" aria-pressed="false" class="follow-button" onclick="follow(this)" data-url="/titles/follow" data-community-id="3333298316" title="Follow"><span class="ui-icon" role="img" aria-label="heart icon" style="line-height:0.7"><svg></svg></span></a></div><div class="description">Feel free to discuss about the Mario &amp; Luigi series here. Screenshots should be supported for all related 3DS titles.</div><div class="stat-boxes cols-3"><div><div class="value" id="followers">206</div><div class="name">Followers</div></div><div><div class="value">95</div><div class="name">Posts</div></div><div><div class="value">N/A</div><div class="name">Tags</div></div></div></div></div><div class="page-infobox-buttons"></div>"""

    @Test
    fun parsesPostsFromAFeedFragment() {
        val page = JuxtHtml.parsePostPage(feedHtml)

        assertEquals(2, page.posts.size)
        val first = page.posts[0]
        assertEquals("16przDIh2t7zyaDtBLXMn", first.id)
        assertEquals("Dajori_10", first.author.name)
        assertEquals(1057484826L, first.author.pid)
        assertTrue(first.author.avatarUrl!!.endsWith("/mii/1057484826/wink_left.png"))
        assertEquals("¿¿Algún consejo para el superstar saga original??", first.body)
        assertEquals("3333298316", first.communityId)
        assertEquals("Mario & Luigi Series", first.communityName)
        assertEquals("9 hours ago", first.timeText)
        assertEquals(1, first.empathyCount)
        assertTrue(!first.yeahed)
        assertTrue(!first.removed)
    }

    @Test
    fun readsThePagingLinkTheServerRendered() {
        val page = JuxtHtml.parsePostPage(feedHtml)
        assertEquals("/titles/3333298316/new?offset=10&pjax=true", page.nextLink)
        assertTrue(page.hasMore)
    }

    @Test
    fun parsesCommunityList() {
        val communities = JuxtHtml.parseCommunityList(communityListHtml)

        assertEquals(2, communities.size)
        val first = communities[0]
        assertEquals("2642035489", first.id)
        assertEquals("Sonic & All Stars Racing: Transformed", first.name)
        assertEquals("85 followers", first.followersText)
        assertTrue(first.iconUrl!!.endsWith("/icons/2642035489/128.png"))
    }

    @Test
    fun parsesAPostPageIntoAMainPostAndReplies() {
        val thread = JuxtHtml.parseThread(threadHtml)

        assertNotNull(thread)
        assertEquals("16przDIh2t7zyaDtBLXMn", thread.post.id)
        assertEquals("3333298316", thread.communityId)
        assertEquals(1, thread.replies.size)
        // A reply must never be mistaken for the main post.
        assertTrue(thread.replies.none { it.id == thread.post.id })
    }

    @Test
    fun parsesCommunityDetail() {
        val detail = JuxtHtml.parseCommunityDetail(communityDetailHtml, "3333298316")

        assertEquals("Mario & Luigi Series", detail.community.name)
        assertEquals("206", detail.followerCount)
        assertTrue(detail.description!!.startsWith("Feel free to discuss"))
        assertTrue(!detail.isFollowing)
    }

    @Test
    fun detectsTheLoginPage() {
        val loginHtml = """<form action="/login" method="post" class="account"><input name="username"/></form>"""
        assertTrue(JuxtHtml.isLoginPage(loginHtml))
        assertTrue(!JuxtHtml.isLoginPage(feedHtml))
    }

    @Test
    fun readsTheLoginErrorToast() {
        val html = """<div id="toast" data-show="true">Username was invalid.</div>"""
        assertEquals("Username was invalid.", JuxtHtml.parseLoginError(html))
    }
}
