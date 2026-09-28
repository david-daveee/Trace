package com.podhod.app;

import android.content.*;
import android.os.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.*;
import java.util.concurrent.*;
import org.json.JSONObject;

final class AccountSync {
    interface Listener { void done(); void error(Exception e); }
    static final class Pending {
        final String uid,localHash;final int epoch;final JSONObject local;final CloudStore.Revision remote;
        Pending(String uid,int epoch,JSONObject local,CloudStore.Revision remote){this.uid=uid;this.epoch=epoch;this.local=local;this.remote=remote;localHash=SyncPayload.fingerprint(local);}
    }
    private static AccountSync instance;
    static synchronized AccountSync get(Context c){if(instance==null)instance=new AccountSync(c.getApplicationContext());return instance;}
    static boolean configured(Context c){return !FirebaseApp.getApps(c).isEmpty();}
    private final Context context;private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private volatile int epoch;private boolean busy;
    private AccountSync(Context c){context=c;if("syncing".equals(prefs().getString("status","")))prefs().edit().putString("status","pending").apply();}
    private final Runnable afterEdit=()->refresh();
    static void changed(Context c){AccountSync sync=get(c);sync.main.removeCallbacks(sync.afterEdit);sync.main.postDelayed(sync.afterEdit,5000);}
    SharedPreferences prefs(){return context.getSharedPreferences("trace-account",0);}
    FirebaseUser user(){return configured(context)?FirebaseAuth.getInstance().getCurrentUser():null;}
    boolean enabled(){FirebaseUser u=user();return u!=null&&u.getUid().equals(prefs().getString("consentUid",""));}
    boolean busy(){return busy;}
    boolean same(String uid,int token){FirebaseUser u=user();return token==epoch&&u!=null&&u.getUid().equals(uid);}
    void state(String value){prefs().edit().putString("status",value).apply();context.sendBroadcast(new Intent("com.podhod.app.CHANGED").setPackage(context.getPackageName()),"com.podhod.app.INTERNAL");}
    void resetConsent(){epoch++;busy=false;prefs().edit().remove("consentUid").remove("baseRevision").remove("baseHash").remove("lastSync").putString("status","pending").commit();}
    void signOut(){resetConsent();FirebaseAuth.getInstance().signOut();AccountSyncJob.cancel(context);state("guest");}
    void refresh(){if(user()!=null&&!busy&&!prefs().getString("status", "").equals("restored"))prepare(null,false);}
    void prepare(Listener listener,boolean accountFirst){
        FirebaseUser u=user();if(u==null||busy)return;

        String uid=u.getUid();int token=epoch;JSONObject local=SyncLocal.capture(context);String hash=SyncPayload.fingerprint(local);
        String base=prefs().getString("baseRevision",""),baseHash=prefs().getString("baseHash","");boolean consent=enabled();
        busy=true;state("syncing");
        worker.execute(()->{try{
            CloudStore.Revision remote=new CloudStore(uid,()->same(uid,token)).read();if(remote.payload!=null)SyncLocal.validate(remote.payload);
            main.post(()->{
                if(!same(uid,token))return;busy=false;
                if(!hash.equals(SyncPayload.fingerprint(SyncLocal.capture(context)))){state("pending");if(listener!=null)listener.error(new IllegalStateException(AccountPanel.s("Data changed while checking. Please sync again.","Данные изменились во время проверки. Повтори синхронизацию.")));return;}
                Pending pending=new Pending(uid,token,local,remote);
                SyncDecision.Action action=SyncDecision.decide(consent&&!accountFirst,base,remote.id,baseHash,hash);
                if(action==SyncDecision.Action.NOTHING){state("synced");if(listener!=null)listener.done();}
                else accept(pending,action==SyncDecision.Action.PHONE,listener);
            });
        }catch(Exception e){main.post(()->fail(uid,token,listener,e));}});
    }
    void accept(Pending pending,boolean usePhone,Listener listener){
        if(busy||!same(pending.uid,pending.epoch))return;
        if(!pending.localHash.equals(SyncPayload.fingerprint(SyncLocal.capture(context)))){state("pending");if(listener!=null)listener.error(new IllegalStateException(AccountPanel.s("Phone data changed. Check the account again before choosing.","Данные телефона изменились. Повтори проверку аккаунта перед выбором.")));return;}

        JSONObject chosen=pending.remote.payload==null?pending.local:SyncPayload.choose(pending.local,pending.remote.payload,usePhone);
        try { SyncLocal.validate(chosen); } catch(Exception e) { fail(pending.uid,pending.epoch,listener,e); return; }
        JSONObject rollback=FullBackup.create(context,Store.get(context).data);
        busy=true;state("syncing");
        worker.execute(()->{try{
            SyncLocal.backup(context,rollback);
            String revision=pending.remote.id;
            if(pending.remote.payload==null||!SyncPayload.fingerprint(chosen).equals(SyncPayload.fingerprint(pending.remote.payload)))revision=new CloudStore(pending.uid,()->same(pending.uid,pending.epoch)).publish(chosen,pending.remote.id);
            final String published=revision;
            main.post(()->{if(!same(pending.uid,pending.epoch))return;try{
                if(!pending.localHash.equals(SyncPayload.fingerprint(SyncLocal.capture(context))))throw new IllegalStateException(AccountPanel.s("Phone data changed during transfer. Nothing on this phone was replaced; sync again.","Данные телефона изменились во время передачи. На телефоне ничего не заменено — повтори синхронизацию."));
                SyncLocal.apply(context,chosen);
                String appliedHash=SyncPayload.fingerprint(chosen);
                if(!prefs().edit().putString("consentUid",pending.uid).putString("baseRevision",published).putString("baseHash",appliedHash).putLong("lastSync",System.currentTimeMillis()).commit())throw new IllegalStateException("Cannot save sync status");
                busy=false;state("synced");AccountSyncJob.schedule(context);if(listener!=null)listener.done();
            }catch(Exception e){fail(pending.uid,pending.epoch,listener,e);}});
        }catch(Exception e){main.post(()->fail(pending.uid,pending.epoch,listener,e));}});
    }
    private void fail(String uid,int token,Listener listener,Exception e){if(!same(uid,token))return;busy=false;state("error");if(listener!=null)listener.error(e);}
}
