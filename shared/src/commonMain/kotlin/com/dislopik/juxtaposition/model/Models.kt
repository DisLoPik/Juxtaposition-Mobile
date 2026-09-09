package com.dislopik.juxtaposition.model

/** A Juxtaposition user as referenced from a post. */
data class UserRef(
    val pid: Long,
    val name: String,
    val avatarUrl: String? = null
)

/** Feelings map to the Miiverse `feeling_id` form field (0..5). */
enum class Feeling(val id: Int, val label: String) {
    NORMAL(0, "Normal"),
    HAPPY(1, "Happy"),
    LIKE(2, "Like"),
    SURPRISED(3, "Surprised"),
    FRUSTRATED(4, "Frustrated"),
    PUZZLED(5, "Puzzled");

    companion object {
        fun fromId(id: Int): Feeling = entries.firstOrNull { it.id == id } ?: NORMAL
    }
}

data class Post(
    val id: String,
    val author: UserRef,
    val body: String?,
    val imageUrl: String?,
    val communityId: String?,
    val communityName: String?,
    val timeText: String,
    val empathyCount: Int,
    val replyCount: Int,
    val yeahed: Boolean,
    val isSpoiler: Boolean,
    val removed: Boolean
) {
    val hasText: Boolean get() = !body.isNullOrBlank()
}

/** One page of posts plus the link used to fetch the following page. */
data class PostPage(
    val posts: List<Post>,
    val nextLink: String?
) {
    val hasMore: Boolean get() = nextLink != null && posts.isNotEmpty()
}

data class Community(
    val id: String,
    val name: String,
    val iconUrl: String?,
    val followersText: String?
)

data class CommunityDetail(
    val community: Community,
    val description: String?,
    val bannerUrl: String?,
    val followerCount: String?,
    val postCount: String?,
    val isFollowing: Boolean,
    val canPost: Boolean,
    val isOpen: Boolean
)

data class PostThread(
    val post: Post,
    val replies: List<Post>,
    val communityId: String?,
    val communityName: String?
)


/** Which activity feed to show. Maps to /feed, /feed/people and /feed/all. */
enum class FeedType(val path: String, val title: String) {
    FOLLOWING("/feed", "My Feed"),
    PEOPLE("/feed/people", "People"),
    ALL("/feed/all", "Global")
}

/** Recent vs popular ordering inside a community. */
enum class CommunitySort(val segment: String, val title: String) {
    RECENT("new", "Recent"),
    POPULAR("hot", "Popular")
}

/** The signed-in user, resolved from the navbar's link to their own page. */
data class SelfUser(
    val pid: Long,
    val avatarUrl: String? = null
)

data class UserProfile(
    val pid: Long,
    val miiName: String,
    val username: String?,
    val avatarUrl: String?,
    val comment: String?,
    val stats: List<ProfileStat>,
    val isFollowing: Boolean,
    val isSelf: Boolean
)

/** One of the labelled boxes on a profile: Country, Birthday, Game Experience, Followers. */
data class ProfileStat(val label: String, val value: String)

/** Which list a profile is showing. Posts and Yeahs are both post lists. */
enum class ProfileTab(val segment: String, val title: String) {
    POSTS("", "Posts"),
    YEAHS("yeahs", "Yeahs")
}

data class JuxtNotification(
    val text: String,
    val timeText: String,
    val iconUrl: String?,
    /** Where tapping it goes, e.g. `/posts/{id}` or `/users/{pid}`. */
    val link: String?
) {
    val postId: String? get() = link?.takeIf { it.startsWith("/posts/") }?.removePrefix("/posts/")
    val userPid: Long? get() = link?.takeIf { it.startsWith("/users/") }?.removePrefix("/users/")?.toLongOrNull()
}

/**
 * Report reasons, matching the `<select name="reason">` the site renders. The numbers are
 * the values the server expects, so they must not be renumbered.
 */
enum class ReportReason(val id: Int, val label: String) {
    NOT_NICE(11, "Mean/Rude/Hateful (Rule 1)"),
    INAPPROPRIATE(12, "Inappropriate/NSFW (Rule 2)"),
    SPAM(13, "Spam/Self-Promotion (Rule 3)"),
    OFFTOPIC(14, "Off-Topic (Rule 4)"),
    PIRACY(15, "Piracy (Rule 5)"),
    EXPLOITS(16, "API Abuse/Exploiting Bugs (Rule 8)"),
    DRAMA(17, "Drama (Rule 9)"),
    CHEATING(18, "Cheating Online (Rule 10)"),
    SPOILER(19, "Spoiler (Rule 11)"),
    PERSONAL_INFO(20, "Personal Information (Rule 12)"),
    POLITICS(21, "Politics (Rule 13)"),
    MISINFORMATION(22, "Misinformation/Bad Advice (Rule 14)"),
    IMPERSONATION(23, "Impersonation (Rule 15)"),
    OTHER(9, "Other")
}
