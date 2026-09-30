package com.podhod.app;
import android.content.*;
import org.json.*;
import java.nio.charset.StandardCharsets;
public final class Store {
    @android.annotation.SuppressLint("StaticFieldLeak") // Only application context is retained.
    private static Store instance;
    private final Context context;
    public JSONObject data;
    public static synchronized Store get(Context c) { if(instance==null) instance=new Store(c.getApplicationContext()); return instance; }
    private Store(Context c) {
        context=c;
        String saved=c.getSharedPreferences("podhod",0).getString("state",null);
        if(saved!=null) data=Engine.obj(saved);
        else {
            data=Engine.obj("{\"programs\":[],\"history\":[]}");
            try(var in=c.getAssets().open("sheiko.json")) {
                JSONObject p=Engine.obj(new String(Importer.read(in),StandardCharsets.UTF_8));
                Engine.put(p,"mine",true);Engine.put(p,"id",Engine.id());Engine.put(p,"lastUsed",System.currentTimeMillis());
                data.optJSONArray("programs").put(p);save();
            } catch(Exception e) { throw new IllegalStateException(Lang.t("Не удалось загрузить программу"),e); }
        }
        if(Zlat.ensure(data))save();
        ensureCatalog();
        if(active()!=null)Engine.put(active(),"cursor",Engine.firstUndone(active()));
        save();
    }
    public void ensureCatalog() {
        try(var in=context.getAssets().open("home_plans.json")) {
            JSONArray home=new JSONArray(new String(Importer.read(in),StandardCharsets.UTF_8));
            for(int i=0;i<home.length();i++)BuiltinPlans.ensure(data,home.getJSONObject(i));
        } catch(Exception e) { throw new IllegalStateException(Lang.t("Не удалось загрузить программу"),e); }
        ZlatIntermediate.ensure(data);
        ZlatAdvanced.ensure(data);
        Zlat.repairMicroplateProgression(data);
        try(var in=context.getAssets().open("sheiko_competition.json")) {
            BuiltinPlans.ensure(data,Engine.obj(new String(Importer.read(in),StandardCharsets.UTF_8)));
            JSONArray plans=data.optJSONArray("programs");for(int i=0;i<plans.length();i++){JSONObject p=plans.optJSONObject(i);if(!p.has("category")&&PlanCategory.sheiko(p))Engine.put(p,"category","powerlifting");}
        } catch(Exception e) { throw new IllegalStateException(Lang.t("Не удалось загрузить программу"),e); }
        PlanPresentation.normalize(data);
        SheikoBarbells.migrate(data);
    }
    public synchronized void save() {
        JSONArray allPlans=data.optJSONArray("programs");if(allPlans!=null)for(int i=0;i<allPlans.length();i++){JSONObject p=allPlans.optJSONObject(i);PlanPopularity.identity(p);}
        Engine.clearRest(active());
        JSONObject savedSessions=data.optJSONObject("savedSessions");
        if(savedSessions!=null){java.util.Iterator<String> keys=savedSessions.keys();while(keys.hasNext())Engine.clearRest(savedSessions.optJSONObject(keys.next()));}
        if(!context.getSharedPreferences("podhod",0).edit().putString("state",data.toString()).commit()) throw new IllegalStateException(Lang.t("Не удалось сохранить тренировку"));
        AccountSync.changed(context);
    }
    public JSONObject active() { return data.optJSONObject("active"); }
    public JSONObject sessionFor(JSONObject p){JSONObject s=active();if(s!=null&&s.optString("programId").equals(p.optString("id")))return s;JSONObject saved=data.optJSONObject("savedSessions");return saved==null?null:saved.optJSONObject(p.optString("id"));}
    public JSONObject program(String id) {
        JSONArray a=data.optJSONArray("programs");
        for(int i=0;i<a.length();i++) if(a.optJSONObject(i).optString("id").equals(id)) return a.optJSONObject(i);
        return null;
    }
    public synchronized void deletePlan(String id){if(PlanManagement.delete(data,id))save();}
    public synchronized void removeFromMine(JSONObject p) {
        String id=p.optString("id");JSONObject current=active();
        if(current!=null&&id.equals(current.optString("programId"))){
            JSONObject saved=data.optJSONObject("savedSessions");if(saved==null){saved=new JSONObject();Engine.put(data,"savedSessions",saved);}
            if(!current.optBoolean("paused"))Engine.act(current,"pause",System.currentTimeMillis());
            Engine.put(saved,id,current);data.remove("active");
        }
        Engine.put(p,"mine",false);save();
    }
    public synchronized void start(JSONObject p) {
        if((ZlatAdvanced.is(p)&&(!p.optBoolean("configured")||p.optJSONObject("advancedWeights")==null))||(Zlat.is(p)&&(!p.optBoolean("configured")||p.optJSONObject("workingWeights")==null))||(ZlatIntermediate.is(p)&&(!p.optBoolean("configured")||p.optJSONObject("intermediateWeights")==null)))throw new IllegalStateException(Lang.t("Сначала задай рабочие веса Злата"));
        Engine.selectProgram(data,p,System.currentTimeMillis());save();
    }
    public synchronized void updateMaxima(JSONObject p,JSONObject maxima,int seconds) {
        JSONObject session=sessionFor(p);Engine.updateMaxima(p,maxima,session);
        save();
    }
    public synchronized boolean action(String id,int revision,String action) {
        JSONObject s=active();
        if(s==null||!s.optString("id").equals(id)||s.optInt("revision")!=revision) return false;
        if(action.equals("finish")) {
            if(Engine.doneCount(s)!=s.optJSONArray("sets").length()) return false;
            JSONObject finishingPlan=program(s.optString("programId"));
            if((Zlat.is(finishingPlan)||(ZlatIntermediate.is(finishingPlan)&&ZlatIntermediate.confirmation(s))||(ZlatAdvanced.is(finishingPlan)&&ZlatAdvanced.confirmation(s)))&&s.optInt("confirmedRevision",-1)!=s.optInt("revision"))return false;
            Engine.put(s,"ended",System.currentTimeMillis());data.optJSONArray("history").put(Engine.copy(s));
            JSONObject p=program(s.optString("programId")); if(p!=null){if(ZlatAdvanced.is(p))ZlatAdvanced.advance(p,s);if(Zlat.is(p))Zlat.advance(p,s);if(ZlatIntermediate.is(p))ZlatIntermediate.advance(p,s);Engine.put(p,"nextDay",s.optInt("day")+1);}
            data.remove("active");save();return true;
        }
        boolean changed=Engine.act(s,action,System.currentTimeMillis());if(changed){ZlatIntermediate.syncBackoffs(s);save();}return changed;
    }
    public synchronized boolean setDone(String id,int revision,int index,boolean done){
        JSONObject s=active();if(s==null||!s.optString("id").equals(id)||s.optInt("revision")!=revision)return false;
        boolean changed=Engine.setDone(s,index,done,System.currentTimeMillis());if(changed){ZlatIntermediate.syncBackoffs(s);save();}return changed;
    }
}
