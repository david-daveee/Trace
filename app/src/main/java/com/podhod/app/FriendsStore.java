package com.podhod.app;

import com.google.android.gms.tasks.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.IOException;

/** Worker-thread API; all social reads are server reads, with no offline visibility promises. */
final class FriendsStore {
    final String uid;final FirebaseFirestore db=FirebaseFirestore.getInstance();
    FriendsStore(){FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();if(user==null)throw new IllegalStateException("Sign in first");uid=user.getUid();}
    void check(){FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();if(u==null||!uid.equals(u.getUid()))throw new IllegalStateException("Account changed");}
    <T>T waitFor(Task<T> task)throws Exception{T result=Tasks.await(task,45,TimeUnit.SECONDS);check();return result;}
    DocumentReference profile(){return db.document("socialProfiles/"+uid);}
    DocumentReference privacy(String owner){return db.document("social/"+owner+"/settings/privacy");}
    CollectionReference items(String owner){return db.collection("social/"+owner+"/items");}
    Map<String,Object> profileData()throws Exception{return waitFor(profile().get(Source.SERVER)).getData();}
    Map<String,Object> portrait(String owner)throws Exception{Map<String,Object> p=waitFor(db.document("social/"+owner+"/settings/profile").get(Source.SERVER)).getData();return p==null?new HashMap<>():p;}
    void updatePortrait(Map<String,Object> fields)throws Exception{check();waitFor(db.document("social/"+uid+"/settings/profile").set(fields,SetOptions.merge()));}
    Map<String,Object> createProfile(String name)throws Exception{
        name=name.trim();if(name.isEmpty()||name.length()>40)throw new IllegalArgumentException("Use 1–40 characters");
        final String label=name,code=UUID.randomUUID().toString().replace("-","").substring(0,12).toUpperCase(Locale.ROOT);
        return waitFor(db.runTransaction(t->{check();DocumentSnapshot existing=t.get(profile());if(existing.exists())return existing.getData();
            DocumentReference index=db.document("friendCodes/"+code);if(t.get(index).exists())throw new FirebaseFirestoreException("Please retry",FirebaseFirestoreException.Code.ABORTED);
            Map<String,Object> p=new HashMap<>();p.put("name",label);p.put("code",code);p.put("uid",uid);t.set(profile(),p);t.set(index,p);
            t.set(privacy(uid),new HashMap<String,Object>(){{put("plansVisible",true);put("walksVisible",true);}});return p;
        }));
    }
    List<DocumentSnapshot> connections()throws Exception{return new ArrayList<>(waitFor(db.collection("connections").whereArrayContains("members",uid).limit(100).get(Source.SERVER)).getDocuments());}
    String peer(DocumentSnapshot c){return uid.equals(c.getString("from"))?c.getString("to"):c.getString("from");}
    String peerName(DocumentSnapshot c){return uid.equals(c.getString("from"))?c.getString("toName"):c.getString("fromName");}
    boolean friends(String peer)throws Exception{
        for(String id:new String[]{uid+"~"+peer,peer+"~"+uid}){DocumentSnapshot c=waitFor(db.document("connections/"+id).get(Source.SERVER));if(c.exists()&&"accepted".equals(c.getString("status")))return true;}return false;
    }
    void request(String input)throws Exception{
        String code=SocialPayload.code(input);if(!code.matches("[A-F0-9]{12}"))throw new IllegalArgumentException("Invalid friend code");
        DocumentSnapshot target=waitFor(db.document("friendCodes/"+code).get(Source.SERVER));
        if(!target.exists())throw new IllegalArgumentException("Friend code not found");String to=target.getString("uid");
        if(uid.equals(to))throw new IllegalArgumentException("This is your own code");Map<String,Object> me=profileData();if(me==null)throw new IllegalStateException("Create your friend code first");
        waitFor(db.runTransaction(t->{check();DocumentReference forward=db.document("connections/"+uid+"~"+to),reverse=db.document("connections/"+to+"~"+uid);
            DocumentSnapshot a=t.get(forward),b=t.get(reverse);if(a.exists()||b.exists())throw new FirebaseFirestoreException("A request or friendship already exists",FirebaseFirestoreException.Code.ALREADY_EXISTS);
            Map<String,Object> data=new HashMap<>();data.put("from",uid);data.put("to",to);data.put("fromName",me.get("name"));data.put("toName",target.getString("name"));data.put("members",Arrays.asList(uid,to));data.put("status","pending");data.put("createdAt",System.currentTimeMillis());t.set(forward,data);return null;
        }));
    }
    void accept(String id)throws Exception{check();waitFor(db.document("connections/"+id).update("status","accepted"));}
    void remove(String id)throws Exception{check();waitFor(db.document("connections/"+id).delete());}
    Map<String,Object> privacyData()throws Exception{Map<String,Object> p=waitFor(privacy(uid).get(Source.SERVER)).getData();return p==null?Collections.emptyMap():p;}
    void privacy(String field,boolean value)throws Exception{check();waitFor(privacy(uid).set(Collections.singletonMap(field,value),SetOptions.merge()));}
    List<DocumentSnapshot> ownItems()throws Exception{return new ArrayList<>(waitFor(items(uid).limit(100).get(Source.SERVER)).getDocuments());}
    List<DocumentSnapshot> allOwnItems()throws Exception{List<DocumentSnapshot> result=new ArrayList<>();Query q=items(uid).orderBy(FieldPath.documentId()).limit(100);while(true){List<DocumentSnapshot> page=waitFor(q.get(Source.SERVER)).getDocuments();result.addAll(page);if(page.size()<100)return result;q=items(uid).orderBy(FieldPath.documentId()).startAfter(page.get(page.size()-1)).limit(100);}}
    List<DocumentSnapshot> feed(String owner,String kind)throws Exception{
        if(!friends(owner))throw new IllegalStateException("Friendship no longer active");
        Map<String,Object> settings=waitFor(privacy(owner).get(Source.SERVER)).getData();
        boolean legacyVisible=settings!=null&&Boolean.TRUE.equals(settings.get(kind.equals("plan")?"plansVisible":"walksVisible"));
        Map<String,DocumentSnapshot> unique=new LinkedHashMap<>();
        Query all=items(owner).whereEqualTo("kind",kind).whereEqualTo("visible",true).whereEqualTo("audience","friends").limit(100);
        Query direct=items(owner).whereEqualTo("kind",kind).whereEqualTo("visible",true).whereEqualTo("audience","direct").whereArrayContains("recipients",uid).limit(100);
        for(Query q:(legacyVisible?new Query[]{all,direct}:new Query[]{all.whereEqualTo("individualPrivacy",true)})){for(DocumentSnapshot item:waitFor(q.get(Source.SERVER)).getDocuments())unique.put(item.getId(),item);}
        ArrayList<DocumentSnapshot> result=new ArrayList<>(unique.values());result.sort((x,y)->Long.compare(y.getLong("updatedAt"),x.getLong("updatedAt")));return result;
    }
    void hide(String id)throws Exception{check();waitFor(items(uid).document(id).update("visible",false));}
    void publish(String kind,String sourceId,String name,JSONObject payload,List<String> recipients)throws Exception{publish(kind,sourceId,name,payload,recipients,false);}
    void publish(String kind,String sourceId,String name,JSONObject payload,List<String> recipients,boolean individual)throws Exception{
        check();SocialPayload.validate(payload,kind);CloudSnapshot snapshot=CloudSnapshot.encode(payload);
        if(snapshot.bytes>5*1024*1024)throw new IOException("Shared item exceeds 5 MB");
        if(recipients!=null)for(String recipient:recipients)if(!friends(recipient))throw new IllegalStateException("Recipient is no longer a friend");
        String id=SocialPayload.itemId(kind,sourceId),revision=UUID.randomUUID().toString();DocumentReference item=items(uid).document(id);
        for(int offset=0;offset<snapshot.chunks.size();offset+=16){check();WriteBatch batch=db.batch();for(int i=offset;i<Math.min(offset+16,snapshot.chunks.size());i++)batch.set(item.collection("revisions").document(revision).collection("chunks").document(Integer.toString(i)),Collections.singletonMap("data",Blob.fromBytes(snapshot.chunks.get(i))));waitFor(batch.commit());}
        Map<String,Object> data=new HashMap<>();data.put("kind",kind);data.put("name",name.length()>120?name.substring(0,120):name);data.put("audience",recipients==null?"friends":"direct");data.put("recipients",recipients==null?Collections.emptyList():recipients);data.put("visible",true);data.put("revision",revision);data.put("bytes",snapshot.bytes);data.put("chunks",snapshot.chunks.size());data.put("sha256",snapshot.sha256);data.put("updatedAt",System.currentTimeMillis());data.put("routeShared",payload.optBoolean("routeShared"));if(individual)data.put("individualPrivacy",true);
        check();waitFor(item.set(data));
    }
    JSONObject download(String owner,String id)throws Exception{
        DocumentReference ref=items(owner).document(id);DocumentSnapshot item=waitFor(ref.get(Source.SERVER));if(!item.exists())throw new IOException("Item is no longer shared");
        String rev=item.getString("revision"),hash=item.getString("sha256"),kind=item.getString("kind");Long bytes=item.getLong("bytes"),count=item.getLong("chunks");
        if(rev==null||!rev.matches("[a-f0-9-]{36}")||bytes==null||bytes<1||bytes>5*1024*1024||count==null||count!=(bytes+CloudSnapshot.CHUNK-1)/CloudSnapshot.CHUNK)throw new IOException("Invalid shared item");
        List<byte[]> chunks=new ArrayList<>();for(int i=0;i<count;i++){Blob b=waitFor(ref.collection("revisions").document(rev).collection("chunks").document(Integer.toString(i)).get(Source.SERVER)).getBlob("data");if(b==null)throw new IOException("Incomplete shared item");chunks.add(b.toBytes());}
        JSONObject result=CloudSnapshot.decode(chunks,hash,bytes.intValue());SocialPayload.validate(result,kind);
        DocumentSnapshot current=waitFor(ref.get(Source.SERVER));if(!current.exists()||!Objects.equals(rev,current.getString("revision"))||!Boolean.TRUE.equals(current.getBoolean("visible")))throw new IOException("Sharing changed; refresh the profile");return result;
    }
}
