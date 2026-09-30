package com.podhod.app;

import org.json.*;

/** Repeating beginner routine; progression uses the last planned set, never tap order. */
public final class Zlat {
    public static boolean is(JSONObject p){return p!=null&&"zlat-beginner-2018".equals(p.optString("routine"));}
    public static boolean ensure(JSONObject data){
        JSONArray plans=data.optJSONArray("programs");
        for(int i=0;i<plans.length();i++)if(is(plans.optJSONObject(i)))return false;
        plans.put(create());return true;
    }
    public static JSONObject create(){
        JSONObject p=Engine.obj("{\"id\":\"builtin-zlat-2018\",\"routine\":\"zlat-beginner-2018\",\"name\":\"Злат · Начальный\",\"mine\":false,\"nextDay\":0,\"restSeconds\":240,\"maxima\":{},\"days\":[]}");
        Engine.put(p,"description","Базовая схема 2018 года: 3 занятия в неделю, 3–4 подхода по 5–8 повторений. Здесь 12 занятий для удобства дневника, а не авторский срок цикла. Между занятиями — день отдыха. После цикла можно создать копию.");
        Engine.put(p,"source","https://www.youtube.com/watch?v=AeB4znuGuSo");
        for(int d=0;d<12;d++){
            JSONObject day=Engine.obj("{\"groups\":[]}");Engine.put(day,"name","Неделя "+(d/3+1)+" · День "+(d%3+1));
            for(String key:new String[]{"pullup","dip"}){
                JSONObject g=Engine.obj("{\"sets\":3,\"reps\":5,\"kg\":-1}");Engine.put(g,"zlatLift",key);Engine.put(g,"exercise",key.equals("pullup")?"Подтягивания с весом":"Брусья с весом");day.optJSONArray("groups").put(g);
            }p.optJSONArray("days").put(day);
        }EnglishPlans.plan(p);return p;
    }
    public static void configure(JSONObject p,double pull,double dip,int sets){
        if(!Double.isFinite(pull)||!Double.isFinite(dip)||pull<0||dip<0||pull>1995||dip>1995||(sets!=3&&sets!=4))throw new IllegalArgumentException(Lang.t("Укажи дополнительный вес от 0 до 1995 кг и 3 или 4 подхода"));
        JSONObject weights=new JSONObject();Engine.put(weights,"pullup",pull);Engine.put(weights,"dip",dip);Engine.put(p,"workingWeights",weights);Engine.put(p,"zlatSets",sets);Engine.put(p,"configured",true);updateFuture(p,p.optInt("nextDay"));
    }
    static void updateFuture(JSONObject p,int from){
        JSONArray days=p.optJSONArray("days");JSONObject weights=p.optJSONObject("workingWeights");
        for(int d=from;d<days.length();d++){JSONArray groups=days.optJSONObject(d).optJSONArray("groups");for(int g=0;g<groups.length();g++){JSONObject group=groups.optJSONObject(g);String key=group.optString("zlatLift");if(weights.has(key)){Engine.put(group,"kg",weights.optDouble(key));Engine.put(group,"sets",p.optInt("zlatSets",3));}}}
    }
    public static JSONObject last(JSONObject session,String key){JSONObject result=null;JSONArray sets=session.optJSONArray("sets");for(int i=0;i<sets.length();i++){JSONObject set=sets.optJSONObject(i);if(key.equals(set.optString("zlatLift")))result=set;}return result;}
    public static double increment(int reps){return reps>=8?5:reps==7?2.5:reps==6?1.25:0;}
    /** Repairs only a demonstrable old +0.5 result; never rewrites completed snapshots. */
    public static boolean repairMicroplateProgression(JSONObject data){boolean changed=false;JSONArray plans=data.optJSONArray("programs"),history=data.optJSONArray("history");if(plans==null||history==null)return false;
        for(int i=0;i<plans.length();i++){JSONObject p=plans.optJSONObject(i);if(!is(p))continue;String id=p.optString("lastProgressedSession");if(id.isEmpty()||id.equals(p.optString("microplateRepairSession")))continue;JSONObject source=null;for(int h=history.length()-1;h>=0;h--){JSONObject candidate=history.optJSONObject(h);if(id.equals(candidate.optString("id"))&&p.optString("id").equals(candidate.optString("programId"))){source=candidate;break;}}if(source==null||p.optJSONObject("workingWeights")==null)continue;
            JSONObject weights=p.optJSONObject("workingWeights");boolean repaired=false;
            for(String key:new String[]{"pullup","dip"}){JSONObject last=last(source,key);if(last==null||!last.optBoolean("done")||last.optInt("reps")!=5)continue;double base=last.optDouble("kg",-1),old=base+.5;if(base<0||Math.abs(weights.optDouble(key,-1)-old)>.00001)continue;
                Engine.put(weights,key,base);repaired=true;JSONArray days=p.optJSONArray("days");for(int d=Math.max(source.optInt("day")+1,p.optInt("nextDay"));d<days.length();d++){JSONArray groups=days.optJSONObject(d).optJSONArray("groups");for(int g=0;g<groups.length();g++){JSONObject group=groups.optJSONObject(g);if(key.equals(group.optString("zlatLift"))&&Math.abs(group.optDouble("kg",-1)-old)<.00001)Engine.put(group,"kg",base);}}
                repairSession(data.optJSONObject("active"),p.optString("id"),source.optInt("day"),key,old,base);JSONObject saved=data.optJSONObject("savedSessions");if(saved!=null){java.util.Iterator<String> keys=saved.keys();while(keys.hasNext())repairSession(saved.optJSONObject(keys.next()),p.optString("id"),source.optInt("day"),key,old,base);}
            }
            if(repaired){StringBuilder note=new StringBuilder();for(String key:new String[]{"pullup","dip"}){JSONObject last=last(source,key);if(last==null)continue;if(note.length()>0)note.append("\n");note.append(key.equals("pullup")?"Подтягивания: ":"Брусья: ").append(Engine.number(last.optDouble("kg"))).append(" → ").append(Engine.number(weights.optDouble(key))).append(" кг");}Engine.put(p,"lastProgression",note.toString());Engine.put(p,"microplateRepairSession",id);changed=true;}
        }return changed;
    }
    private static void repairSession(JSONObject session,String program,int day,String lift,double old,double weight){if(session==null||!program.equals(session.optString("programId"))||session.optInt("day")<=day)return;boolean changed=false;JSONArray sets=session.optJSONArray("sets");if(sets==null)return;for(int i=0;i<sets.length();i++){JSONObject set=sets.optJSONObject(i);if(!set.optBoolean("done")&&!set.optBoolean("manualWeight")&&lift.equals(set.optString("zlatLift"))&&Math.abs(set.optDouble("kg",-1)-old)<.00001){Engine.put(set,"kg",weight);changed=true;}}if(changed){Engine.put(session,"revision",session.optInt("revision")+1);session.remove("confirmedRevision");}}
    public static void advance(JSONObject p,JSONObject session){
        if(!is(p)||session.optString("id").equals(p.optString("lastProgressedSession")))return;
        JSONObject weights=Engine.copy(p.optJSONObject("workingWeights"));StringBuilder note=new StringBuilder();
        for(String key:new String[]{"pullup","dip"}){JSONObject set=last(session,key);if(set==null||!set.optBoolean("done"))continue;double kg=set.optDouble("kg",-1);if(kg<0)continue;double delta=increment(set.optInt("reps"));double next=Math.min(2000,kg+delta);Engine.put(weights,key,next);if(note.length()>0)note.append("\n");note.append(key.equals("pullup")?"Подтягивания: ":"Брусья: ").append(Engine.number(kg)).append(" → ").append(Engine.number(next)).append(" кг");}
        Engine.put(p,"workingWeights",weights);updateFuture(p,session.optInt("day")+1);Engine.put(p,"lastProgressedSession",session.optString("id"));Engine.put(p,"lastProgression",note.toString());
    }
}
