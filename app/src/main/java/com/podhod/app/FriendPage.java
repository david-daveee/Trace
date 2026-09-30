package com.podhod.app;

import android.app.*;
import android.view.*;
import android.widget.*;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.*;
import org.json.*;

/** A browsable friend profile; only the server-authorized social feed is read. */
final class FriendPage {
 final MainActivity a; final String owner,connection,account; final Dialog dialog;
 final LinearLayout root,content,tabs; final ScrollView scroll; final TextView counts;
 final List<DocumentSnapshot> items=new ArrayList<>(); final Map<String,JSONObject> payloads=new HashMap<>();
 String filter="all"; int limit=6,generation; boolean busy;
 static String s(String en,String ru){return FriendsPanel.s(en,ru);}
 static FriendPage open(MainActivity a,String owner,String name,Map<String,Object> details,String connection){FriendPage page=new FriendPage(a,owner,name,details,connection);page.load();return page;}
 FriendPage(MainActivity a,String owner,String name,Map<String,Object> details,String connection){
  this.a=a;this.owner=owner;this.connection=connection;account=FriendsPanel.uid(a);
  dialog=new Dialog(a,android.R.style.Theme_Material_NoActionBar);root=a.column();root.setBackgroundColor(a.BG);dialog.setContentView(root);
  LinearLayout bar=new LinearLayout(a);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(a.dp(12),a.dp(4),a.dp(12),a.dp(4));root.addView(bar,new LinearLayout.LayoutParams(-1,a.dp(64)));
  toolbarIcon(bar,"back",s("Back to friends","Назад к друзьям"),dialog::dismiss);
  TextView pageTitle=a.title(s("Profile","Профиль"),17);pageTitle.setGravity(Gravity.CENTER);bar.addView(pageTitle,new LinearLayout.LayoutParams(0,-2,1));
  toolbarIcon(bar,"refresh",s("Refresh profile","Обновить профиль"),()->{if(!busy)load();});
  scroll=new ScrollView(a);scroll.setFillViewport(true);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));LinearLayout body=a.column();body.setPadding(a.dp(20),a.dp(8),a.dp(20),a.dp(24));scroll.addView(body);
  LinearLayout hero=a.card(body);android.graphics.drawable.GradientDrawable bg=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xFF28564A,a.CARD});bg.setCornerRadius(a.dp(24));hero.setBackground(bg);
  hero.addView(FriendsProfile.avatar(a,details,name,96),new LinearLayout.LayoutParams(a.dp(96),a.dp(96)));a.space(hero,20);a.badge(hero,s("TRAINING PROFILE","ТРЕНИРОВОЧНЫЙ ПРОФИЛЬ"),a.GREEN);a.space(hero,10);hero.addView(a.title(name,32));String bio=String.valueOf(details.getOrDefault("bio",""));if(!bio.isEmpty()){a.space(hero,10);hero.addView(a.text(bio,16,a.MUTED));}a.space(hero,18);a.button(hero,s("Message","Написать"),true,()->FriendChat.open(a,connection,name));counts=a.text(s("Loading publications…","Загружаем публикации…"),14,a.GREEN);hero.addView(counts);
  tabs=new LinearLayout(a);body.addView(tabs);String[] kinds={"all","plan","walk"};String[] labels={s("All","Все"),s("Plans","Планы"),s("Walks","Прогулки")};for(int i=0;i<3;i++){final String kind=kinds[i];FriendsPanel.compact(a,tabs,labels[i],i==0,()->{if(filter.equals(kind))return;filter=kind;limit=6;draw();});}
  a.space(body,18);content=a.column();body.addView(content);a.space(body,16);a.link(body,s("Remove friend","Удалить друга"),this::remove);
  dialog.setOnDismissListener(d->generation++);
  Window window=dialog.getWindow();
  if(android.os.Build.VERSION.SDK_INT>=30)window.setDecorFitsSystemWindows(false);
  else window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
  root.setOnApplyWindowInsetsListener((v,insets)->{
   if(android.os.Build.VERSION.SDK_INT>=30){android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);}
   else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
   return insets;
  });
  dialog.show();window.setLayout(-1,-1);window.setStatusBarColor(a.BG);window.setNavigationBarColor(a.BG);root.requestApplyInsets();
 }
 void toolbarIcon(LinearLayout bar,String icon,String label,Runnable action){ImageView button=new ImageView(a);button.setImageDrawable(new LineIcon(icon,a.TEXT));button.setPadding(a.dp(13),a.dp(13),a.dp(13),a.dp(13));button.setContentDescription(label);button.setTooltipText(label);android.util.TypedValue feedback=new android.util.TypedValue();a.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless,feedback,true);button.setBackgroundResource(feedback.resourceId);bar.addView(button,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));a.clickable(button,action);}
 boolean active(){return dialog.isShowing()&&account.equals(FriendsPanel.uid(a))&&!a.isDestroyed();}
 void load(){busy=true;int token=++generation;if(items.isEmpty()){content.removeAllViews();content.addView(a.text(s("Loading…","Загрузка…"),15,a.MUTED));}counts.setText(s("Updating…","Обновляем…"));FriendsPanel.run(a,b->{List<DocumentSnapshot> result=new ArrayList<>(b.feed(owner,"plan"));result.addAll(b.feed(owner,"walk"));result.sort((x,y)->Long.compare(y.getLong("updatedAt")==null?0:y.getLong("updatedAt"),x.getLong("updatedAt")==null?0:x.getLong("updatedAt")));return result;},result->{if(!active()||token!=generation)return;busy=false;items.clear();items.addAll(result);payloads.clear();draw();},()->{if(!active()||token!=generation)return;busy=false;counts.setText(s("Could not refresh","Не удалось обновить"));if(items.isEmpty()){content.removeAllViews();a.button(content,s("Try again","Повторить"),true,this::load);}});}
 void draw(){if(!active())return;long plans=items.stream().filter(i->"plan".equals(i.getString("kind"))).count();counts.setText(s("Plans: ","Планы: ")+plans+s("  ·  Walks: ","  ·  Прогулки: ")+(items.size()-plans));String[] kinds={"all","plan","walk"};for(int i=0;i<3;i++){TextView tab=(TextView)tabs.getChildAt(i);boolean selected=filter.equals(kinds[i]);tab.setBackground(a.shape(selected?a.GREEN:a.LINE,14));tab.setTextColor(selected?a.BG:a.TEXT);}content.removeAllViews();int shown=0,total=0;for(DocumentSnapshot item:items){if(!filter.equals("all")&&!filter.equals(item.getString("kind")))continue;total++;if(shown++>=limit)continue;LinearLayout card=a.card(content);renderCard(card,item,payloads.get(item.getId()));if(!payloads.containsKey(item.getId())){int token=generation;FriendsPanel.run(a,b->b.download(owner,item.getId()),data->{if(!active()||token!=generation)return;payloads.put(item.getId(),data);if(card.isAttachedToWindow())renderCard(card,item,data);},()->{if(card.isAttachedToWindow()){card.removeAllViews();card.addView(a.text(s("This item is unavailable","Эта запись недоступна"),14,a.MUTED));}});}}
  if(total==0){LinearLayout empty=a.card(content);a.largeIcon(empty,"eye-off",a.MUTED);a.space(empty,16);empty.addView(a.title(s("Nothing here yet","Пока ничего нет"),22));a.space(empty,8);empty.addView(a.text(s("Plans and walks appear here when your friend opens their privacy eye.","Здесь появятся планы и прогулки, которые друг откроет глазком приватности."),15,a.MUTED));}if(total>limit)a.button(content,s("Show more","Показать ещё"),false,()->{limit+=6;draw();});
 }
 void renderCard(LinearLayout card,DocumentSnapshot item,JSONObject data){card.removeAllViews();boolean plan="plan".equals(item.getString("kind"));if(plan&&data!=null){JSONObject p=data.optJSONObject("plan");a.programCover(card,p,PlanCategory.label(p),true);if(!p.optString("routine").isEmpty()||!p.optString("popularityId").isEmpty())PlanLikes.get(a).button(card,p);}else{a.badge(card,plan?s("PLAN","ПЛАН"):s("WALK","ПРОГУЛКА"),plan?a.BLUE:a.GREEN);a.space(card,12);card.addView(a.title(item.getString("name"),22));}if(!plan&&data!=null){a.space(card,14);card.addView(a.text(Walks.summary(data.optJSONObject("walk")),20,a.GREEN));a.space(card,8);card.addView(a.text(data.optBoolean("routeShared")?s("Route available · tap to explore","Есть маршрут · нажми для просмотра"):s("Walk result","Результат прогулки"),13,a.MUTED));}a.space(card,10);a.link(card,plan?s("Explore plan →","Посмотреть план →"):s("Explore walk →","Посмотреть прогулку →"),()->FriendsPanel.run(a,b->b.download(owner,item.getId()),fresh->{if(!active())return;if(plan)a.preview(fresh.optJSONObject("plan"));else FriendsPanel.viewWalk(a,fresh);}));}
 void remove(){new AlertDialog.Builder(a).setTitle(s("Remove friend?","Удалить друга?")).setMessage(s("Access to shared items will stop. Saved copies remain.","Доступ к публикациям закроется. Сохранённые копии останутся.")).setNegativeButton(Lang.t("Отмена"),null).setPositiveButton(s("Remove","Удалить"),(d,w)->FriendsPanel.run(a,b->{b.remove(connection);return true;},ok->{dialog.dismiss();FriendsPanel.refresh(a);})).show();}
}
