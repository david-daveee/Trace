package com.podhod.app;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONObject;
import java.util.*;

final class CatalogFilters {
 static String key(JSONObject p){int index=PlanCategory.index(p);return index==6?"custom:"+p.optString("categoryName").trim():PlanCategory.KEYS[index];}
 static ArrayList<JSONObject> show(MainActivity a,ArrayList<JSONObject> plans){
  LinkedHashMap<String,String> labels=new LinkedHashMap<>();labels.put("all",Lang.t("Все"));labels.put("powerlifting",Lang.t("Пауэрлифтинг"));labels.put("streetlifting",Lang.t("Стритлифтинг"));
  for(JSONObject p:plans)labels.putIfAbsent(key(p),PlanCategory.label(p));
  if(!labels.containsKey(a.catalogCategory))a.catalogCategory="all";
  HorizontalScrollView scroll=new HorizontalScrollView(a);scroll.setHorizontalScrollBarEnabled(false);LinearLayout row=new LinearLayout(a);scroll.addView(row);a.body.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
  for(Map.Entry<String,String> entry:labels.entrySet()){String key=entry.getKey();boolean selected=key.equals(a.catalogCategory);TextView chip=a.text(entry.getValue(),14,selected?a.BG:a.TEXT);chip.setTag("catalog-filter:"+key);chip.setGravity(Gravity.CENTER);chip.setPadding(a.dp(16),0,a.dp(16),0);chip.setMinHeight(a.dp(48));chip.setBackground(a.shape(selected?a.GREEN:a.CARD,16));chip.setSelected(selected);chip.setContentDescription(entry.getValue());LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,a.dp(48));lp.rightMargin=a.dp(8);row.addView(chip,lp);a.clickable(chip,()->{a.catalogCategory=key;a.render();});if(selected)scroll.post(()->scroll.smoothScrollTo(Math.max(0,chip.getLeft()-a.dp(8)),0));}
  a.space(a.body,18);ArrayList<JSONObject> result=new ArrayList<>();for(JSONObject p:plans)if(a.catalogCategory.equals("all")||a.catalogCategory.equals(key(p)))result.add(p);
  LinearLayout summary=new LinearLayout(a);summary.setGravity(Gravity.CENTER_VERTICAL);a.body.addView(summary,new LinearLayout.LayoutParams(-1,-2));summary.addView(a.text(labels.get(a.catalogCategory)+" · "+result.size(),12,a.MUTED),new LinearLayout.LayoutParams(0,-2,1));summary.addView(a.text(AccountPanel.s("Most liked first","Сначала популярные"),12,a.MUTED));a.space(a.body,6);
  if(result.isEmpty()){LinearLayout empty=a.card(a.body);empty.addView(a.title(Lang.t("В этой категории пока нет планов"),20));empty.addView(a.text(Lang.t("Создай или импортируй план и выбери для него эту категорию."),14,a.MUTED));}
  return result;
 }
}
