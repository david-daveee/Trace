package com.podhod.app;
import android.app.*;
import android.content.*;
import android.view.*;
import android.widget.*;
import com.google.firebase.firestore.*;
import org.json.*;
import java.util.*;

final class WalkSocial {
 static String s(String en,String ru){return FriendsPanel.s(en,ru);}
 static String title(JSONObject walk){String title=walk.optString("title").trim();return title.isEmpty()?s("A walk to remember","Моя прогулка"):title;}
 static void backdrop(MainActivity a,LinearLayout hero,JSONObject walk){if(!walk.optString("coverImage").isEmpty())a.planBackground(hero,walk);else{android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xFF244B4C,0xFF172B3C,0xFF473528});bg.setCornerRadius(a.dp(22));hero.setBackground(bg);}}
 static void metrics(MainActivity a,LinearLayout box,JSONObject walk){LinearLayout row=new LinearLayout(a);box.addView(row);WalkPanel.metric(a,row,s("KILOMETERS","КИЛОМЕТРЫ")).setText(String.format(Lang.locale(),"%.2f",walk.optDouble("meters")/1000));WalkPanel.metric(a,row,s("TIME","ВРЕМЯ")).setText(WalkVisuals.time(walk,System.currentTimeMillis()));WalkPanel.metric(a,row,s("MIN / KM","МИН / КМ")).setText(WalkVisuals.pace(walk,System.currentTimeMillis()));}
 static AlertDialog result(MainActivity a,JSONObject source,Runnable changed){
  JSONObject walk=Engine.copy(source);LinearLayout box=a.form();final AlertDialog[] dialog={null};
  LinearLayout hero=a.card(box);hero.setMinimumHeight(a.dp(120));backdrop(a,hero,walk);a.badge(hero,s("WALK COMPLETE","ПРОГУЛКА ЗАВЕРШЕНА"),0xFFFFB18A);hero.addView(a.title(title(walk),27));hero.addView(a.text(FriendsPanel.walkName(walk),13,a.TEXT));metrics(a,box,walk);
  PrivacyEye.add(a,box,"walk",walk);
  a.button(box,s("Share this walk","Поделиться прогулкой"),true,()->options(a,walk));a.space(box,12);
  WalkMap map=new WalkMap(a);map.route(walk);WalkPanel.mapFrame(a,box,map,260);WalkPanel.mapTools(a,box,map);
  a.link(box,s("Edit title","Изменить название"),()->{LinearLayout form=a.form();EditText input=a.input(form,s("Walk title","Название прогулки"),walk.optString("title"),false);a.formDialog(s("Name your walk","Назови прогулку"),form,s("Save","Сохранить"),()->{String name=input.getText().toString().trim();if(name.length()>100)throw new IllegalArgumentException(s("Use up to 100 characters.","Не больше 100 символов."));Engine.put(walk,"title",name);Walks.metadata(a,walk);dialog[0].dismiss();changed.run();result(a,walk,changed);});});
  a.link(box,s("Add a photo","Добавить фото"),()->{a.walkPhoto=uri->new Thread(()->{try{String encoded=PlanImages.read(a.getApplicationContext(),uri);a.runOnUiThread(()->{if(a.isDestroyed()||!dialog[0].isShowing())return;CoverEditor.show(a,walk,encoded,false,()->{Walks.metadata(a,walk);dialog[0].dismiss();changed.run();result(a,walk,changed);});});}catch(Exception e){a.runOnUiThread(()->a.error(e));}},"walk-photo").start();a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),76);});
  box.addView(a.text(s("Title and photo edits stay private until you share again.","Изменения названия и фото остаются личными, пока ты не поделишься заново."),12,a.MUTED));
  a.link(box,s("Delete walk","Удалить прогулку"),()->new AlertDialog.Builder(a).setTitle(s("Delete this walk?","Удалить прогулку?"))
   .setMessage(s("Shared cards will stop opening after access is revoked. Daily steps are kept.","После закрытия доступа отправленные карточки перестанут открываться. Шаги за день сохранятся."))
   .setNegativeButton(s("Cancel","Отмена"),null).setPositiveButton(s("Delete","Удалить"),(v,w)->{
    Runnable remove=()->{Walks.deleteHistory(a,walk.optString("id"));AccountSync.changed(a);dialog[0].dismiss();changed.run();};
    if(FriendsPanel.uid(a).isEmpty())remove.run();else FriendsPanel.run(a,store->{hide(store,walk.optString("id"));return true;},ok->{invalidate(a);remove.run();});
   }).show());
  dialog[0]=a.panel(s("Your walk","Твоя прогулка"),box,s("Close","Закрыть"),changed,false);return dialog[0];
 }
 static void invalidate(MainActivity a){a.friendsState=null;if(a.privacyEyes!=null){a.privacyEyes.loaded=false;a.privacyEyes.load();}}
 static void options(MainActivity a,JSONObject walk){
  if(FriendsPanel.uid(a).isEmpty()){a.error(new IllegalArgumentException(s("Sign in to share walks with friends.","Войди в аккаунт, чтобы делиться прогулками.")));return;}
  new AlertDialog.Builder(a).setTitle(s("Share walk","Поделиться прогулкой")).setItems(new String[]{s("Show in friends' feed","Показать в ленте друзей"),s("Send to a friend","Отправить другу"),s("Hide from everyone","Скрыть от всех")},(d,index)->{
   if(index==0)preview(a,walk,null,null,null);
   else if(index==1)chooseFriend(a,walk);
   else FriendsPanel.run(a,store->{hide(store,walk.optString("id"));return true;},ok->{invalidate(a);Toast.makeText(a,s("Walk hidden, including chat links","Прогулка скрыта, включая ссылки в чатах"),Toast.LENGTH_SHORT).show();});
  }).show();
 }
 static void hide(FriendsStore store,String walkId)throws Exception{List<DocumentSnapshot> items=store.allOwnItems();for(DocumentSnapshot item:items)if(WalkSharing.belongs(item.getId(),walkId)&&Boolean.TRUE.equals(item.getBoolean("visible")))store.hide(item.getId());}
 static void chooseFriend(MainActivity a,JSONObject walk){FriendsPanel.run(a,store->{List<DocumentSnapshot> rows=new ArrayList<>();for(DocumentSnapshot c:store.connections())if("accepted".equals(c.getString("status")))rows.add(c);return rows;},rows->{if(rows.isEmpty()){a.error(new IllegalArgumentException(s("Add a friend first.","Сначала добавь друга.")));return;}String uid=FriendsPanel.uid(a);String[] names=new String[rows.size()];for(int i=0;i<rows.size();i++){DocumentSnapshot c=rows.get(i);names[i]=uid.equals(c.getString("from"))?c.getString("toName"):c.getString("fromName");}new AlertDialog.Builder(a).setTitle(s("Send to a friend","Отправить другу")).setItems(names,(d,index)->{DocumentSnapshot c=rows.get(index);String peer=uid.equals(c.getString("from"))?c.getString("to"):c.getString("from");preview(a,walk,peer,c.getId(),names[index]);}).show();});}
 static void preview(MainActivity a,JSONObject walk,String peer,String connection,String name){
  LinearLayout box=a.form();box.addView(a.text(peer==null?s("Visible to accepted friends only.","Видно только подтверждённым друзьям."):s("Only this friend can open the chat card.","Карточку в чате сможет открыть только этот друг."),14,a.MUTED));
  CheckBox route=new CheckBox(a);route.setText(s("Include GPS route","Показать GPS-маршрут"));route.setTextColor(a.TEXT);box.addView(route);
  CheckBox trim=new CheckBox(a);trim.setText(s("Hide 200 m around start and finish","Скрыть 200 м вокруг начала и конца"));trim.setTextColor(a.TEXT);trim.setChecked(true);trim.setEnabled(false);box.addView(trim);route.setOnCheckedChangeListener((v,on)->trim.setEnabled(on));
  box.addView(a.text(s("Statistics are shared by default. The preview shows exactly which route points will be sent. A photo may reveal a location too.","По умолчанию отправляется только статистика. Предпросмотр показывает именно те точки маршрута, которые будут отправлены. Фото тоже может раскрыть место."),13,a.MUTED));
  a.link(box,s("Preview shared result","Посмотреть отправляемый результат"),()->FriendsPanel.viewWalk(a,WalkSharing.payload(walk,route.isChecked(),trim.isChecked()?200:0)));
  a.formDialog(s("Walk privacy","Приватность прогулки"),box,peer==null?s("Publish to friends","Показать друзьям"):s("Send card","Отправить карточку"),()->{
   JSONObject payload=WalkSharing.payload(walk,route.isChecked(),trim.isChecked()?200:0);String messageId=Engine.id();
   FriendsPanel.run(a,store->{String item=peer==null?WalkSharing.item(walk.optString("id")):WalkSharing.direct(walk.optString("id"),peer);store.publishTo("walk",item,title(walk),payload,peer==null?null:Collections.singletonList(peer),true);
    if(peer!=null){Map<String,Object> message=new HashMap<>();message.put("sender",store.uid);message.put("text",title(walk)+"\n"+Walks.summary(walk));message.put("sentAt",FieldValue.serverTimestamp());message.put("kind","walk");message.put("walkOwner",store.uid);message.put("walkItem",item);store.waitFor(store.db.document("connections/"+connection+"/messages/"+messageId).set(message));}return true;
   },ok->{invalidate(a);Toast.makeText(a,peer==null?s("Shared with friends","Опубликовано для друзей"):s("Walk card sent","Карточка отправлена"),Toast.LENGTH_SHORT).show();if(peer!=null)FriendChat.open(a,connection,name);});
  });
 }
 static void openShared(MainActivity a,String owner,String item){FriendsPanel.run(a,store->store.download(owner,item),payload->FriendsPanel.viewWalk(a,payload));}
}
