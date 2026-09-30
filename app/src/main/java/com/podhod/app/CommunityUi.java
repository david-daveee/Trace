package com.podhod.app;
import android.app.*;
import android.widget.*;
import android.view.*;
import org.json.*;
import java.util.*;
import com.google.firebase.firestore.*;

final class CommunityUi {
 static String s(String en,String ru){return AccountPanel.s(en,ru);}
 static void shortcuts(MainActivity a,LinearLayout host){
  if(FriendsPanel.uid(a).isEmpty())return;
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);
  LinearLayout.LayoutParams rowParams=new LinearLayout.LayoutParams(-1,-2);rowParams.setMargins(0,a.dp(8),0,a.dp(16));host.addView(row,rowParams);
  shortcut(a,row,"file",s("My submissions","Мои заявки"),false,()->queue(a,false));
  if(CommunityPlans.admin(a))shortcut(a,row,"moderator",s("In review","На проверке"),true,()->queue(a,true));
 }
 static void shortcut(MainActivity a,LinearLayout row,String icon,String label,boolean review,Runnable action){
  LinearLayout button=new LinearLayout(a);button.setGravity(Gravity.CENTER_VERTICAL);button.setPadding(a.dp(12),a.dp(10),a.dp(12),a.dp(10));button.setMinimumHeight(a.dp(48));
  button.setBackground(a.shape(review?0xFF25243A:0xFF202532,14));button.setContentDescription(label);button.setTooltipText(label);
  ImageView image=new ImageView(a);image.setImageDrawable(new LineIcon(icon,review?0xFFC5ADFF:a.MUTED));image.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);button.addView(image,new LinearLayout.LayoutParams(a.dp(18),a.dp(18)));
  TextView text=a.text(label,13,review?0xFFE1D6FF:a.TEXT);text.setMaxLines(2);text.setPadding(a.dp(8),0,0,0);button.addView(text,new LinearLayout.LayoutParams(0,-2,1));
  LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-2,1);if(row.getChildCount()>0)params.leftMargin=a.dp(8);row.addView(button,params);a.clickable(button,action);
 }
 static void submit(MainActivity a,JSONObject p){
  if(FriendsPanel.uid(a).isEmpty()){a.error(new IllegalStateException(s("Sign in to submit a plan.","Войди, чтобы предложить план.")));return;}
  JSONObject copy=Engine.copy(p);PlanPopularity.identity(copy);
  new AlertDialog.Builder(a).setTitle(s("Submit for review?","Предложить в библиотеку?"))
   .setMessage(s("Use English for the plan title, description, day names and exercises. The moderator will check the language and review a copy of this plan. If approved, its exercises, cover, your profile name and photo will be public. Later edits need a new review. Private workout history and exercise notes are not included.","Название, описание, дни и упражнения должны быть на английском. Модератор проверит язык и копию плана. После одобрения упражнения, обложка, имя и фото из профиля будут видны всем. Дальнейшие правки потребуют новой проверки. Личная история тренировок и заметки к упражнениям не передаются."))
   .setNegativeButton(s("Cancel","Отмена"),null).setPositiveButton(s("Submit","Отправить"),(d,w)->FriendsPanel.run(a,store->{CommunityPlans.submit(a,copy,store);return true;},ok->{PlanAuthor.claim(a,p);a.store.save();Toast.makeText(a,s("Sent for review","Отправлено на проверку"),Toast.LENGTH_LONG).show();})).show();
 }
 static void body(MainActivity a,LinearLayout box,JSONObject p){
  a.programCover(box,p,PlanCategory.label(p),true);
  if(!p.optString("description").isEmpty()){a.space(box,12);box.addView(a.text(p.optString("description"),14,a.MUTED));}
  JSONArray days=p.optJSONArray("days");
  for(int i=0;i<days.length();i++){JSONObject day=days.optJSONObject(i);LinearLayout c=a.card(box);c.addView(a.title((i+1)+". "+day.optString("name"),18));JSONArray groups=day.optJSONArray("groups");for(int j=0;j<groups.length();j++){JSONObject g=groups.optJSONObject(j);a.space(c,8);c.addView(a.text(g.optString("exercise")+" · "+g.optInt("sets")+" × "+g.optInt("reps"),15,a.TEXT));double kg=Engine.weight(g,p.optJSONObject("maxima"),p.optDouble("step",2.5));String weight=g.has("factor")?Engine.number(g.optDouble("factor")*100)+"% · "+g.optString("lift"):kg<0?s("Choose weight in the gym","Вес выбирается в зале"):Engine.number(kg)+s(" kg"," кг");c.addView(a.text(weight,12,a.MUTED));}}
 }
 static void view(MainActivity a,JSONObject p){LinearLayout box=a.form();body(a,box,p);a.formDialog(s("Plan preview","Просмотр плана"),box,s("Add to My plans","В мои планы"),()->CommunityCatalog.add(a,p));}
 static void author(MainActivity a,JSONObject source){
  JSONObject author=source.optJSONObject("author");String uid=author==null?CommunityPlans.ADMIN:author.optString("uid");boolean trace=CommunityPlans.ADMIN.equals(uid);LinearLayout box=a.form();
  Map<String,Object> portrait=new HashMap<>();portrait.put("avatar",author==null?"":author.optString("avatar"));String name=trace?"Trace":author.optString("name",s("Author","Автор"));if(trace)a.largeIcon(box,"trace",a.GREEN);else box.addView(FriendsProfile.avatar(a,portrait,name,64),new LinearLayout.LayoutParams(a.dp(64),a.dp(64)));
  a.space(box,12);box.addView(a.title(name,24));a.label(box,s("Published plans","Опубликованные планы"));int count=0;
  for(JSONObject p:CommunityCatalog.plans(a,a.programsSorted(false))){JSONObject by=p.optJSONObject("author");if((trace&&CommunityPlans.builtin(p))||(!p.optString("catalogId").isEmpty()&&by!=null&&uid.equals(by.optString("uid")))){count++;a.link(box,Lang.content(p.optString("name")),()->{if(CommunityCatalog.remote(a,p))view(a,p);else a.open(p);});}}
  if(count==0)box.addView(a.text(s("No public plans yet","Пока нет публичных планов"),14,a.MUTED));
  a.panel(s("Author","Автор"),box,s("Close","Закрыть"),()->{},false);
 }
 static void queue(MainActivity a,boolean moderation){
  if(moderation&&!CommunityPlans.admin(a))return;
  LinearLayout box=a.form();TextView status=a.text(s("Loading…","Загрузка…"),14,a.MUTED);box.addView(status);AlertDialog dialog=a.panel(moderation?s("Review queue","На проверке"):s("My submissions","Мои заявки"),box,s("Close","Закрыть"),()->{},false);
  FriendsPanel.run(a,store->{ArrayList<DocumentSnapshot> result=new ArrayList<>();Query q=store.db.collection("catalogSubmissions").whereEqualTo(moderation?"status":"owner",moderation?"pending":store.uid).orderBy(FieldPath.documentId()).limit(50);while(true){List<DocumentSnapshot> page=store.waitFor(q.get(Source.SERVER)).getDocuments();result.addAll(page);if(page.size()<50)break;q=q.startAfter(page.get(page.size()-1));}return result;},rows->{
   if(!dialog.isShowing())return;box.removeAllViews();if(rows.isEmpty())box.addView(a.text(s("Nothing here yet","Здесь пока пусто"),16,a.MUTED));
   for(DocumentSnapshot row:rows){LinearLayout card=a.card(box);card.addView(a.title(row.getString("title"),19));card.addView(a.text(row.getString("authorName"),13,a.MUTED));String state=row.getString("status");a.badge(card,"approved".equals(state)?s("PUBLISHED","ОПУБЛИКОВАНО"):"rejected".equals(state)?s("DECLINED","ОТКЛОНЕНО"):s("IN REVIEW","НА ПРОВЕРКЕ"),a.GREEN);if("rejected".equals(state))card.addView(a.text(row.getString("reason"),14,a.MUTED));a.link(card,s("View submitted copy","Посмотреть отправленную копию"),()->review(a,row,moderation,()->{dialog.dismiss();queue(a,moderation);}));}
  });
 }
 static void review(MainActivity a,DocumentSnapshot row,boolean moderation,Runnable refresh){
  try{JSONObject p=CommunityPlans.decode(row.getData());JSONObject by=new JSONObject();Engine.put(by,"uid",row.getString("owner"));Engine.put(by,"name",row.getString("authorName"));Engine.put(by,"avatar",row.getString("authorAvatar"));Engine.put(p,"author",by);
   LinearLayout box=a.form();body(a,box,p);AlertDialog dialog=a.panel(s("Submitted plan","Отправленный план"),box,s("Close","Закрыть"),()->{},false);
   if(moderation&&"pending".equals(row.getString("status"))){
    a.button(box,s("Edit before approval","Исправить перед одобрением"),false,()->{dialog.dismiss();ModeratorEditor.submission(a,row,refresh);});
    box.addView(a.text(s("Before publishing, check that the title, description, days and exercises are in English.","Перед публикацией проверь, что название, описание, дни и упражнения написаны на английском."),14,a.MUTED));
    a.button(box,s("Approve and publish","Одобрить и опубликовать"),true,()->new AlertDialog.Builder(a).setTitle(s("Publish this copy?","Опубликовать эту копию?")).setMessage(row.getString("title")).setNegativeButton(s("Cancel","Отмена"),null).setPositiveButton(s("Publish","Опубликовать"),(d,w)->FriendsPanel.run(a,store->{CommunityPlans.decide(store,row,true,"");return true;},ok->{dialog.dismiss();CommunityCatalog.get(a).load(true);refresh.run();})).show());
    a.button(box,s("Decline","Отклонить"),false,()->{LinearLayout form=a.form();EditText reason=a.input(form,s("Reason for the author","Причина для автора"),"",false);a.formDialog(s("Decline submission","Отклонить заявку"),form,s("Decline","Отклонить"),()->{String value=reason.getText().toString().trim();if(value.isEmpty()||value.length()>400)throw new IllegalArgumentException(s("Write 1–400 characters.","Напиши от 1 до 400 символов."));FriendsPanel.run(a,store->{CommunityPlans.decide(store,row,false,value);return true;},ok->{dialog.dismiss();refresh.run();});});});
   }
  }catch(Exception e){a.error(e);}
 }
}
