# Firestore access checks

Draft: `firestore.rules`. Tested in the Firebase console Rules playground on 2026-09-28, before publication. No production documents were created by these simulations.

| Operation | Document | Auth UID | Expected | Observed |
|---|---|---|---|---|
| get | users/trace-test-owner/sync/head | none | deny | denied |
| get | users/trace-test-owner/sync/head | trace-test-owner | allow | allowed |
| get | users/trace-test-owner/sync/head | trace-test-other | deny | denied |
| create | users/trace-test-owner/sync/head | trace-test-other | deny | denied |
| create | users/trace-test-owner/sync/head | trace-test-owner | allow | allowed |

Publication confirmed on 2026-09-28: after the owner published in Chrome, a fresh Firebase console tab showed the current 22:23 rules revision matching `firestore.rules`. Earlier console errors are resolved. No production documents were created by these playground checks.

These checks verify basic namespace isolation, not complete account synchronization. Device tests must still cover conflict choices, offline changes, interrupted uploads, backup recovery and account switching before release.
