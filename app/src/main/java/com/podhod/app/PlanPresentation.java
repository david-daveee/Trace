package com.podhod.app;
import org.json.*;
final class PlanPresentation {
 static boolean street(JSONObject p){return Zlat.is(p)||ZlatIntermediate.is(p)||ZlatAdvanced.is(p)||(p!=null&&"streetlifting".equals(p.optString("category")));}
 static void normalize(JSONObject data){EnglishPlans.migrate(data);}
 static String detail(JSONObject p){if(Zlat.is(p))return EnglishPlanText.text("НАЧАЛЬНЫЙ · 3 ДНЯ В НЕДЕЛЮ");if(ZlatIntermediate.is(p))return EnglishPlanText.text("СРЕДНИЙ · 3 ДНЯ В НЕДЕЛЮ");if(ZlatAdvanced.is(p))return EnglishPlanText.text("ПРОДВИНУТЫЙ · ЦИКЛ 3 НЕДЕЛИ");if("sheiko-cms-ms".equals(p.optString("routine")))return EnglishPlanText.text("КМС / МС · ПОДГОТОВКА");if("sheiko-12-week".equals(p.optString("routine")))return EnglishPlanText.text("12 НЕДЕЛЬ · К СОРЕВНОВАНИЯМ");return "";}
}
