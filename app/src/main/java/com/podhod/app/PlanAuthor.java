package com.podhod.app;
import android.widget.*;
import android.view.*;
import org.json.*;
import java.util.*;

final class PlanAuthor {
 static JSONObject own(MainActivity a){
  JSONObject author=new JSONObject();String uid=FriendsPanel.uid(a);if(uid.isEmpty())return author;Map<String,Object> profile=FriendsProfile.cache(a).portrait;
  Engine.put(author,"uid",uid);Engine.put(author,"name",FriendsProfile.name(profile,AccountPanel.s("Author","Автор")));Engine.put(author,"avatar",String.valueOf(profile.getOrDefault("avatar","")));Engine.put(author,"official",CommunityPlans.ADMIN.equals(uid));return author;
 }
 static void claim(MainActivity a,JSONObject p){if(!p.has("author")&&!CommunityPlans.builtin(p)&&!FriendsPanel.uid(a).isEmpty())Engine.put(p,"author",own(a));}
 static void show(MainActivity a,LinearLayout host,JSONObject p){
  JSONObject author=p.optJSONObject("author");boolean official=(author==null&&CommunityPlans.builtin(p))||(author!=null&&CommunityPlans.ADMIN.equals(author.optString("uid")));
  LinearLayout row=new LinearLayout(a);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,a.dp(6),0,a.dp(2));String name=official?"Trace":author==null?AccountPanel.s("Personal plan","Личный план"):author.optString("name",AccountPanel.s("Author","Автор"));
  View avatar;if(official){ImageView mark=new ImageView(a);mark.setImageResource(R.drawable.ic_trace);mark.setPadding(a.dp(3),a.dp(3),a.dp(3),a.dp(3));avatar=mark;}else{Map<String,Object> portrait=new HashMap<>();portrait.put("avatar",author==null?"":author.optString("avatar"));avatar=FriendsProfile.avatar(a,portrait,name,24);}
  row.addView(avatar,new LinearLayout.LayoutParams(a.dp(24),a.dp(24)));TextView label=a.text(name,12,a.TEXT);label.setMaxLines(1);label.setEllipsize(android.text.TextUtils.TruncateAt.END);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.leftMargin=a.dp(6);row.addView(label,lp);host.addView(row);
  row.setContentDescription(AccountPanel.s("Plan author: ","Автор плана: ")+name);if(official||author!=null)a.clickable(row,()->CommunityUi.author(a,p));
 }
 static boolean canSubmit(MainActivity a,JSONObject p){JSONObject author=p.optJSONObject("author");return !CommunityPlans.builtin(p)&&(author==null||author.optString("uid").equals(FriendsPanel.uid(a)));}
}
