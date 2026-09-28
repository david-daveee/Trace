package com.podhod.app;
import org.json.*;
import java.util.*;

/** Add equipment metadata without recalculating weights or changing completion records. */
final class SheikoBarbells {
 static final Set<String> EXERCISES=new HashSet<>(Arrays.asList("Жим лёжа","Присед","Становая тяга","Тяга до колен","Тяга из ямы","Тяга с плинтов","Тяга на подставке","Жим из-за головы","Наклонный жим","Жим сидя под углом","Наклоны сидя","Наклоны стоя","Наклон со штангой сидя","Наклоны со штангой стоя","Подъем штанги на трицепсы","Присед со штангой на груди","Приседания со штангой в \"ножницах\""));
 static void mark(JSONArray groups){if(groups==null)return;for(int i=0;i<groups.length();i++){JSONObject g=groups.optJSONObject(i);if(g!=null&&!g.has("barbell")&&EXERCISES.contains(g.optString("exercise")))Engine.put(g,"barbell",true);}}
 static void migrate(JSONObject data){Set<String> ids=new HashSet<>();JSONArray plans=data.optJSONArray("programs");if(plans==null)return;for(int i=0;i<plans.length();i++){JSONObject p=plans.optJSONObject(i);if(!PlanCategory.sheiko(p))continue;ids.add(p.optString("id"));JSONArray days=p.optJSONArray("days");if(days!=null)for(int d=0;d<days.length();d++)mark(days.optJSONObject(d).optJSONArray("groups"));}session(data.optJSONObject("active"),ids);JSONObject saved=data.optJSONObject("savedSessions");if(saved!=null){Iterator<String> keys=saved.keys();while(keys.hasNext())session(saved.optJSONObject(keys.next()),ids);}}
 static void session(JSONObject s,Set<String> ids){if(s!=null&&ids.contains(s.optString("programId")))mark(s.optJSONArray("sets"));}
}
