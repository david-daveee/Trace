package com.podhod.app;

import java.util.*;
import org.json.*;

/** Workouts are portable, including unfinished sessions. Live GPS and sensor state stay local. */
final class SyncPayload {
    static JSONObject portable(JSONObject backup) {
        JSONObject result = Engine.copy(backup);
        result.remove("createdAt");
        JSONObject workouts = result.optJSONObject("workouts");
        if (workouts == null) throw new IllegalArgumentException("Missing workouts");
        Engine.put(result,"sessionSyncVersion",2);
        JSONObject walks = result.optJSONObject("walks");
        if (walks == null) throw new IllegalArgumentException("Missing walks");
        walks.remove("active");
        JSONObject steps = result.optJSONObject("steps");
        if (steps == null) throw new IllegalArgumentException("Missing steps");
        steps.remove("enabled");
        JSONObject settings = result.optJSONObject("settings");
        if (settings != null) settings.remove("lastBackupAt");
        return result;
    }

    /** The selected side wins for plans, settings and steps; only walks are unioned. */
    static JSONObject choose(JSONObject phone, JSONObject cloud, boolean usePhone) {
        JSONObject result = Engine.copy(usePhone ? phone : cloud);
        JSONObject localWalks = phone.optJSONObject("walks"), remoteWalks = cloud.optJSONObject("walks");
        if (localWalks == null || remoteWalks == null) throw new IllegalArgumentException("Missing walks");
        Set<String> deleted = new TreeSet<>();
        for (JSONObject walks : new JSONObject[]{localWalks, remoteWalks}) {
            JSONArray ids = walks.optJSONArray("deletedIds");
            if (ids != null) for (int i = 0; i < ids.length(); i++) {
                String id = ids.optString(i, "");
                if (!id.isEmpty()) deleted.add(id);
            }
        }
        JSONObject merged = new JSONObject();
        Engine.put(merged, "history", SyncMerge.walks(localWalks.optJSONArray("history"), remoteWalks.optJSONArray("history"), deleted));
        Engine.put(merged, "deletedIds", new JSONArray(deleted));
        Engine.put(result, "walks", merged);
        return result;
    }

    static String fingerprint(JSONObject payload) { return SyncMerge.digest(SyncMerge.canonical(payload)); }
}
