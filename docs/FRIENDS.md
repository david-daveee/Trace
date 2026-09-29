# Friends and privacy

Friends uses separate Firestore collections, never the private account snapshot. Deploy `firestore.rules` and `firestore.indexes.json` together. The production rules and direct-share index were verified from the Android client for this release.

- `socialProfiles/{uid}`: own profile, chosen display name and immutable random code.
- `friendCodes/{code}`: exact signed-in lookup; directory listing is denied.
- `connections/{from}~{to}`: pending requests; only the recipient can accept, either member can remove.
- `social/{uid}/settings/privacy`: owner-controlled category visibility.
- `social/{uid}/items/{id}`: explicitly shared snapshots and audience; compressed payload chunks live under immutable revisions.

Only accepted friends can read visible items in enabled categories. Direct shares also require membership in the recipient list. Chunk reads check the current item's visibility, audience and revision. The private `users/{uid}` sync namespace remains owner-only.

Plan exports remove history, current/paused workouts, personal exercise notes and progression bookkeeping. Walk exports use a whitelist; exact points are included only with explicit route sharing. Shared plan weights and covers are included. Re-sharing replaces the snapshot and audience. Hiding cannot revoke copies already downloaded.

All social reads use the server. No automatic publication, push notification delivery or offline social feed is implemented. Queries currently return at most 100 connections/items. Old payload revisions can remain in storage but are inaccessible to friends once superseded.

## Verification

Run the Firestore emulator locally on port 8181 with project `demo-trace-friends` and this repository's rules, then run `python scripts/test_friend_rules.py`. This resets only the disposable local demo database and checks 44 access cases.

`SocialPayloadTest` verifies private-field removal, explicit route opt-in, deep copying, rejection of unfinished walks and code formatting.

`AccountSmoke -e friendsRead true` verifies real server reads and both feed query shapes without creating social data. `-e createFriendName NAME` is an explicitly opt-in mutation: it creates that signed-in user's profile. Do not run it on a real account without the user's requested name. `WelcomeSmoke -e preview friends-live` renders the existing profile for visual checks.

The release was tested on one physical phone. Full friend-to-friend exchange on two phones remains to be verified.
