package com.podhod.app;

import android.app.AlertDialog;
import android.widget.LinearLayout;
import java.text.DateFormat;
import java.util.Date;

/** Keeps recovery available without competing with the primary sync controls. */
final class DataSettings {
    static String s(String en,String ru){return AccountPanel.s(en,ru);}
    static void show(MainActivity a){
        boolean account=AccountSync.get(a).user()!=null;
        if(!account){
            a.label(a.body,s("ON THIS PHONE","НА ЭТОМ ТЕЛЕФОНЕ"));
            LinearLayout local=a.card(a.body);
            a.badge(local,s("LOCAL BACKUP","РЕЗЕРВНАЯ КОПИЯ"),a.GREEN);a.space(local,8);
            a.actionRow(local,"export",s("Automatic backup","Автоматическая копия"),s("Every two weeks · ","Раз в две недели · ")+AutoBackup.status(a),a::autoBackupSettings);
            a.divider(local);
            a.actionRow(local,"history",s("Recovery & transfer","Восстановление и перенос"),lastBackup(a),()->open(a));
        }else{
            LinearLayout recovery=a.card(a.body);
            a.actionRow(recovery,"history",s("Recovery & transfer","Восстановление и перенос"),s("Backup history, file export and restore","История копий, экспорт и восстановление"),()->open(a));
        }
    }
    static String lastBackup(MainActivity a){
        long last=a.getSharedPreferences("trace-settings",0).getLong("lastBackupAt",0);
        return last==0?s("No exported backup yet","Экспортированной копии пока нет"):s("Last export · ","Последний экспорт · ")+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,Lang.locale()).format(new Date(last));
    }
    static void open(MainActivity a){
        LinearLayout box=a.form();
        a.badge(box,s("A WAY BACK","МОЖНО ВЕРНУТЬСЯ НАЗАД"),a.BLUE);a.space(box,16);
        box.addView(a.title(s("Keep a copy. Stay in control.","Сохрани копию. Всё под контролем."),23));a.space(box,10);
        box.addView(a.text(s("Sync keeps devices up to date. Backups help you return to an earlier version.","Синхронизация обновляет данные на устройствах. Копии помогают вернуться к прежней версии."),14,a.MUTED));a.space(box,20);
        AlertDialog[] dialog={null};
        LinearLayout files=a.card(box);
        a.actionRow(files,"export",s("Export a backup","Экспортировать копию"),lastBackup(a),()->{dialog[0].dismiss();a.backup();});
        a.divider(files);
        a.actionRow(files,"history",s("Backup history","История резервных копий"),s("Choose a date and restore","Выбери дату и восстанови данные"),()->{dialog[0].dismiss();BackupHistory.show(a);});
        a.divider(files);
        a.actionRow(files,"file",s("Restore from a file","Восстановить из файла"),s("Import a Trace backup from your phone","Выбери резервную копию Trace на телефоне"),()->{dialog[0].dismiss();a.pick(42);});
        LinearLayout automatic=a.card(box);
        a.actionRow(automatic,"export",s("Automatic backup","Автоматическая копия"),AutoBackup.status(a),()->{dialog[0].dismiss();a.autoBackupSettings();});
        box.addView(a.text(s("Before account data replaces phone data, Trace saves a safety copy. Find it in Backup history.","Перед заменой данных телефона версией из аккаунта Trace сохраняет страховочную копию. Она доступна в истории копий."),13,a.MUTED));
        dialog[0]=a.panel(s("Recovery & transfer","Восстановление и перенос"),box,Lang.t("Закрыть"),()->{},false);
    }
}
