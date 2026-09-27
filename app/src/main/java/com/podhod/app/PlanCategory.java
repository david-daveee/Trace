package com.podhod.app;
import org.json.JSONObject;
final class PlanCategory {
 static final String[] KEYS={"strength","streetlifting","powerlifting","bodybuilding","calisthenics","cardio","custom"};
 static final String[] LABELS={"Силовой план","Стритлифтинг","Пауэрлифтинг","Бодибилдинг","Калистеника","Кардио","Своя категория"};
 static boolean sheiko(JSONObject p){String name=p.optString("name");return p.optString("routine").startsWith("sheiko-")||name.startsWith("Шейко · ")||name.startsWith("Sheiko · ");}
 static int defaultIndex(JSONObject p){return Zlat.is(p)?1:sheiko(p)?2:0;}
 static int index(JSONObject p){String key=p.optString("category",KEYS[defaultIndex(p)]);for(int i=0;i<KEYS.length;i++)if(KEYS[i].equals(key))return i;return defaultIndex(p);}
 static String label(JSONObject p){int i=index(p);return i==6&&!p.optString("categoryName").trim().isEmpty()?customLabel(p.optString("categoryName")):Lang.t(LABELS[i]);}
 static String customLabel(String name){return name.trim().equalsIgnoreCase("Домашняя")?Lang.t("Домашняя"):name;}
 static void set(JSONObject p,int index,String custom){if(index<0||index>=KEYS.length)throw new IllegalArgumentException();String name=custom.trim();if(index==6&&(name.isEmpty()||name.length()>40))throw new IllegalArgumentException(Lang.t("Название категории — от 1 до 40 символов"));Engine.put(p,"category",KEYS[index]);if(index==6)Engine.put(p,"categoryName",name);else p.remove("categoryName");}
}
