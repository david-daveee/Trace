package com.podhod.app;
import org.json.*;
import android.widget.*;
final class ExerciseNotes {
 static String get(JSONObject owner,String exercise){JSONObject notes=owner==null?null:owner.optJSONObject("exerciseNotes");return notes==null?"":notes.optString(exercise);}
 static void set(JSONObject owner,String exercise,String text){String value=text.trim();if(value.length()>2000)throw new IllegalArgumentException(Lang.t("Заметка — до 2000 символов"));JSONObject notes=owner.optJSONObject("exerciseNotes");if(notes==null){notes=new JSONObject();Engine.put(owner,"exerciseNotes",notes);}if(value.isEmpty())notes.remove(exercise);else Engine.put(notes,exercise,value);}
 static void edit(MainActivity a,JSONObject plan,JSONObject session,String exercise,Runnable after){
  LinearLayout box=a.form();box.addView(a.text(Lang.t("Заметка сохраняется для этого упражнения в плане."),14,a.MUTED));
  EditText input=a.input(box,Lang.t("Заметка к упражнению"),get(session!=null&&session.has("exerciseNotes")?session:plan,exercise),false);input.setSingleLine(false);input.setMinLines(3);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(2000)});
  a.formDialog(Lang.t("Заметка к упражнению"),box,Lang.t("Сохранить"),()->{String note=input.getText().toString();if(plan!=null)set(plan,exercise,note);if(session!=null){if(!session.has("exerciseNotes")&&plan!=null&&plan.optJSONObject("exerciseNotes")!=null)Engine.put(session,"exerciseNotes",Engine.copy(plan.optJSONObject("exerciseNotes")));set(session,exercise,note);}a.store.save();after.run();});
 }
}
