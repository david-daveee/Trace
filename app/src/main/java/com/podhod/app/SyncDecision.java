package com.podhod.app;

/** Whole-library conflicts are never resolved by a last-writer-wins upload. */
final class SyncDecision {
    enum Action { CHOOSE, NOTHING, PHONE, CLOUD }
    static Action decide(boolean consent, String baseRevision, String remoteRevision, String baseHash, String localHash) {
        if (!consent) return Action.CHOOSE;
        boolean remoteChanged = !baseRevision.equals(remoteRevision);
        boolean localChanged = !baseHash.equals(localHash);
        if (remoteChanged && localChanged) return Action.CHOOSE;
        if (remoteChanged) return Action.CLOUD;
        return localChanged ? Action.PHONE : Action.NOTHING;
    }
}
