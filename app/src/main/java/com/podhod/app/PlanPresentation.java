package com.podhod.app;
import org.json.*;
final class PlanPresentation {
 static boolean street(JSONObject p){return Zlat.is(p)||ZlatIntermediate.is(p)||ZlatAdvanced.is(p)||(p!=null&&"streetlifting".equals(p.optString("category")));}
 static void normalize(JSONObject data){JSONArray ps=data.optJSONArray("programs");for(int i=0;i<ps.length();i++){JSONObject p=ps.optJSONObject(i);String n=p.optString("name"),replacement=null;if(n.equals("Матвей Злат · турник и брусья"))replacement="Злат · Начальный";else if(n.equals("Злат · intermediate"))replacement="Злат · Средний";else if(n.equals("Шейко · КМС / МС"))replacement="Шейко · Подготовка";else if(n.equals("Шейко · 12 недель к соревнованиям"))replacement="Шейко · Соревнования";if(replacement!=null)Engine.put(p,"name",replacement);}}
 static String detail(JSONObject p){if(Zlat.is(p))return Lang.t("НАЧАЛЬНЫЙ · 3 ДНЯ В НЕДЕЛЮ");if(ZlatIntermediate.is(p))return Lang.t("СРЕДНИЙ · 3 ДНЯ В НЕДЕЛЮ");if(ZlatAdvanced.is(p))return Lang.t("ПРОДВИНУТЫЙ · ЦИКЛ 3 НЕДЕЛИ");if("sheiko-cms-ms".equals(p.optString("routine")))return Lang.t("КМС / МС · ПОДГОТОВКА");if("sheiko-12-week".equals(p.optString("routine")))return Lang.t("12 НЕДЕЛЬ · К СОРЕВНОВАНИЯМ");return "";}
}
