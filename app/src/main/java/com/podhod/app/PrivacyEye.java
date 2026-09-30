package com.podhod.app;
import android.widget.*;
import android.view.*;
import java.util.*;
import org.json.*;
import com.google.firebase.firestore.DocumentSnapshot;
/** Server-confirmed item visibility. Never queues an optimistic public state. */
final class PrivacyEye {
 static String s(String en,String ru){return FriendsPanel.s(en,ru);}
 static final class Controller {
  final MainActivity a;final String uid;final Map<String,Map<String,Object>> items=new HashMap<>();Map<String,Object> categories=new HashMap<>();final List<Row> rows=new ArrayList<>();final Set<String> busy=new HashSet<>();boolean loaded,loading,failed;
  Controller(MainActivity a,String uid){this.a=a;this.uid=uid;}
  boolean current(){return uid.equals(FriendsPanel.uid(a))&&!a.isDestroyed();}
  void paint(){rows.removeIf(r->!r.box.isAttachedToWindow());for(Row r:rows)r.paint();}
  void load(){if(loading||uid.isEmpty())return;loading=true;failed=false;FriendsPanel.run(a,b->{Map<String,Object> result=new HashMap<>();result.put("items",b.allOwnItems());result.put("categories",b.privacyData());return result;},data->{if(!current())return;items.clear();for(Object value:(List<?>)data.get("items")){DocumentSnapshot item=(DocumentSnapshot)value;items.put(item.getId(),item.getData());}categories=(Map<String,Object>)data.get("categories");loaded=true;loading=false;paint();},()->{loading=false;failed=true;paint();});}
 }
 static void compact(MainActivity a,LinearLayout parent,String kind,JSONObject source){add(a,parent,kind,source);LinearLayout box=(LinearLayout)parent.getChildAt(parent.getChildCount()-1);box.getChildAt(0).setVisibility(View.GONE);box.setLayoutParams(new LinearLayout.LayoutParams(a.dp(44),a.dp(48)));}
 static void add(MainActivity a,LinearLayout parent,String kind,JSONObject source){String uid=FriendsPanel.uid(a);if(a.privacyEyes==null||!a.privacyEyes.uid.equals(uid))a.privacyEyes=new Controller(a,uid);Controller c=a.privacyEyes;Row row=new Row(c,parent,kind,source);c.rows.add(row);row.paint();row.box.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){public void onViewAttachedToWindow(View v){if(!c.loaded&&!c.loading)c.load();}public void onViewDetachedFromWindow(View v){c.rows.remove(row);}});a.stepHandler.post(()->{if(row.box.isAttachedToWindow()&&!c.loaded&&!c.loading)c.load();});}
 static final class Row {
  final Controller c;final MainActivity a;final LinearLayout box;final ImageView eye;final TextView label;final String kind,id;final JSONObject source;
  Row(Controller c,LinearLayout parent,String kind,JSONObject source){this.c=c;a=c.a;this.kind=kind;this.source=source;id=SocialPayload.itemId(kind,source.optString("id"));box=new LinearLayout(a);box.setGravity(Gravity.CENTER_VERTICAL);parent.addView(box,new LinearLayout.LayoutParams(-1,a.dp(44)));label=a.text("",12,a.MUTED);box.addView(label,new LinearLayout.LayoutParams(0,-2,1));eye=new ImageView(a);eye.setPadding(a.dp(11),a.dp(11),a.dp(11),a.dp(11));box.addView(eye,new LinearLayout.LayoutParams(a.dp(44),a.dp(44)));a.clickable(eye,this::toggle);}
  void paint(){boolean busy=c.busy.contains(id);int state=ItemPrivacy.visibility(c.items.get(id),c.categories);boolean known=c.loaded||c.uid.isEmpty();String text=busy?s("Saving privacy…","Сохраняем видимость…"):!known?(c.failed?s("Could not check visibility · retry","Не удалось проверить · повторить"):s("Checking visibility…","Проверяем видимость…")):state==1?(kind.equals("walk")?(Boolean.TRUE.equals(c.items.get(id).get("routeShared"))?s("Friends can see the route","Друзья видят маршрут"):s("Friends can see stats only","Друзья видят только результат")):s("Visible to all friends","Видно всем друзьям")):state==2?s("Selected friends · tap for all","Выбранным друзьям · открыть всем"):s("Hidden from friends","Скрыто от друзей");label.setText(text);eye.setImageDrawable(new LineIcon(!known||busy?"eye-pending":state==0?"eye-off":"eye",state==1?a.GREEN:a.MUTED));eye.setContentDescription(text);eye.setEnabled(!busy);}
  void toggle(){if(!c.current())return;if(c.uid.isEmpty()){Toast.makeText(a,s("Sign in to manage visibility for friends","Войди в аккаунт, чтобы менять видимость для друзей"),Toast.LENGTH_LONG).show();return;}if(!c.loaded){c.load();return;}if(c.busy.contains(id))return;boolean visible=ItemPrivacy.visibility(c.items.get(id),c.categories)==1;
   JSONObject payload;try{payload=visible?null:(kind.equals("plan")?SocialPayload.plan(source):SocialPayload.walk(source,true));}catch(Exception e){a.error(e);return;}String title=kind.equals("plan")?Lang.content(source.optString("name")):FriendsPanel.walkName(source);c.busy.add(id);c.paint();
   FriendsPanel.run(a,b->{if(visible)b.hide(id);else b.publish(kind,source.optString("id"),title,payload,null,true);return b.waitFor(b.items(b.uid).document(id).get(com.google.firebase.firestore.Source.SERVER)).getData();},item->{if(!c.current())return;c.items.put(id,item);c.busy.remove(id);c.paint();a.friendsState=null;},()->{c.busy.remove(id);c.paint();});
  }
 }
}
