# Friends and privacy

Friends uses separate Firestore collections, never the private account snapshot. Deploy `firestore.rules` and `firestore.indexes.json` together. The production rules and direct-share index were verified from the Android client for this release.

- `socialProfiles/{uid}`: own profile, chosen display name and immutable random code.
- `friendCodes/{code}`: exact signed-in lookup; directory listing is denied.
- `connections/{from}~{to}`: pending requests; only the recipient can accept, either member can remove.
- `social/{uid}/settings/privacy`: owner-controlled category visibility.
- `social/{uid}/items/{id}`: explicitly shared snapshots and audience; compressed payload chunks live under immutable revisions.

Only accepted friends can read visible items. New eye publications set `individualPrivacy: true`, overriding the legacy category gate for that item alone. Legacy publications still require an enabled category. Direct shares also require membership in the recipient list. Chunk reads check the current item's visibility, audience and revision. The private `users/{uid}` sync namespace remains owner-only.

Plan exports remove history, current/paused workouts, personal exercise notes and progression bookkeeping. Walk exports use a whitelist; exact points are included only with explicit route sharing. Shared plan weights and covers are included. Re-sharing replaces the snapshot and audience. Hiding cannot revoke copies already downloaded.

All social reads use the server. No automatic publication, push notification delivery or offline social feed is implemented. Friend feeds currently return at most 100 connections/items; the owner visibility lookup paginates all items. Old payload revisions can remain in storage but are inaccessible to friends once superseded.

## Verification

Run the Firestore emulator locally on port 8181 with project `demo-trace-friends` and this repository's rules, then run `python scripts/test_friend_rules.py`. This resets only the disposable local demo database and checks 73 access cases.

`SocialPayloadTest` verifies private-field removal, explicit route opt-in, deep copying, rejection of unfinished walks and code formatting.

`AccountSmoke -e friendsRead true` verifies real server reads and both feed query shapes without creating social data. `-e createFriendName NAME` is an explicitly opt-in mutation: it creates that signed-in user's profile. Do not run it on a real account without the user's requested name. `WelcomeSmoke -e preview friends-live` renders the existing profile for visual checks.

The release was tested on one physical phone. Full friend-to-friend exchange on two phones remains to be verified.

## Profile and screen updates

A voluntary profile at `social/{uid}/settings/profile` contains only displayName, bio and avatar, readable by the owner and accepted friends. The legacy immutable code profile remains compatible. Photos use the Android document picker, are positioned/zoomed in a circular preview, and are saved as 256px JPEGs without source metadata. Profiles are account data; they are not part of local workout backup replacement.

Friends ignores unrelated app change broadcasts while the signed-in account is unchanged. Manual refresh preserves the current content until a new result arrives and restores scroll position. Cache lifetime is limited to the activity and account; stale request callbacks cannot replace a newer result. Phone instrumentation checks that 20 broadcasts preserve the view root and that avatar cropping produces a decodable 256px image.

## Item privacy eyes

Each plan card and completed walk has a server-confirmed eye toggle. Missing metadata defaults to hidden. Opening publishes a snapshot to all accepted friends; closing revokes future reads. Walk eye publication includes the route. New walks are never published automatically. Loading and saving have a separate pending icon; failed requests do not optimistically claim visibility. Legacy direct audiences remain restricted until the owner explicitly opens them to all friends.

## Browsable profiles

Tap an accepted friend to open a full-screen profile with avatar, bio and All / Plans / Walks filters. The authorized feed is loaded once, with six rich previews at a time and a Show more action. Tabs reuse loaded data; refresh preserves content until the request finishes. Opening a plan or walk rechecks server access. Hidden items never contribute to the displayed publication counts. The profile shows shared plan covers and walk results; route maps open from walk details. `WelcomeSmoke -e preview friend-page` checks a real accepted friend and tab switching without publishing data.

The top bar has a dedicated My profile avatar beside Friends and Settings. It opens the owner photo, name, bio editor and friend code. Friends now focuses on the friend list and requests. Profile updates preserve the screen during unrelated sync broadcasts.

## Private text chats

The chat icon on a friend row and Message in a friend profile open a private conversation. Messages live in `connections/{id}/messages`; only members of an accepted connection can read or create them. Sender identity and server timestamp are enforced, text is limited to 2,000 characters, and updates/deletes are denied. Removing the connection revokes access. The client listens to the latest 40 messages and loads older pages on demand. It reports success only after the server acknowledges the write; failed sends retain the draft. Chats are separate from workout backups and account snapshot replacement. This version has no attachments, read receipts, push notifications, or end-to-end encryption. Server rules protect access, and messages are stored in Firebase. Tests send only disposable emulator messages; production verification must not send messages to real friends without explicit authorization.
