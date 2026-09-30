package com.podhod.app;
import org.json.*;
import java.util.*;
import java.io.*;
import com.google.firebase.firestore.*;
import com.google.android.gms.tasks.Tasks;
import java.util.concurrent.TimeUnit;

/** Reviewed snapshots only. A local edit never modifies the published copy. */
final class CommunityPlans {
 static final String ADMIN="I8mrFaI4vhXFpGOM8Dxmg4mOf7w1";
 static boolean admin(MainActivity a){return ADMIN.equals(FriendsPanel.uid(a));}
 static boolean builtin(JSONObject p){String r=PlanIdentity.routine(p);return Arrays.asList("sheiko-cms-ms","sheiko-12-week","zlat-beginner-2018","zlat-intermediate-trace","zlat-advanced-trace","home-beginner","home-strength").contains(r);}
 static JSONObject snapshot(JSONObject p){
  JSONObject clean=new JSONObject();
  for(String k:new String[]{"name","description","category","categoryName","days","maxima","step","coverImage","coverX","coverY","coverZoom","routine","source","terminalNote","workingWeights","increment","configured","popularityId"})
   if(p.has(k))Engine.put(clean,k,p.opt(k));
  // Do not publish private exercise notes embedded in a day's groups.
  JSONArray days=clean.optJSONArray("days");if(days!=null){days=Engine.copy(p).optJSONArray("days");Engine.put(clean,"days",days);for(int d=0;d<days.length();d++){JSONObject day=days.optJSONObject(d);day.remove("note");JSONArray groups=day.optJSONArray("groups");for(int g=0;g<groups.length();g++)groups.optJSONObject(g).remove("note");}}
  Engine.validateProgram(clean);return clean;
 }
 static Map<String,Object> encode(JSONObject p)throws Exception{
  JSONObject safe=snapshot(p);CloudSnapshot packed=CloudSnapshot.encode(safe);ByteArrayOutputStream out=new ByteArrayOutputStream();for(byte[] c:packed.chunks)out.write(c);byte[] bytes=out.toByteArray();
  if(bytes.length>700000)throw new IllegalArgumentException(AccountPanel.s("Plan is too large to submit. Use a smaller cover image.","План слишком большой для заявки. Выбери обложку меньшего размера."));
  Map<String,Object> data=new HashMap<>();data.put("payload",Blob.fromBytes(bytes));data.put("sha256",packed.sha256);data.put("title",safe.optString("name"));return data;
 }
 static JSONObject decode(Map<String,Object> data)throws Exception{
  byte[] bytes=((Blob)data.get("payload")).toBytes();if(bytes.length>700000)throw new IOException("Oversized catalog plan");List<byte[]> chunks=new ArrayList<>();for(int i=0;i<bytes.length;i+=CloudSnapshot.CHUNK)chunks.add(Arrays.copyOfRange(bytes,i,Math.min(bytes.length,i+CloudSnapshot.CHUNK)));
  JSONObject p=CloudSnapshot.decode(chunks,(String)data.get("sha256"),bytes.length);Engine.validateProgram(p);return p;
 }
 static JSONObject published(DocumentSnapshot doc)throws Exception{
  Map<String,Object> d=doc.getData();JSONObject p=decode(d);Engine.put(p,"catalogId",doc.getId());Engine.put(p,"id","community-"+doc.getId());Engine.put(p,"mine",false);Engine.put(p,"nextDay",0);
  JSONObject author=new JSONObject();Engine.put(author,"uid",d.get("owner"));Engine.put(author,"name",d.get("authorName"));Engine.put(author,"avatar",d.get("authorAvatar"));Engine.put(author,"official",ADMIN.equals(d.get("owner")));Engine.put(p,"author",author);return p;
 }
 static String requestId(String uid,JSONObject p){return uid+"~"+PlanPopularity.key(p);}
 static Map<String,Object> submission(MainActivity a,JSONObject p,FriendsStore store)throws Exception{
  JSONObject author=p.optJSONObject("author");if(author!=null&&!store.uid.equals(author.optString("uid")))throw new IllegalArgumentException(AccountPanel.s("Only the author can submit this plan.","Предложить план может только его автор."));
  Map<String,Object> portrait=store.portrait(store.uid),profile=store.profileData();String name=FriendsProfile.name(portrait,profile==null?"":String.valueOf(profile.get("name")));
  if(name.isBlank())throw new IllegalArgumentException(AccountPanel.s("Set your profile name first.","Сначала укажи имя в профиле."));
  Map<String,Object> data=encode(p);data.put("owner",store.uid);data.put("authorName",name);data.put("authorAvatar",String.valueOf(portrait.getOrDefault("avatar","")));data.put("planKey",PlanPopularity.key(p));data.put("status","pending");data.put("reason","");data.put("submittedAt",FieldValue.serverTimestamp());return data;
 }
 static void submit(MainActivity a,JSONObject p,FriendsStore store)throws Exception{
  String id=requestId(store.uid,p);Map<String,Object> data=submission(a,p,store);
  store.waitFor(store.db.runTransaction(tx->{store.check();DocumentReference ref=store.db.document("catalogSubmissions/"+id);DocumentSnapshot prior=tx.get(ref);if("pending".equals(prior.getString("status")))throw new FirebaseFirestoreException("Already awaiting review",FirebaseFirestoreException.Code.ALREADY_EXISTS);tx.set(ref,data);return null;}));
 }
 static void decide(FriendsStore store,DocumentSnapshot reviewed,boolean approve,String reason)throws Exception{
  if(!ADMIN.equals(store.uid))throw new IllegalStateException("Admin only");
  store.waitFor(store.db.runTransaction(tx->{store.check();DocumentReference request=reviewed.getReference();DocumentSnapshot fresh=tx.get(request);
   if(!"pending".equals(fresh.getString("status"))||!Objects.equals(fresh.getString("sha256"),reviewed.getString("sha256")))throw new FirebaseFirestoreException("Submission changed; refresh review",FirebaseFirestoreException.Code.ABORTED);
   DocumentReference target=store.db.document("publicPlans/"+fresh.getString("planKey"));DocumentSnapshot existing=tx.get(target);
   if(existing.exists()&&!Objects.equals(existing.getString("owner"),fresh.getString("owner")))throw new FirebaseFirestoreException("Plan belongs to another author",FirebaseFirestoreException.Code.PERMISSION_DENIED);
   if(approve){Map<String,Object> publicData=new HashMap<>(fresh.getData());publicData.remove("status");publicData.remove("reason");publicData.put("requestId",fresh.getId());publicData.put("publishedAt",FieldValue.serverTimestamp());tx.set(target,publicData);}
   tx.update(request,"status",approve?"approved":"rejected","reason",approve?"":reason);return null;
  }));
 }
 static <T>T read(com.google.android.gms.tasks.Task<T> task)throws Exception{return Tasks.await(task,45,TimeUnit.SECONDS);}
}
