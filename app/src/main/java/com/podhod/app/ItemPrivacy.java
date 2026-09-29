package com.podhod.app;
import java.util.*;
/** 0 hidden, 1 all accepted friends, 2 legacy direct audience. Missing data is private. */
final class ItemPrivacy {
 static int visibility(Map<String,Object> item,Map<String,Object> categories){
  if(item==null||!Boolean.TRUE.equals(item.get("visible")))return 0;
  String field="walk".equals(item.get("kind"))?"walksVisible":"plansVisible";
  if(!Boolean.TRUE.equals(item.get("individualPrivacy"))&&(categories==null||!Boolean.TRUE.equals(categories.get(field))))return 0;
  return "friends".equals(item.get("audience"))?1:2;
 }
}
