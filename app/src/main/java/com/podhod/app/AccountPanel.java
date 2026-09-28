package com.podhod.app;

import android.app.AlertDialog;
import android.os.CancellationSignal;
import android.widget.LinearLayout;
import androidx.credentials.*;
import androidx.credentials.exceptions.*;
import com.google.android.libraries.identity.googleid.*;
import com.google.firebase.auth.*;
import java.text.DateFormat;
import java.util.Date;

final class AccountPanel {
    static String s(String en,String ru){return Lang.english()?en:ru;}
    static void show(MainActivity a){
        AccountSync sync=AccountSync.get(a);FirebaseUser user=sync.user();
        LinearLayout card=a.card(a.body);android.graphics.drawable.GradientDrawable background=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xFF203E3B,0xFF1A293D});background.setCornerRadius(a.dp(24));card.setBackground(background);a.largeIcon(card,"trace",a.GREEN);a.space(card,18);a.badge(card,user==null?s("YOUR DATA","ТВОИ ДАННЫЕ"):s("CLOUD SYNC","ОБЛАЧНАЯ СИНХРОНИЗАЦИЯ"),a.GREEN);a.space(card,12);
        card.addView(a.title(user==null?s("Your Trace, everywhere","Твой Trace на любом телефоне"):s("Your Trace, connected","Твой Trace с тобой"),22));
        a.space(card,8);
        if(user==null){
            card.addView(a.text(s("Keep using Trace without an account, or sign in to sync your plans, progress and walks.","Пользуйся Trace без аккаунта или войди, чтобы синхронизировать планы, прогресс и прогулки."),14,a.MUTED));
            if(AccountSync.configured(a))a.button(card,s("Continue with Google","Войти через Google"),true,()->signIn(a));
            else card.addView(a.text(s("Cloud sign-in is not configured in this build.","В этой сборке облачный вход не настроен."),13,a.MUTED));
            return;
        }
        card.addView(a.text(user.getEmail()==null?"":user.getEmail(),14,a.MUTED));a.space(card,10);
        String status=sync.prefs().getString("status","choose");
        String message=status.equals("syncing")?s("Syncing…","Синхронизация…"):status.equals("choose")?s("Choose which data to use","Выбери, какие данные использовать"):status.equals("active")?s("Will sync after the workout or walk","Синхронизация после тренировки или прогулки"):status.equals("error")?s("Not synced yet · check your connection and retry","Пока не синхронизировано · проверь подключение и повтори"):status.equals("pending")?s("Changes waiting to sync","Изменения ожидают синхронизации"):s("Synced","Синхронизировано");
        card.addView(a.text(message,15,status.equals("error")?a.BLUE:a.GREEN));
        long last=sync.prefs().getLong("lastSync",0);if(last>0)card.addView(a.text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,Lang.locale()).format(new Date(last)),12,a.MUTED));
        if(!sync.busy())a.button(card,sync.enabled()?s("Sync now","Синхронизировать"):s("Set up sync","Настроить синхронизацию"),true,()->sync.prepare(listener(a),false));
        a.link(card,s("Sign out","Выйти из аккаунта"),()->new AlertDialog.Builder(a).setTitle(s("Sign out?","Выйти из аккаунта?")).setMessage(s("Your data stays on this phone. Cloud sync will stop.","Данные останутся на телефоне. Облачная синхронизация остановится.")).setNegativeButton(s("Cancel","Отмена"),null).setPositiveButton(s("Sign out","Выйти"),(d,w)->{
            sync.signOut();CredentialManager.create(a).clearCredentialStateAsync(new ClearCredentialStateRequest(),null,a::runOnUiThread,new CredentialManagerCallback<Void,ClearCredentialException>(){public void onResult(Void ignored){}public void onError(ClearCredentialException ignored){}});a.render();
        }).show());
    }
    private static void signIn(MainActivity a){signIn(a,()->{});}
    static void signIn(MainActivity a,Runnable signedIn){
        int resource=a.getResources().getIdentifier("default_web_client_id","string",a.getPackageName());
        if(resource==0){a.error(new IllegalStateException(s("Missing Google sign-in configuration","Отсутствует конфигурация Google-входа")));return;}
        GetGoogleIdOption option=new GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false).setServerClientId(a.getString(resource)).setAutoSelectEnabled(false).build();
        GetCredentialRequest request=new GetCredentialRequest.Builder().addCredentialOption(option).build();
        CredentialManager.create(a).getCredentialAsync(a,request,new CancellationSignal(),a::runOnUiThread,new CredentialManagerCallback<GetCredentialResponse,GetCredentialException>(){
            public void onResult(GetCredentialResponse response){try{
                Credential credential=response.getCredential();
                if(!(credential instanceof CustomCredential)||!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType()))throw new IllegalArgumentException("Unsupported sign-in response");
                GoogleIdTokenCredential token=GoogleIdTokenCredential.createFrom(credential.getData());
                FirebaseAuth.getInstance().signInWithCredential(GoogleAuthProvider.getCredential(token.getIdToken(),null)).addOnCompleteListener(task->{
                    if(!task.isSuccessful()){if(!a.isDestroyed())a.error(new IllegalStateException(s("Google sign-in failed. Check your connection and try again.","Не удалось войти через Google. Проверь подключение и повтори.")));return;}
                    AccountSync sync=AccountSync.get(a);sync.resetConsent();signedIn.run();
                    if(!a.isDestroyed()){a.render();sync.prepare(listener(a),true);}
                });
            }catch(Exception e){if(!a.isDestroyed())a.error(e);}}
            public void onError(GetCredentialException e){if(!(e instanceof GetCredentialCancellationException)&&!a.isDestroyed())a.error(new IllegalStateException(s("Could not open Google sign-in. Check Google Play services and try again.","Не удалось открыть Google-вход. Проверь сервисы Google Play и повтори.")));}
        });
    }
    static AccountSync.Listener listener(MainActivity a){return new AccountSync.Listener(){
        public void done(){if(!a.isDestroyed()){Notices.update(a);a.recreate();}}
        public void error(Exception e){if(!a.isDestroyed()){a.render();a.error(new IllegalStateException(s("Sync did not finish. ","Синхронизация не завершена. ")+safeMessage(e)));}}
        public void choice(AccountSync.Pending p){
            if(a.isDestroyed())return;
            String phone=counts(p.local),cloud=p.remote.payload==null?s("Empty account","Аккаунт пуст"):counts(p.remote.payload);
            String when=p.remote.time==0?"":"\n"+DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,Lang.locale()).format(new Date(p.remote.time));
            String message=s("On this phone: ","На телефоне: ")+phone+"\n"+s("In this account: ","В аккаунте: ")+cloud+when+"\n\n"+s("Plans, current and paused workouts, workout history, steps and settings use the selected source. Completed walks are combined without duplicates. Covers and GPS routes are stored privately in your Firebase account. A phone backup is saved before replacement.","Планы, текущая и отложенные тренировки, история, шаги и настройки берутся из выбранного источника. Завершённые прогулки объединяются без дублей. Обложки и GPS-маршруты сохраняются в личном разделе Firebase. Перед заменой создаётся копия телефона.");
            AlertDialog.Builder dialog=new AlertDialog.Builder(a).setTitle(s("Your data, your choice","Твои данные — твой выбор")).setMessage(message).setNegativeButton(s("Not now","Не сейчас"),null);
            if(p.remote.payload==null)dialog.setPositiveButton(s("Upload phone data","Загрузить данные телефона"),(d,w)->AccountSync.get(a).accept(p,true,listener(a)));
            else {dialog.setPositiveButton(s("Load from account","Загрузить из аккаунта"),(d,w)->AccountSync.get(a).accept(p,false,listener(a)));dialog.setNeutralButton(s("Use phone data","Использовать данные телефона"),(d,w)->new AlertDialog.Builder(a).setTitle(s("Replace cloud plans?","Заменить планы в облаке?")).setMessage(s("Phone plans, current and paused workouts, history, steps and settings will replace the account's version. Walks will be combined. The previous cloud revision will be retained.","Планы, текущая и отложенные тренировки, история, шаги и настройки телефона заменят версию аккаунта. Прогулки объединятся. Предыдущая облачная версия сохранится.")).setNegativeButton(s("Cancel","Отмена"),null).setPositiveButton(s("Replace","Заменить"),(d2,w2)->AccountSync.get(a).accept(p,true,listener(a))).show());}
            dialog.show();
        }
    };}
    private static String counts(org.json.JSONObject p){org.json.JSONObject w=p.optJSONObject("workouts");return w.optJSONArray("programs").length()+s(" plans · "," планов · ")+w.optJSONArray("history").length()+s(" workouts · "," тренировок · ")+p.optJSONObject("walks").optJSONArray("history").length()+s(" walks"," прогулок");}
    private static String safeMessage(Exception e){if(e instanceof IllegalStateException&&e.getCause()==null)return e.getMessage();Throwable t=e;while(t.getCause()!=null)t=t.getCause();return t instanceof com.google.firebase.firestore.FirebaseFirestoreException?s("Check your connection and account access, then retry.","Проверь подключение и доступ к аккаунту, затем повтори."):t instanceof java.util.concurrent.TimeoutException?s("The connection timed out. Try again.","Истекло время ожидания. Повтори попытку."):s("Retry when no workout or walk is active.","Повтори, когда нет активной тренировки или прогулки.");}
}
