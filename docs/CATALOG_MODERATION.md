# Catalog moderation

The sole moderator is the owner's Trace Firebase Auth UID, fixed in CommunityPlans.ADMIN and firestore.rules catalogAdmin(). A client role flag cannot confer access.

Namespaces:
- catalogSubmissions/{ownerUid}~{planKey}: pending/reviewed snapshots. Owner and moderator only; bounded owner queries and moderator queue queries.
- publicPlans/{planKey}: publicly readable approved snapshots. Only the moderator can write, with fields tied to the approved submission.
- planLikes: unchanged public counts and private per-account votes.

Approvals use a transaction and compare the reviewed payload hash with the current pending record, preventing stale approvals. A pending snapshot cannot be edited by its author. Approved or rejected entries can be resubmitted; the previous public plan stays unchanged until approval. Published contents are separate from account backup data.

Validate rules with scripts/test_friend_rules.py against a disposable emulator only. Production rules publication requires confirmation. No real user submission should be auto-published as a test.
