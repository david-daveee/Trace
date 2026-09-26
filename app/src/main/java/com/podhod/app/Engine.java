package com.podhod.app;

import org.json.*;
import java.util.*;

/** Pure data operations. Completed sets are snapshots; future percentage sets follow program maxima. */
public final class Engine {
    public static JSONObject obj(String text) {
        try { return new JSONObject(text); } catch(JSONException e) { throw new IllegalArgumentException(Lang.t("Некорректный JSON"),e); }
    }
    public static JSONObject put(JSONObject o,String k,Object v) {
        try { o.put(k,v); return o; } catch(JSONException e) { throw new IllegalArgumentException(e); }
    }
    public static JSONObject copy(JSONObject o) { return obj(o.toString()); }
    public static String id() { return UUID.randomUUID().toString(); }
    public static String number(double n) { return n==Math.rint(n)?String.format(Locale.US,"%.0f",n):String.format(Locale.US,"%.2f",n).replaceAll("0+$","").replace('.',','); }
    public static double weight(JSONObject group,JSONObject maxima,double step) {
        String lift=group.optString("lift");
        if(!lift.isEmpty() && group.has("factor")) {
            // Source spreadsheet rounds some groups and deliberately leaves others unrounded.
            double raw=maxima.optDouble(lift,0)*group.optDouble("factor");
            double round=group.optDouble("round",step);
            return round>0?Math.floor(raw/round+0.5)*round:raw;
        }
        return group.optDouble("kg",-1);
    }
    public static void updateMaxima(JSONObject program, JSONObject maxima, JSONObject active) {
        JSONObject old=program.optJSONObject("maxima");
        for(String lift:Arrays.asList("squat","bench","deadlift")) if(maxima.has(lift)) {
            double n=maxima.optDouble(lift,Double.NaN);
            if(!Double.isFinite(n)||n<=0||n>2000)throw new IllegalArgumentException(Lang.t("Максимум должен быть больше 0 и не больше 2000 кг"));
        }
        // Validate before mutating either program or active session.
        JSONObject candidate=copy(program);put(candidate,"maxima",copy(maxima));validateProgram(candidate);
        if(active!=null&&active.optString("programId").equals(program.optString("id"))) {
            JSONArray sets=active.optJSONArray("sets");
            for(int i=0;i<sets.length();i++) {
                JSONObject set=sets.optJSONObject(i);
                if(set.optBoolean("done")||!set.has("factor"))continue;
                // Existing v0.1 sessions have no explicit override flag. Preserve a weight
                // that differs from the old program calculation instead of guessing it away.
                boolean manual=set.has("manualWeight")?set.optBoolean("manualWeight"):
                    Math.abs(set.optDouble("kg")-weight(set,old,program.optDouble("step",2.5)))>.00001;
                put(set,"manualWeight",manual);
                if(!manual)put(set,"kg",weight(set,maxima,program.optDouble("step",2.5)));
            }
            put(active,"revision",active.optInt("revision")+1);
        }
        put(program,"maxima",copy(maxima));
    }
    public static JSONObject start(JSONObject program,long now) {
        int day=program.optInt("nextDay");
        JSONArray days=program.optJSONArray("days");
        if(days==null || day>=days.length()) throw new IllegalArgumentException(Lang.t("Программа завершена"));
        JSONObject plan=days.optJSONObject(day);
        JSONArray sets=new JSONArray();
        JSONArray groups=plan.optJSONArray("groups");
        for(int g=0;g<groups.length();g++) {
            JSONObject group=groups.optJSONObject(g);
            for(int n=0;n<group.optInt("sets");n++) {
                JSONObject set=copy(group);
                put(set,"kg",weight(group,program.optJSONObject("maxima"),program.optDouble("step",2.5)));
                put(set,"group",g); put(set,"ordinal",n+1); put(set,"done",false);
                sets.put(set);
            }
        }
        JSONObject s=new JSONObject();
        put(s,"id",id());put(s,"programId",program.optString("id"));put(s,"programName",program.optString("name"));
        put(s,"day",day);put(s,"dayName",plan.optString("name"));put(s,"started",now);put(s,"sets",sets);
        put(s,"cursor",0);put(s,"revision",0);put(s,"exerciseNotes",program.optJSONObject("exerciseNotes")==null?new JSONObject():copy(program.optJSONObject("exerciseNotes")));
        return s;
    }
    public static void selectProgram(JSONObject data,JSONObject program,long now){
        String id=program.optString("id");JSONObject current=data.optJSONObject("active");
        if(current!=null&&id.equals(current.optString("programId")))return;
        JSONObject saved=data.optJSONObject("savedSessions");if(saved==null)saved=new JSONObject();
        JSONObject next=saved.optJSONObject(id);next=next==null?start(program,now):copy(next);
        // Prepare the requested session before changing the current one.
        if(current!=null){JSONObject parked=copy(current);if(!parked.optBoolean("paused"))act(parked,"pause",now);put(saved,parked.optString("programId"),parked);}
        saved.remove(id);put(next,"cursor",firstUndone(next));put(next,"revision",next.optInt("revision")+1);
        put(data,"savedSessions",saved);put(data,"active",next);put(program,"lastUsed",now);
    }
    public static void validateProgram(JSONObject p) {
        JSONArray days=p.optJSONArray("days");
        if(p.optString("name").trim().isEmpty() || days==null || days.length()==0 || days.length()>366) throw new IllegalArgumentException(Lang.t("Нужны название и 1–366 тренировочных дней"));
        if(p.optJSONObject("maxima")==null) put(p,"maxima",new JSONObject());
        int count=0;
        for(int d=0;d<days.length();d++) {
            JSONObject day=days.optJSONObject(d);
            JSONArray groups=day==null?null:day.optJSONArray("groups");
            if(groups==null||groups.length()==0) throw new IllegalArgumentException(Lang.t("День ")+(d+1)+Lang.t(": нет упражнений"));
            for(int g=0;g<groups.length();g++) {
                JSONObject x=groups.optJSONObject(g);
                if(x==null || x.optString("exercise").trim().isEmpty() || x.optInt("sets")<1 || x.optInt("sets")>100 || x.optInt("reps")<1 || x.optInt("reps")>1000) throw new IllegalArgumentException(Lang.t("Проверь упражнение, подходы и повторения в дне ")+(d+1));
                double kg=x.optDouble("kg",-1);
                if(!Double.isFinite(kg)||kg< -1||kg>2000) throw new IllegalArgumentException(Lang.t("Некорректный вес"));
                if(x.has("factor")) {
                    double factor=x.optDouble("factor",-1),max=p.optJSONObject("maxima").optDouble(x.optString("lift"),-1);
                    if(!Double.isFinite(factor)||factor<=0||factor>5||!Double.isFinite(max)||max<=0||max>2000) throw new IllegalArgumentException(Lang.t("Некорректный процент или исходный максимум"));
                }
                count+=x.optInt("sets");
            }
        }
        if(count>20000) throw new IllegalArgumentException(Lang.t("В программе слишком много подходов"));
    }
    public static int doneCount(JSONObject s) {
        int count=0;JSONArray sets=s.optJSONArray("sets");
        for(int i=0;i<sets.length();i++)if(sets.optJSONObject(i).optBoolean("done"))count++;
        return count;
    }
    public static int firstUndone(JSONObject s) {
        JSONArray sets=s.optJSONArray("sets");
        for(int i=0;i<sets.length();i++)if(!sets.optJSONObject(i).optBoolean("done"))return i;
        return sets.length();
    }
    public static boolean setDone(JSONObject s,int index,boolean done,long now) {
        if(s==null)return false;JSONArray sets=s.optJSONArray("sets");
        if(index<0||index>=sets.length())return false;JSONObject set=sets.optJSONObject(index);
        if(set.optBoolean("done")==done)return false;
        if(done&&set.optDouble("kg",-1)<0)return false;
        int oldCursor=s.optInt("cursor");put(set,"done",done);
        if(done){long order=s.optLong("completionOrder")+1;put(s,"completionOrder",order);put(set,"completionOrder",order);put(set,"finished",now);}
        else{set.remove("finished");set.remove("completionOrder");}
        int next=firstUndone(s);put(s,"cursor",next);
        if(!done||oldCursor!=next||next==sets.length()){put(s,"rest",false);put(s,"paused",false);s.remove("restUntil");s.remove("remaining");}
        put(s,"revision",s.optInt("revision")+1);return true;
    }
    public static int lastCompleted(JSONObject s) {
        int last=-1;long order=-1,time=-1;JSONArray sets=s.optJSONArray("sets");
        for(int i=0;i<sets.length();i++){JSONObject set=sets.optJSONObject(i);if(!set.optBoolean("done"))continue;long n=set.optLong("completionOrder"),t=set.optLong("finished");if(n>order||(n==order&&t>=time)){last=i;order=n;time=t;}}
        return last;
    }
    public static void clearRest(JSONObject s) {
        if(s==null)return;
        // Retire old timers, including sessions restored from older backups.
        if(s.optBoolean("rest"))put(s,"revision",s.optInt("revision")+1);
        put(s,"rest",false);s.remove("restUntil");s.remove("remaining");s.remove("restSeconds");
    }
    public static boolean act(JSONObject s,String action,long now) {
        if(s==null) return false;
        clearRest(s);
        JSONArray sets=s.optJSONArray("sets"); int cursor=s.optInt("cursor");
        if(action.equals("pause")) {
            boolean paused=s.optBoolean("paused");
            put(s,"paused",!paused);
        } else if(action.equals("undo")) {
            return setDone(s,lastCompleted(s),false,now);
        } else if(action.equals("next")) {
            if(s.optBoolean("paused")) return false;
            if(cursor>=sets.length()) return false;
            return setDone(s,cursor,true,now);
        } else return false;
        put(s,"revision",s.optInt("revision")+1);
        return true;
    }
}
