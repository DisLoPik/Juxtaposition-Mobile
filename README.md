# Juxtaposition for Android

An unofficial Android client for [Juxtaposition](https://juxt.pretendo.network), the Miiverse
revival run by [Pretendo Network](https://pretendo.network). Built with Kotlin Multiplatform and
Compose Multiplatform, with an iOS target already wired up for later.

You can sign in with your Pretendo Network ID, read the activity feeds, browse every listed
community, view profiles and notifications, and report or delete posts. It also does what the
website itself does not offer: **write posts and replies**, **draw** them, and Yeah other
people's posts.

## How it talks to Juxtaposition

This is the important thing to understand before changing any of the networking code.

Juxtaposition is two services (see
[PretendoNetwork/juxtaposition](https://github.com/PretendoNetwork/juxtaposition)):

- **`miiverse-api`** owns the data and exposes a clean, versioned JSON API under `/api/v1`.
- **`juxtaposition-ui`** is the server-rendered website at `juxt.pretendo.network`.

The JSON API is **not reachable over HTTP**. `miiverse-api` mounts it only behind gRPC, guarded by
a shared secret that lives on the web server (`apps/miiverse-api/src/services/internal/server.ts`),
and the website reaches it as an internal caller. There is no public API token an app could get.

So this app does what the
[Juxtaposition-Enhancer](https://github.com/ItsFuntum/Juxtaposition-Enhancer) userscript does in a
browser: it drives the public website directly. Reads parse the server-rendered HTML; writes are
ordinary HTML form submissions.

### Endpoints used

| Action | Request |
| --- | --- |
| Log in | `POST /login`; form `username`, `password`, `grant_type=password`, returns the `access_token` cookie |
| Activity feeds | `GET /feed`, `/feed/people`, `/feed/all` with `?pjax=true&offset=N` |
| All communities | `GET /titles/all` |
| Community posts | `GET /titles/{id}/new` or `/hot`, with `?pjax=true&offset=N` |
| A post and its replies | `GET /posts/{id}` |
| Yeah / un-Yeah | `POST /posts/empathy`; form `postID`, returns JSON |
| New post | `POST /posts/new`; form `community_id`, `body`, `_post_type=body`, `feeling_id`, `spoiler` |
| Reply | `POST /posts/{id}/new`; same fields |
| Follow / unfollow a community | `POST /titles/follow`; form `id`, returns JSON |
| A profile and its posts | `GET /users/{pid}`, or `/users/me`; `/users/{pid}/yeahs` for Yeahs |
| Follow / unfollow a user | `POST /users/follow`; form `id` (a PID), returns JSON |
| Notifications | `GET /news/my_news?pjax=true`; reading it also marks them read |
| Report a post | `POST /posts/{id}/report?api=true`; form `post_id`, `reason`, `message` |
| Delete your own post | `DELETE /posts/{id}`; the server checks ownership |

Two details that the code depends on:

- **`?pjax=true` returns just the post-list fragment** instead of a whole page. The website uses it
  for its own "load more" button, and it makes paging cheap here too. The app follows the
  `data-href` on the rendered `#load-more` button rather than computing offsets itself.
- **Redirects are not followed.** A successful post answers with a `302` to the new post, a
  rejected one redirects back to a `/create` URL carrying the reason, and an expired session
  redirects to `/login`. The `Location` header is how the app tells these apart, so
  `followRedirects` is off in `createJuxtHttpClient()`.

### Two things that will break this app

1. **Cloudflare.** `juxt.pretendo.network` sits behind a managed challenge that turns away clients
   without a browser `User-Agent`. `JuxtApi.USER_AGENT` exists for that reason; don't remove it.
2. **Markup changes.** Every selector lives in one file, `data/JuxtHtml.kt`, and is covered by
   `JuxtHtmlTest`, whose fixtures are real captured responses. If the site is restyled, those tests
   fail first and that file is the only one to update.

`LiveSmokeTest` (ignored by default) hits the real site to tell those two failure modes apart:

```
./gradlew :shared:testAndroidHostTest --tests "*LiveSmokeTest*" -i
```

The posting form fields are dictated by `newPost()` in the site's
`services/juxt-web/routes/console/posts.tsx`; `feeling_id` is `0` to `5` (Normal, Happy, Like,
Surprised, Frustrated, Puzzled) and posts are capped at 280 characters. Report reason ids come
from the site's `reportModalView.tsx` and are not sequential; do not renumber `ReportReason`.

### Drawings

A drawing is **not** a PNG. `miiverse-api` uploads paintings with `autodetectFormat: false`, so
the blob has to be the format Miiverse itself used: an uncompressed 32-bit **TGA**, reduced to
pure black and white, **zlib-compressed**, then base64, the same encoding the
Juxtaposition-Enhancer userscript produces. `data/Painting.kt` writes the TGA and `data/Zlib.kt`
compresses it (`java.util.zip.Deflater` on Android; a stored-block zlib stream elsewhere, which
is larger but decodes identically). The canvas is 320x120, matching the Wii U memo pad.

`ZlibTest` round-trips both compressors through a real inflater, so a broken drawing upload
fails the build rather than the server.

## Layout

```
shared/src/commonMain/kotlin/com/dislopik/juxtaposition/
├── App.kt                 root composable, session gate and back stack
├── model/Models.kt        Post, Community, Feeling, feed and sort types
├── data/
│   ├── JuxtApi.kt         every request the app makes
│   ├── JuxtHtml.kt        all HTML parsing, one place
│   ├── JuxtClient.kt      Ktor client setup and service locator
│   ├── Painting.kt        drawing -> TGA -> zlib -> base64
│   ├── Zlib.kt            expect/actual compression
│   └── TokenStore.kt      expect/actual session persistence
└── ui/
    ├── Theme.kt, Navigation.kt, PostListState.kt
    ├── components/        Avatar, PostCard, Drawing, dialogs, shared states
    └── screens/           Login, Home, Feed, Communities, Community,
                           Post, Profile, Notifications, Composer
```

`LocalSelfPid` carries the signed-in user's PID through the tree; it is what lets a post offer
Delete rather than Report. It is resolved once at startup from the navbar's own-profile link.

The session cookie is stored in `SharedPreferences` on Android and `NSUserDefaults` on iOS; the
password is only ever sent to `/login` and never persisted.

### Colors

`ui/Theme.kt` carries Juxtaposition's own palette, lifted from the site's stylesheets
(`webfiles/web/css/web.scss` and `login.css`) with each constant named after the CSS variable it
mirrors. The website is dark-only, so the app ignores the system light/dark setting.

One Material quirk is worth knowing before editing the scheme: Material uses `primary` for accent
*text* such as tab indicators, text buttons and links, as well as for filled buttons. The site's `--btn`
(`#673db6`) is a fill colour and only manages 1.8:1 against the navy background, so it lives in
`primaryContainer` (filled buttons and FABs) while `primary` holds the lighter `--theme-light`.
Putting `--btn` on `primary` is what makes text disappear.

## Building

Android is the supported target today:

```
./gradlew :androidApp:assembleDebug        # APK in androidApp/build/outputs/apk/debug/
./gradlew :shared:testAndroidHostTest      # parser tests
```

The iOS target compiles as part of the Kotlin Multiplatform setup but needs a Mac and Xcode to
build the app itself; nothing in `commonMain` is Android-only.

## Notes

This is an unofficial client and is not affiliated with Pretendo Network. Because it acts as a
browser, be considerate of the server's rate limits; the app surfaces them rather than retrying
(posting is limited to roughly 10 per 15 seconds, Yeahs to 60 per minute).

## License

Licensed under the GNU Affero General Public License, version 3 or later. The full text is in
[LICENSE](./LICENSE).

    Copyright (C) 2026 the Juxtaposition for Android authors

    This program is free software: you can redistribute it and/or modify it under the terms
    of the GNU Affero General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
    without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License along with this
    program. If not, see <https://www.gnu.org/licenses/>.

AGPL-3.0 matches [PretendoNetwork/juxtaposition](https://github.com/PretendoNetwork/juxtaposition),
whose routes and markup this client is written against.
