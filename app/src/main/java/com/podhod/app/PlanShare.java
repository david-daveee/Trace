package com.podhod.app;
import android.content.*;
import android.net.Uri;
import org.json.JSONObject;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
final class PlanShare {
    static JSONObject snapshot(JSONObject plan){JSONObject copy=Engine.copy(plan);for(String key:new String[]{"id","mine","nextDay","lastUsed","lastProgressedSession","lastProgression","history","active","savedSessions"})copy.remove(key);return copy;}
    static Intent intent(Context context,JSONObject plan)throws IOException{
        byte[] bytes=snapshot(plan).toString().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>5*1024*1024)throw new IOException(Lang.t("Файл больше 5 МБ"));
        File folder=new File(context.getCacheDir(),"shared-plans");if(!folder.isDirectory()&&!folder.mkdirs())throw new IOException(Lang.t("Не удалось сохранить тренировку"));
        File[] old=folder.listFiles();if(old!=null)for(File f:old)if(f.isFile()&&f.lastModified()<System.currentTimeMillis()-7L*86400000)f.delete();
        File file=new File(folder,"trace-plan-"+UUID.randomUUID()+".json");try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);}
        Uri uri=new Uri.Builder().scheme("content").authority(context.getPackageName()+".plans").appendPath(file.getName()).build();
        Intent send=new Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM,uri)
            .putExtra(Intent.EXTRA_SUBJECT,Lang.content(plan.optString("name")))
            .putExtra(Intent.EXTRA_TEXT,Lang.content(plan.optString("name"))+"\n"+Lang.t("Открой этот файл с помощью Trace и добавь план из превью. Или сохрани файл: Планы → Из файла."))
             .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newUri(context.getContentResolver(),"Trace plan",uri));return send;
    }
}
