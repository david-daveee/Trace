"""Runs only against the local Firestore emulator with disposable demo data."""
import base64, json, time, urllib.request, urllib.error, os
HOST="http://127.0.0.1:"+str(int(os.environ.get("TRACE_EMULATOR_PORT","8181")))
PROJECT="demo-trace-friends"
BASE=f"{HOST}/v1/projects/{PROJECT}/databases/(default)/documents"
PREFIX=f"projects/{PROJECT}/databases/(default)/documents/"
checks=0

def token(uid):
    enc=lambda v:base64.urlsafe_b64encode(json.dumps(v).encode()).decode().rstrip("=")
    return enc({"alg":"none","typ":"JWT"})+"."+enc({"iss":f"https://securetoken.google.com/{PROJECT}","aud":PROJECT,"iat":int(time.time()),"exp":int(time.time())+3600,"sub":uid,"user_id":uid,"email":uid+"@gmail.com","email_verified":uid!="unverified","firebase":{"identities":{},"sign_in_provider":"custom"}})+"."
def call(url,method="GET",data=None,uid=None,expected=200,label=""):
    global checks
    headers={"Content-Type":"application/json"}
    if uid:headers["Authorization"]="Bearer "+token(uid)
    req=urllib.request.Request(url,data=None if data is None else json.dumps(data).encode(),headers=headers,method=method)
    try:
        with urllib.request.urlopen(req,timeout=30) as response:status=response.status;body=response.read()
    except urllib.error.HTTPError as e:status=e.code;body=e.read()
    if status!=expected:raise AssertionError(f"{label}: expected {expected}, got {status}: {body.decode()[:1000]}")
    checks+=1;print("PASS",label or method)
    return json.loads(body) if body else None

def value(v):
    if isinstance(v,bool):return {"booleanValue":v}
    if isinstance(v,int):return {"integerValue":str(v)}
    if isinstance(v,str):return {"stringValue":v}
    if isinstance(v,bytes):return {"bytesValue":base64.b64encode(v).decode()}
    if isinstance(v,list):return {"arrayValue":{"values":[value(x) for x in v]}}
    if isinstance(v,dict):return {"mapValue":{"fields":fields(v)}}
    raise TypeError(v)
def fields(d):return {k:value(v) for k,v in d.items()}
def write(path,d):return {"update":{"name":PREFIX+path,"fields":fields(d)}}
def put(path,d,uid,expected=200,label=""):
    return call(BASE+":commit","POST",{"writes":[write(path,d)]},uid,expected,label)
def get(path,uid,expected=200,label=""):
    return call(BASE+"/"+path,uid=uid,expected=expected,label=label)
def query(parent,collection,filters,uid,expected=200,label=""):
    clauses=[{"fieldFilter":{"field":{"fieldPath":key},"op":op,"value":value(v)}} for key,op,v in filters]
    q={"from":[{"collectionId":collection}],"limit":100}
    if clauses:q["where"]={"compositeFilter":{"op":"AND","filters":clauses}}
    return call(BASE+("/"+parent if parent else "")+":runQuery","POST",{"structuredQuery":q},uid,expected,label)

call(f"{HOST}/emulator/v1/projects/{PROJECT}/databases/(default)/documents","DELETE",label="Reset disposable demo database")
for uid,code in [("alice","AAAAAAAAAAAA"),("bob","BBBBBBBBBBBB"),("carol","CCCCCCCCCCCC")]:
    p={"uid":uid,"name":uid.title(),"code":code}
    call(BASE+":commit","POST",{"writes":[write("socialProfiles/"+uid,p),write("friendCodes/"+code,p),write(f"social/{uid}/settings/privacy",{"plansVisible":True,"walksVisible":True})]},uid,label="Create own profile and code: "+uid)
emailPath="friendEmails/alice@gmail.com"
get(emailPath,"alice",404,"Owner reads missing Gmail opt-in")
put(emailPath,{"uid":"alice","code":"AAAAAAAAAAAA","enabled":True},"alice",label="Verified Gmail owner enables lookup")
get(emailPath,"bob",label="Exact enabled Gmail lookup")
get(emailPath,None,403,"Anonymous cannot look up Gmail")
query("","friendEmails",[],"bob",403,"Cannot enumerate Gmail directory")
put(emailPath,{"uid":"bob","code":"BBBBBBBBBBBB","enabled":True},"bob",403,"Cannot claim another Gmail")
put("friendEmails/unverified@gmail.com",{"uid":"unverified","code":"AAAAAAAAAAAA","enabled":True},"unverified",403,"Unverified email cannot enable lookup")
put(emailPath,{"uid":"alice","code":"BBBBBBBBBBBB","enabled":True},"alice",403,"Cannot redirect Gmail to another code")
put(emailPath,{"uid":"bob","code":"AAAAAAAAAAAA","enabled":True},"alice",403,"Cannot redirect Gmail UID")
put(emailPath,{"uid":"alice","code":"AAAAAAAAAAAA","enabled":False},"alice",label="Disable Gmail lookup")
get(emailPath,"bob",403,"Disabled Gmail is not discoverable")
get(emailPath,"alice",label="Owner can read disabled lookup")
get("friendEmails/absent@gmail.com","bob",403,"Missing Gmail is not discoverable")
put("social/alice/settings/profile",{"displayName":"Ace","bio":"Streetlifting","avatar":"sample"},"alice",label="Owner edits voluntary profile")
get("social/alice/settings/profile","alice",label="Owner reads profile")
get("social/alice/settings/profile","bob",403,"Stranger cannot read profile")
get("social/alice/settings/profile",None,403,"Anonymous cannot read profile")
put("social/alice/settings/profile",{"bio":"intruder"},"bob",403,"Other account cannot edit profile")
put("social/alice/settings/profile",{"bio":"a"*161},"alice",403,"Reject oversized profile bio")
put("social/alice/settings/profile",{"avatar":"a"*160001},"alice",403,"Reject oversized avatar")
get("friendCodes/AAAAAAAAAAAA","bob",label="Exact code lookup")
get("friendCodes/AAAAAAAAAAAA",None,403,"No anonymous code lookup")
query("","friendCodes",[],"bob",403,"Cannot enumerate codes")
put("socialProfiles/alice",{"uid":"alice","name":"Impersonated","code":"AAAAAAAAAAAA"},"bob",403,"Cannot impersonate profile")
put("users/alice/sync/head",{"revision":"private"},"alice",label="Own private sync works")
def connection(to,status="pending"):
 return {"from":"alice","to":to,"fromName":"Alice","toName":to.title(),"members":["alice",to],"status":status,"createdAt":1}
put("connections/alice~bob",connection("bob","accepted"),"alice",403,"Cannot self-approve friendship")
put("connections/alice~bob",connection("bob"),"alice",label="Send request")
get("connections/alice~carol","alice",404,"Missing peer lookup allowed")
get("connections/alice~bob","carol",403,"Stranger cannot read request")
query("","connections",[("members","ARRAY_CONTAINS","bob")],"bob",label="Recipient can list own requests")
query("","connections",[],"bob",403,"Cannot list other requests")
rev="11111111-1111-1111-1111-111111111111"
item={"kind":"plan","name":"Training","audience":"friends","recipients":[],"visible":True,"revision":rev,"bytes":3,"chunks":1,"sha256":"a"*64,"updatedAt":1,"routeShared":False}
path="social/alice/items/plan-test"
put(path+f"/revisions/{rev}/chunks/0",{"data":b"abc"},"alice",label="Stage own share chunk")
put(path,item,"alice",label="Publish selected plan")
get(path,"bob",403,"Pending request has no access")
put("connections/alice~bob",connection("bob","accepted"),"alice",403,"Sender still cannot accept")
put("connections/alice~bob",connection("bob","accepted"),"bob",label="Recipient accepts")
get(path,"bob",label="Accepted friend reads selected plan")
get("social/alice/settings/profile","bob",label="Accepted friend reads profile")
get(path,None,403,"No anonymous plan access")
get(path,"carol",403,"Stranger cannot read plan")
get("users/alice/sync/head","bob",403,"Friend cannot read private sync")
get(path+f"/revisions/{rev}/chunks/0","bob",label="Friend reads current shared chunks")
get(path+"/revisions/22222222-2222-2222-2222-222222222222/chunks/0","bob",403,"Old chunks remain inaccessible")
query("social/alice","items",[("kind","EQUAL","plan"),("visible","EQUAL",True),("audience","EQUAL","friends")],"bob",label="Friend feed query works")
query("social/alice","items",[],"bob",403,"Unfiltered feed cannot expose hidden items")
put("social/alice/settings/privacy",{"plansVisible":False,"walksVisible":True},"alice",label="Hide all plans")
get(path,"bob",403,"Category hide revokes plan")
get(path+f"/revisions/{rev}/chunks/0","bob",403,"Category hide revokes chunks")
eye=dict(item,individualPrivacy=True)
eyePath="social/alice/items/plan-eye"
put(eyePath,eye,"alice",label="Open eye on one plan with legacy category hidden")
get(eyePath,"bob",label="Eye enables exactly selected plan")
get(path,"bob",403,"Other plans remain hidden")
query("social/alice","items",[("kind","EQUAL","plan"),("visible","EQUAL",True),("audience","EQUAL","friends"),("individualPrivacy","EQUAL",True)],"bob",label="Individual visibility feed query")
eye["visible"]=False;put(eyePath,eye,"alice",label="Close eye")
get(eyePath,"bob",403,"Closed eye revokes access")
put("social/alice/settings/privacy",{"plansVisible":True,"walksVisible":True},"alice",label="Restore category visibility")
put("connections/alice~carol",connection("carol"),"alice",label="Request second friend")
put("connections/alice~carol",connection("carol","accepted"),"carol",label="Second friend accepts")
item.update(audience="direct",recipients=["bob"])
put(path,item,"alice",label="Share only with Bob")
get(path,"carol",403,"Other accepted friend cannot read direct share")
query("social/alice","items",[("kind","EQUAL","plan"),("visible","EQUAL",True),("audience","EQUAL","direct"),("recipients","ARRAY_CONTAINS","bob")],"bob",label="Direct-share query works")
item["visible"]=False;put(path,item,"alice",label="Hide individual plan")
get(path,"bob",403,"Individual hide revokes access")
item["visible"]=True;put(path,item,"alice",label="Reshare item")
# Text messages: only accepted participants; immutable and server-timestamped.
chat="connections/alice~bob/messages/one"
def message(path,sender,text,actor,expected=200,label=""):
 w=write(path,{"sender":sender,"text":text});w["updateTransforms"]=[{"fieldPath":"sentAt","setToServerValue":"REQUEST_TIME"}]
 return call(BASE+":commit","POST",{"writes":[w]},actor,expected,label)
message(chat,"alice","Hello Bob","alice",label="Participant sends chat message")
get(chat,"bob",label="Recipient reads chat")
get(chat,"carol",403,"Another accepted friend cannot read chat")
get(chat,None,403,"Anonymous cannot read chat")
query("connections/alice~bob","messages",[],"bob",label="Participant reads bounded chat history")
query("connections/alice~bob","messages",[],"carol",403,"Other friend cannot list chat")
message("connections/alice~bob/messages/spoof","alice","Fake","bob",403,"Cannot impersonate message sender")
message("connections/alice~bob/messages/empty","alice","","alice",403,"Reject empty message")
message("connections/alice~bob/messages/long","alice","x"*2001,"alice",403,"Reject oversized message")
message(chat,"alice","Changed","alice",403,"Messages cannot be overwritten")
call(BASE+"/"+chat,"DELETE",uid="alice",expected=403,label="Message deletion is not exposed")
put("connections/alice~bob/messages/clock",{"sender":"alice","text":"Bad time","sentAt":1},"alice",403,"Reject client timestamp")
call(BASE+"/connections/alice~bob","DELETE",uid="bob",label="Either friend can remove friendship")
get(path,"bob",403,"Unfriend revokes shared plan")
get("social/alice/settings/profile","bob",403,"Unfriend revokes profile access")
get(path+f"/revisions/{rev}/chunks/0","bob",403,"Unfriend revokes chunks")
put(path,item,"bob",403,"Friend cannot change owner publications")
get(chat,"alice",403,"Removed friendship revokes chat history")
message("connections/alice~bob/messages/after","alice","No access","alice",403,"Removed friendship cannot send")
print(f"{checks} Firestore access checks passed")
