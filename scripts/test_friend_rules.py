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
# Walk cards reference an owner's publication; they never bypass its visibility.
walkId="walk-"+"b"*64+"-"+"c"*64
walkPath="social/alice/items/"+walkId
walkItem=dict(item,kind="walk",individualPrivacy=True,recipients=["bob"],visible=True)
put(walkPath,walkItem,"alice",label="Publish private walk for Bob")
def walk_message(suffix,actor="alice",owner="alice",ref=walkId,expected=200,extra=None):
 d={"sender":actor,"text":"Evening walk · 3 km","kind":"walk","walkOwner":owner,"walkItem":ref}
 if extra:d.update(extra)
 w=write("connections/alice~bob/messages/"+suffix,d);w["updateTransforms"]=[{"fieldPath":"sentAt","setToServerValue":"REQUEST_TIME"}]
 return call(BASE+":commit","POST",{"writes":[w]},actor,expected,"Walk card: "+suffix)
walk_message("valid")
get("connections/alice~bob/messages/valid","bob",label="Friend reads walk card")
get(walkPath,"bob",label="Recipient can open referenced walk")
get(walkPath,"carol",403,"Other friend cannot open direct walk")
walk_message("wrong-owner",owner="carol",expected=403)
walk_message("forged-sender",actor="bob",expected=403)
walk_message("missing",ref="walk-"+"d"*64,expected=403)
walk_message("extra-gps",extra={"points":[1,2]},expected=403)
walk_message("stranger",actor="carol",expected=403)
walkItem["recipients"]=["carol"];put(walkPath,walkItem,"alice",label="Walk shared with a different recipient")
walk_message("wrong-recipient",expected=403)
walkItem["recipients"]=["bob"];walkItem["visible"]=False;put(walkPath,walkItem,"alice",label="Hide referenced walk")
walk_message("hidden",expected=403)
get(walkPath,"bob",403,"Existing card cannot reopen a hidden walk")
walkItem.update(visible=True,audience="friends",recipients=[]);put(walkPath,walkItem,"alice",label="Publish walk for friends")
walk_message("friends-visible")

call(BASE+"/connections/alice~bob","DELETE",uid="bob",label="Either friend can remove friendship")
get(path,"bob",403,"Unfriend revokes shared plan")
get("social/alice/settings/profile","bob",403,"Unfriend revokes profile access")
get(path+f"/revisions/{rev}/chunks/0","bob",403,"Unfriend revokes chunks")
put(path,item,"bob",403,"Friend cannot change owner publications")
get(chat,"alice",403,"Removed friendship revokes chat history")
message("connections/alice~bob/messages/after","alice","No access","alice",403,"Removed friendship cannot send")
print(f"{checks} Firestore access checks passed")


like="planLikes/"+("a"*64)
get(like,None,404,"Guest reads missing public count")
put(like,{"count":99},"alice",403,"Cannot forge count without a vote")
put(like+"/votes/alice",{"liked":True},"alice",403,"Cannot vote without counter transaction")
def vote(uid,count,liked,expected=200):
    return call(BASE+":commit","POST",{"writes":[write(like,{"count":count}),write(like+"/votes/"+uid,{"liked":liked})]},uid,expected,"Atomic like "+uid+" "+str(count)+" "+str(liked))
vote("alice",1,True)
vote("alice",2,True,403)
vote("bob",2,True)
get(like,None,label="Guests see total likes")
get(like+"/votes/alice","alice",label="Owner sees own vote")
get(like+"/votes/alice","bob",403,"Cannot read another vote")
query(like,"votes",[],"alice",403,"Cannot enumerate voters")
vote("alice",1,False)
vote("alice",0,False,403)
vote("bob",0,False)
vote("alice",1,True)
call(BASE+":commit","POST",{"writes":[write(like,{"count":2}),write(like+"/votes/carol",{"liked":True})]},"alice",403,"Cannot vote for another account")
vote("bob",100,True,403)
put(like,{"count":-1},"alice",403,"Cannot make count negative")
print("ALL CHECKS PASSED:",checks)


admin="I8mrFaI4vhXFpGOM8Dxmg4mOf7w1"
planKey="b"*64
requestPath="catalogSubmissions/alice~"+planKey
publicPath="publicPlans/"+planKey
def submission_write(path,owner="alice",status="pending",reason="",payload=b"snapshot"):
    w=write(path,{"owner":owner,"authorName":"Alice","authorAvatar":"","planKey":planKey,"title":"Test plan","payload":payload,"sha256":"c"*64,"status":status,"reason":reason})
    w["updateTransforms"]=[{"fieldPath":"submittedAt","setToServerValue":"REQUEST_TIME"}]
    return w
get(requestPath,"alice",404,"Owner reads missing submission")
call(BASE+":commit","POST",{"writes":[submission_write(requestPath)]},"alice",label="Author submits snapshot")
get(requestPath,"alice",label="Author sees own submission")
get(requestPath,"bob",403,"Other user cannot read submission")
get(requestPath,None,403,"Guest cannot read submission")
get(requestPath,admin,label="Admin reads submission")
query("","catalogSubmissions",[("owner","EQUAL","alice")],"alice",label="Owner lists own submissions")
query("","catalogSubmissions",[],"alice",403,"User cannot list review queue")
query("","catalogSubmissions",[("status","EQUAL","pending")],admin,label="Admin lists pending submissions")
put(publicPath,{"title":"bypass"},"alice",403,"Cannot publish without admin")
put("catalogSubmissions/bob~"+planKey,{"owner":"bob"},"alice",403,"Cannot impersonate author")
def decision(status,reason="",who=admin,publish=False):
    fields0=get(requestPath,who=who) if False else get(requestPath,admin)["fields"]
    request=write(requestPath,{})
    request["update"]["fields"]=dict(fields0,status=value(status),reason=value(reason))
    writes=[request]
    if publish:
        public={k:v for k,v in fields0.items() if k not in ("status","reason")}
        public["requestId"]=value("alice~"+planKey)
        w={"update":{"name":PREFIX+publicPath,"fields":public},"updateTransforms":[{"fieldPath":"publishedAt","setToServerValue":"REQUEST_TIME"}]}
        writes.append(w)
    return {"writes":writes}
call(BASE+":commit","POST",decision("approved",who="alice",publish=True),"alice",403,"Author cannot approve own submission")
call(BASE+":commit","POST",{"writes":[submission_write(requestPath,payload=b"changed")]},"alice",403,"Cannot silently replace pending snapshot")
call(BASE+":commit","POST",decision("approved",publish=True),admin,label="Admin approves exact snapshot atomically")
get(publicPath,None,label="Guests read approved plan")
query("","publicPlans",[],None,label="Guests browse public catalog")
put(publicPath,{"title":"changed"},"alice",403,"Author cannot edit published copy")
call(BASE+":commit","POST",{"writes":[submission_write(requestPath,payload=b"revision2")]},"alice",label="Author submits new revision")
assert get(publicPath,None)["fields"]["payload"]==value(b"snapshot")
call(BASE+":commit","POST",decision("rejected",""),admin,403,"Rejection needs explanation")
call(BASE+":commit","POST",decision("rejected","Please clarify the exercises"),admin,label="Admin rejects revision with reason")
assert get(publicPath,None)["fields"]["payload"]==value(b"snapshot")
print("ALL CHECKS INCLUDING MODERATION PASSED:",checks)

# Moderator content editing: identity and authorship remain immutable.
call(BASE+":commit","POST",{"writes":[submission_write(requestPath,payload=b"draft3")]},"alice",label="Author submits editable draft")
def patch_existing(path,changes,uid,expected=200,label="",timestamp=False):
    old=get(path,admin)["fields"]
    updated=dict(old,**fields(changes))
    w={"update":{"name":PREFIX+path,"fields":updated}}
    if timestamp:w["updateTransforms"]=[{"fieldPath":"publishedAt","setToServerValue":"REQUEST_TIME"}]
    return call(BASE+":commit","POST",{"writes":[w]},uid,expected,label)
patch_existing(requestPath,{"title":"Reviewed title","payload":b"reviewed","sha256":"d"*64},admin,label="Moderator edits pending content")
patch_existing(requestPath,{"owner":"bob"},admin,403,"Moderator cannot transfer submission author")
patch_existing(requestPath,{"planKey":"e"*64},admin,403,"Moderator cannot change submission identity")
patch_existing(requestPath,{"payload":b"bad"},"alice",403,"Author cannot replace moderator draft while pending")
patch_existing(requestPath,{"title":""},admin,403,"Moderator cannot save empty title")
call(BASE+":commit","POST",decision("approved",publish=True),admin,label="Approve edited draft atomically")
assert get(publicPath,None)["fields"]["payload"]==value(b"reviewed")
patch_existing(publicPath,{"title":"Corrected public title","payload":b"corrected","sha256":"e"*64},admin,label="Moderator corrects public content",timestamp=True)
assert get(publicPath,None)["fields"]["owner"]==value("alice")
patch_existing(publicPath,{"owner":admin},admin,403,"Cannot steal public authorship",timestamp=True)
patch_existing(publicPath,{"authorName":"Trace"},admin,403,"Cannot change public author credit",timestamp=True)
patch_existing(publicPath,{"planKey":"f"*64},admin,403,"Cannot move public vote identity",timestamp=True)
patch_existing(publicPath,{"title":"User edit"},"alice",403,"Author cannot bypass moderation",timestamp=True)
patch_existing(publicPath,{"title":"Stranger edit"},"bob",403,"Other account cannot edit library",timestamp=True)
patch_existing(publicPath,{"title":"Guest edit"},None,403,"Guest cannot edit library",timestamp=True)
newkey="f"*64
builtin=write("publicPlans/"+newkey,{"owner":admin,"authorName":"Trace","authorAvatar":"","planKey":newkey,"requestId":admin+"~"+newkey,"title":"Sheiko update","payload":b"builtin","sha256":"a"*64})
builtin["updateTransforms"]=[{"fieldPath":k,"setToServerValue":"REQUEST_TIME"} for k in ["submittedAt","publishedAt"]]
call(BASE+":commit","POST",{"writes":[builtin]},"alice",403,"Regular account cannot publish built-in override")
call(BASE+":commit","POST",{"writes":[builtin]},admin,label="Moderator publishes built-in override")
get("publicPlans/"+newkey,None,label="Guests can load corrected built-in")
print("ALL CHECKS INCLUDING MODERATOR EDITS PASSED:",checks)
