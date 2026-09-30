package com.podhod.app;
import android.widget.*;
import android.view.View;
import com.google.firebase.firestore.*;
import com.google.firebase.auth.FirebaseAuth;
import org.json.JSONObject;
import java.util.*;
import java.lang.ref.WeakReference;

final class PlanLikes {
 final MainActivity a; final String uid; final Map<String,Entry> entries=new HashMap<>(); Runnable catalog;
 static final class Entry {long count;boolean liked,loading,saving,ready;long checked;final List<WeakReference<PlanLikeButton>> buttons=new ArrayList<>();}
 PlanLikes(MainActivity a){this.a=a;uid=FriendsPanel.uid(a);}
 static PlanLikes get(MainActivity a){if(a.planLikes==null||!a.planLikes.uid.equals(FriendsPanel.uid(a)))a.planLikes=new PlanLikes(a);return a.planLikes;}
 boolean valid(){return !a.isDestroyed()&&uid.equals(FriendsPanel.uid(a));}
 Entry entry(JSONObject p){return entries.computeIfAbsent(PlanPopularity.key(p),k->new Entry());}
 long count(JSONObject p){return entry(p).count;}
 void button(LinearLayout box,JSONObject p){
  String key=PlanPopularity.key(p);Entry e=entry(p);PlanLikeButton v=new PlanLikeButton(a);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.topMargin=a.dp(10);lp.bottomMargin=a.dp(14);box.addView(v,lp);e.buttons.add(new WeakReference<>(v));a.clickable(v,()->toggle(key,e));paint(e);load(key,e);
 }
 void paint(Entry e){e.buttons.removeIf(w->w.get()==null);for(WeakReference<PlanLikeButton>w:e.buttons){PlanLikeButton v=w.get();if(v!=null)v.update(e);}}
 void changed(Entry e){paint(e);if(catalog!=null)catalog.run();}
 void load(String key,Entry e){
  if(!AccountSync.configured(a)||e.loading||e.saving||System.currentTimeMillis()-e.checked<60000)return;
  e.loading=true;paint(e);FirebaseFirestore db=FirebaseFirestore.getInstance();DocumentReference stats=db.document("planLikes/"+key);
  stats.get(Source.SERVER).addOnCompleteListener(t->{if(!valid())return;if(!t.isSuccessful()){e.loading=false;e.checked=System.currentTimeMillis();paint(e);return;}Long n=t.getResult().getLong("count");e.count=n==null?0:n;
   if(uid.isEmpty()){e.loading=false;e.ready=true;e.checked=System.currentTimeMillis();changed(e);return;}
   stats.collection("votes").document(uid).get(Source.SERVER).addOnCompleteListener(v->{if(!valid())return;e.loading=false;e.checked=System.currentTimeMillis();if(v.isSuccessful()){e.liked=Boolean.TRUE.equals(v.getResult().getBoolean("liked"));e.ready=true;}changed(e);});
  });
 }
 void toggle(String key,Entry e){
  if(uid.isEmpty()){Toast.makeText(a,AccountPanel.s("Sign in to like plans","Войди в аккаунт, чтобы ставить лайки"),Toast.LENGTH_SHORT).show();return;}
  if(!valid()||e.saving||e.loading)return;
  e.saving=true;paint(e);FirebaseFirestore db=FirebaseFirestore.getInstance();DocumentReference stats=db.document("planLikes/"+key),vote=stats.collection("votes").document(uid);
  db.runTransaction(tx->{if(!valid())throw new FirebaseFirestoreException("Account changed",FirebaseFirestoreException.Code.CANCELLED);DocumentSnapshot s=tx.get(stats),v=tx.get(vote);boolean liked=Boolean.TRUE.equals(v.getBoolean("liked"));long n=s.exists()?s.getLong("count"):0;long next=n+(liked?-1:1);tx.set(stats,Collections.singletonMap("count",next));tx.set(vote,Collections.singletonMap("liked",!liked));return new long[]{next,liked?0:1};})
   .addOnCompleteListener(t->{if(!valid())return;e.saving=false;if(t.isSuccessful()){e.count=t.getResult()[0];e.liked=t.getResult()[1]==1;e.ready=true;e.checked=System.currentTimeMillis();changed(e);}else{paint(e);Toast.makeText(a,AccountPanel.s("Could not save like. Check your connection and retry.","Не удалось сохранить лайк. Проверь соединение и повтори."),Toast.LENGTH_LONG).show();}});
 }
}
