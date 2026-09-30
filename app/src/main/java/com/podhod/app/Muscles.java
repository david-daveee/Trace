package com.podhod.app;
import org.json.*;
import java.util.*;

/** Exercise-based involvement, not a measure of fatigue or recovery. */
final class Muscles {
 static final String[] KEYS={"chest","back","shoulders","biceps","triceps","abs","glutes","quads","hamstrings","calves","lowerback","forearms"};
 static final String[] LABELS={"Грудь","Спина","Плечи","Бицепс","Трицепс","Пресс","Ягодицы","Передняя поверхность бедра","Задняя поверхность бедра","Икры","Поясница","Предплечья"};
 static Set<String> of(JSONObject set){Set<String> result=new LinkedHashSet<>();JSONArray custom=set.optJSONArray("muscles");if(custom!=null){for(int i=0;i<custom.length();i++)if(Arrays.asList(KEYS).contains(custom.optString(i)))result.add(custom.optString(i));return result;}String n=EnglishPlanText.original(set.optString("exercise")).toLowerCase(Locale.ROOT).replace('ё','е');
  if(n.contains("развод")||n.contains("разведение рук")||n.contains("fly"))add(result,"chest","shoulders");
  else if(n.contains("брусь")||n.contains("dip"))add(result,"chest","triceps","shoulders");
  else if(n.contains("подтяг")||n.contains("pull-up")||n.contains("pull up")||n.contains("широчайш")||n.contains("pulldown"))add(result,"back","biceps","forearms");
  else if(n.contains("трицеп")||n.contains("разгибания рук")||n.contains("tricep"))add(result,"triceps");
  else if(n.contains("бицеп")||n.contains("сгибания рук")||n.contains("bicep"))add(result,"biceps","forearms");
  else if(n.contains("жим ног")||n.contains("leg press"))add(result,"quads","glutes");
  else if(n.contains("жим из-за")||n.contains("жим сидя")||n.contains("жим гантелей сидя")||n.contains("overhead press")||n.contains("shoulder press"))add(result,"shoulders","triceps");
  else if(n.contains("жим леж")||n.contains("наклонный жим")||n.contains("bench")||n.contains("отжимания от пола")||n.contains("push-up"))add(result,"chest","triceps","shoulders");
  else if(n.contains("присед")||n.contains("squat")||n.contains("выпад")||n.contains("lunge"))add(result,"quads","glutes","hamstrings");
  else if(n.contains("станов")||n.contains("тяга до колен")||n.contains("тяга из ямы")||n.contains("тяга с плинтов")||n.contains("тяга на подставке")||n.contains("deadlift"))add(result,"glutes","hamstrings","lowerback","back","forearms");
  else if((n.contains("наклон")&&!n.contains("тяга"))||n.contains("гиперэкст")||n.contains("hyperextension")||n.contains("good morning"))add(result,"lowerback","hamstrings","glutes");
  else if(n.contains("разгибание бедра"))add(result,"glutes","hamstrings");
  else if(n.contains("сгибание ног")||n.contains("leg curl"))add(result,"hamstrings");
  else if(n.contains("разгибание ног")||n.contains("leg extension"))add(result,"quads");
  else if(n.contains("икр")||n.contains("носки")||n.contains("calf"))add(result,"calves");
  else if(n.contains("пресс")||n.contains("планк")||n.contains("crunch")||n.contains("plank"))add(result,"abs");
  else if(n.contains("тяга в наклоне")||n.contains("row"))add(result,"back","biceps","forearms");
  return result;
 }
 static void add(Set<String> s,String... keys){Collections.addAll(s,keys);}
 static Map<String,Integer> count(JSONArray sets){Map<String,Integer> result=new LinkedHashMap<>();for(String k:KEYS)result.put(k,0);if(sets!=null)for(int i=0;i<sets.length();i++){JSONObject s=sets.optJSONObject(i);if(s!=null&&s.optBoolean("done"))for(String k:of(s))result.put(k,result.get(k)+1);}return result;}
 static int unknown(JSONArray sets){Set<String> names=new HashSet<>();if(sets!=null)for(int i=0;i<sets.length();i++){JSONObject s=sets.optJSONObject(i);if(s.optBoolean("done")&&of(s).isEmpty()&&!s.has("muscles"))names.add(s.optString("exercise"));}return names.size();}
 static JSONArray week(JSONArray history,long now){JSONArray result=new JSONArray();if(history!=null)for(int i=0;i<history.length();i++){JSONObject s=history.optJSONObject(i);long ended=s.optLong("ended");if(ended>now||ended<now-7L*86400000)continue;JSONArray sets=s.optJSONArray("sets");if(sets!=null)for(int j=0;j<sets.length();j++)result.put(sets.optJSONObject(j));}return result;}
}
