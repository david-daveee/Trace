package com.podhod.app;

/** Account data wins on sign-in and when the remote revision changed. */
final class SyncDecision {
    enum Action { NOTHING, PHONE, CLOUD }
    static Action afterUpload(boolean accountFirst, boolean ownUpload, String uploadedHash, String remoteHash,
                              boolean consent, String baseRevision, String remoteRevision, String baseHash, String localHash) {
        // A confirmed copy of our own upload is not a competing device edit.
        if (!accountFirst && ownUpload && !uploadedHash.isEmpty() && uploadedHash.equals(remoteHash)) return Action.PHONE;
        return decide(consent && !accountFirst, baseRevision, remoteRevision, baseHash, localHash);
    }
    static Action decide(boolean consent,String baseRevision,String remoteRevision,String baseHash,String localHash){
        if(!consent)return remoteRevision.isEmpty()?Action.PHONE:Action.CLOUD;
        if(!baseRevision.equals(remoteRevision))return remoteRevision.isEmpty()?Action.PHONE:Action.CLOUD;
        return baseHash.equals(localHash)?Action.NOTHING:Action.PHONE;
    }
}
