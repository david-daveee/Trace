package com.podhod.app;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

/** Blocking transport, for a worker thread only. Staged chunks never replace the published head. */
final class CloudStore {
    static final class Revision {
        final String id; final long time; final JSONObject payload;
        Revision(String id,long time,JSONObject payload){this.id=id;this.time=time;this.payload=payload;}
    }
    static final class Conflict extends FirebaseFirestoreException { Conflict(){super("Cloud data changed on another device",Code.FAILED_PRECONDITION);} }
    private final FirebaseFirestore db;
    private final String uid;
    private final java.util.function.BooleanSupplier valid;
    CloudStore(String uid,java.util.function.BooleanSupplier valid) { this.uid=uid; this.valid=valid; db=FirebaseFirestore.getInstance(); }
    private void checkAccount() throws IOException {
        var user=FirebaseAuth.getInstance().getCurrentUser();
        if(!valid.getAsBoolean()||user==null||!uid.equals(user.getUid()))throw new IOException("Account changed; synchronization cancelled");
    }
    private DocumentReference head(){return db.document("users/"+uid+"/sync/head");}
    private DocumentReference revision(String id){return db.document("users/"+uid+"/revisions/"+id);}
    private <T> T waitFor(com.google.android.gms.tasks.Task<T> task)throws Exception {
        T result=Tasks.await(task,45,TimeUnit.SECONDS);checkAccount();return result;
    }
    Revision read() throws Exception {
        checkAccount();
        DocumentSnapshot h=waitFor(head().get(Source.SERVER));
        if(!h.exists())return new Revision("",0,null);
        String id=h.getString("revision");
        if(id==null||!id.matches("[a-f0-9-]{36}"))throw new IOException("Invalid cloud revision");
        DocumentSnapshot m=waitFor(revision(id).get(Source.SERVER));
        Long size=m.getLong("bytes"),count=m.getLong("chunks");String hash=m.getString("sha256");
        if(size==null||count==null||size<1||size>CloudSnapshot.LIMIT||count!=(size+CloudSnapshot.CHUNK-1)/CloudSnapshot.CHUNK||hash==null)throw new IOException("Invalid cloud manifest");
        List<byte[]> chunks=new ArrayList<>();
        for(int i=0;i<count;i++){
            DocumentSnapshot c=waitFor(revision(id).collection("chunks").document(Integer.toString(i)).get(Source.SERVER));
            Blob blob=c.getBlob("data");if(blob==null)throw new IOException("Incomplete cloud snapshot");chunks.add(blob.toBytes());
        }
        Long timestamp=h.getLong("updatedAt");
        return new Revision(id,timestamp==null?0:timestamp,CloudSnapshot.decode(chunks,hash,size.intValue()));
    }
    String publish(JSONObject payload,String expectedRevision)throws Exception {
        checkAccount();CloudSnapshot snapshot=CloudSnapshot.encode(payload);
        String id=UUID.randomUUID().toString();DocumentReference target=revision(id);
        // Small batches avoid Firestore's transaction/request size limit.
        for(int offset=0;offset<snapshot.chunks.size();offset+=16){
            checkAccount();WriteBatch batch=db.batch();
            for(int i=offset;i<Math.min(offset+16,snapshot.chunks.size());i++)
                batch.set(target.collection("chunks").document(Integer.toString(i)),Collections.singletonMap("data",Blob.fromBytes(snapshot.chunks.get(i))));
            waitFor(batch.commit());
        }
        Map<String,Object> metadata=new HashMap<>();metadata.put("bytes",snapshot.bytes);metadata.put("chunks",snapshot.chunks.size());metadata.put("sha256",snapshot.sha256);metadata.put("createdAt",System.currentTimeMillis());
        waitFor(target.set(metadata));checkAccount();
        // The old revision is retained for recovery. Only this transaction publishes the new snapshot.
        String obsolete=waitFor(db.runTransaction(transaction->{
            try { checkAccount(); } catch(IOException e) { throw new FirebaseFirestoreException(e.getMessage(),FirebaseFirestoreException.Code.CANCELLED); }
            DocumentSnapshot current=transaction.get(head());
            String actual=current.exists()?current.getString("revision"):"";
            if(!Objects.equals(expectedRevision,actual))throw new Conflict();
            Map<String,Object> values=new HashMap<>();values.put("revision",id);values.put("previous",actual);values.put("updatedAt",System.currentTimeMillis());
            String previous=current.exists()?current.getString("previous"):null;
            transaction.set(head(),values);return previous;
        }));
        // Retain the current and previous complete versions. Never delete a version reused by a writer.
        if(obsolete!=null&&obsolete.matches("[a-f0-9-]{36}")&&!obsolete.equals(id)&&!obsolete.equals(expectedRevision))try{
            checkAccount();DocumentReference old=revision(obsolete);DocumentSnapshot manifest=waitFor(old.get(Source.SERVER));
            Long count=manifest.getLong("chunks");
            if(count!=null&&count>=0&&count<=CloudSnapshot.LIMIT/CloudSnapshot.CHUNK+1){
                WriteBatch cleanup=db.batch();for(int i=0;i<count;i++)cleanup.delete(old.collection("chunks").document(Integer.toString(i)));
                cleanup.delete(old);checkAccount();waitFor(cleanup.commit());
            }
        }catch(Exception ignored){/* A cleanup failure never invalidates an already published version. */}
        return id;
    }
}
