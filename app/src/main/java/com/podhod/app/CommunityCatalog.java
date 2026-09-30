package com.podhod.app;
import org.json.*;
import java.util.*;
import com.google.firebase.firestore.*;

final class CommunityCatalog {
 final MainActivity a;final ArrayList<JSONObject> published=new ArrayList<>();boolean loading,loaded,failed;long checked;
 CommunityCatalog(MainActivity a){this.a=a;}
 static CommunityCatalog get(MainActivity a){if(a.communityCatalog==null)a.communityCatalog=new CommunityCatalog(a);return a.communityCatalog;}
 static ArrayList<JSONObject> plans(MainActivity a,ArrayList<JSONObject> local){
  return merge(local,get(a).published);
 }
 static ArrayList<JSONObject> merge(List<JSONObject> local,List<JSONObject> published){
  LinkedHashMap<String,JSONObject> result=new LinkedHashMap<>();
  // A saved plan and its approved snapshot share the same identity, even before
  // the saved copy has a catalogId. Keep the local object and its training state.
  for(JSONObject p:local)result.putIfAbsent(catalogKey(p),p);
  for(JSONObject p:published)result.putIfAbsent(catalogKey(p),p);
  return new ArrayList<>(result.values());
 }
 static String catalogKey(JSONObject p){String routine=PlanIdentity.routine(p);if(!routine.isEmpty())return "routine:"+routine;String id=p.optString("catalogId");return id.isEmpty()?PlanPopularity.key(p):id;}

 void load(boolean force){
  if(loading||(!force&&checked>0&&System.currentTimeMillis()-checked<60000)||!AccountSync.configured(a))return;loading=true;
  FriendsPanel.worker.execute(()->{try{
   ArrayList<JSONObject> all=new ArrayList<>();Query query=FirebaseFirestore.getInstance().collection("publicPlans").orderBy(FieldPath.documentId()).limit(50);
   while(true){List<DocumentSnapshot> page=CommunityPlans.read(query.get(Source.SERVER)).getDocuments();for(DocumentSnapshot doc:page)all.add(CommunityPlans.published(doc));if(page.size()<50)break;query=FirebaseFirestore.getInstance().collection("publicPlans").orderBy(FieldPath.documentId()).startAfter(page.get(page.size()-1)).limit(50);}
   a.runOnUiThread(()->{if(a.isDestroyed())return;published.clear();published.addAll(all);loading=false;loaded=true;failed=false;checked=System.currentTimeMillis();if(a.page.equals("library"))a.render();});
  }catch(Exception e){a.runOnUiThread(()->{if(a.isDestroyed())return;loading=false;failed=true;checked=System.currentTimeMillis();if(a.page.equals("library"))a.render();});}});
 }
 static boolean remote(MainActivity a,JSONObject p){return a.store.program(p.optString("id"))==null;}
 static void add(MainActivity a,JSONObject source){
  JSONObject p=a.store.program(source.optString("id"));if(p==null){p=Engine.copy(source);Engine.put(p,"nextDay",0);a.store.data.optJSONArray("programs").put(p);}
  Engine.put(p,"mine",true);Engine.put(p,"lastUsed",System.currentTimeMillis());a.store.save();a.render();
 }
}
