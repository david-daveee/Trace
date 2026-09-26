package com.podhod.app;

import org.json.*;

/** Add catalog entries without resetting a user's edited plan or session. */
public final class BuiltinPlans {
    public static boolean ensure(JSONObject data, JSONObject plan) {
        JSONArray programs=data.optJSONArray("programs");
        for(int i=0;i<programs.length();i++)
            if(plan.optString("id").equals(programs.optJSONObject(i).optString("id")))return false;
        Engine.validateProgram(plan);
        programs.put(Engine.copy(plan));
        return true;
    }
}
