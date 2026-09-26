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
        JSONObject p=Engine.obj("{\"id\":\"builtin-zlat-2018\",\"routine\":\"zlat-beginner-2018\",\"name\":\"Матвей Злат · турник и брусья\",\"mine\":false,\"nextDay\":0,\"restSeconds\":240,\"maxima\":{},\"days\":[]}");
        Engine.put(p,"description","Базовая схема 2018 года: 3 занятия в неделю, 3–4 подхода по 5–8 повторений. Здесь 12 занятий для удобства дневника, а не авторский срок цикла. Между занятиями — день отдыха. После цикла можно создать копию.");
        Engine.put(p,"source","https://www.youtube.com/watch?v=AeB4znuGuSo");
        for(int d=0;d<12;d++){
            JSONObject day=Engine.obj("{\"groups\":[]}");Engine.put(day,"name","Неделя "+(d/3+1)+" · День "+(d%3+1));
            for(String key:new String[]{"pullup","dip"}){
                JSONObject g=Engine.obj("{\"sets\":3,\"reps\":5,\"kg\":-1}");Engine.put(g,"zlatLift",key);Engine.put(g,"exercise",key.equals("pullup")?"Подтягивания с весом":"Брусья с весом");day.optJSONArray("groups").put(g);
            }p.optJSONArray("days").put(day);
        }return p;
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
    public static double increment(int reps){return reps>=8?5:reps==7?2.5:reps==6?1.25:reps==5?.5:0;}
    public static void advance(JSONObject p,JSONObject session){
        if(!is(p)||session.optString("id").equals(p.optString("lastProgressedSession")))return;
        JSONObject weights=Engine.copy(p.optJSONObject("workingWeights"));StringBuilder note=new StringBuilder();
        for(String key:new String[]{"pullup","dip"}){JSONObject set=last(session,key);if(set==null||!set.optBoolean("done"))continue;double kg=set.optDouble("kg",-1);if(kg<0)continue;double delta=increment(set.optInt("reps"));double next=Math.min(2000,kg+delta);Engine.put(weights,key,next);if(note.length()>0)note.append("\n");note.append(key.equals("pullup")?"Подтягивания: ":"Брусья: ").append(Engine.number(kg)).append(" → ").append(Engine.number(next)).append(" кг");}
        Engine.put(p,"workingWeights",weights);updateFuture(p,session.optInt("day")+1);Engine.put(p,"lastProgressedSession",session.optString("id"));Engine.put(p,"lastProgression",note.toString());
    }
}
