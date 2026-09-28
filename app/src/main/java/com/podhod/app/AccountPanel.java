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
            card.addView(a.text(s("Sign in to automatically load your account. If it is empty, this phone’s data will be saved. You can also keep using Trace without an account.","Войди — данные аккаунта загрузятся автоматически. Если он пуст, сохранятся данные телефона. Можно пользоваться Trace и без аккаунта."),14,a.MUTED));
            if(AccountSync.configured(a))a.button(card,s("Continue with Google","Войти через Google"),true,()->signIn(a));
            else card.addView(a.text(s("Cloud sign-in is not configured in this build.","В этой сборке облачный вход не настроен."),13,a.MUTED));
            return;
        }
        card.addView(a.text(user.getEmail()==null?"":user.getEmail(),14,a.MUTED));a.space(card,10);
        String status=sync.prefs().getString("status","pending");
        String message=status.equals("syncing")?s("Syncing…","Синхронизация…"):status.equals("restored")?s("Backup restored · automatic sync paused. Sync now will load account data.","Копия восстановлена · автосинхронизация приостановлена. При синхронизации загрузятся данные аккаунта."):status.equals("active")?s("Will sync after the workout or walk","Синхронизация после тренировки или прогулки"):status.equals("error")?s("Not synced yet · check your connection and retry","Пока не синхронизировано · проверь подключение и повтори"):status.equals("pending")?s("Changes waiting to sync","Изменения ожидают синхронизации"):s("Synced","Синхронизировано");
        card.addView(a.text(message,15,status.equals("error")?a.BLUE:a.GREEN));
        long last=sync.prefs().getLong("lastSync",0);if(last>0)card.addView(a.text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.SHORT,Lang.locale()).format(new Date(last)),12,a.MUTED));
        if(!sync.busy())a.button(card,sync.enabled()?s("Sync now","Синхронизировать"):s("Sync now","Синхронизировать"),true,()->sync.prepare(listener(a),false));
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
    };}

    private static String safeMessage(Exception e){if(e instanceof IllegalStateException&&e.getCause()==null)return e.getMessage();Throwable t=e;while(t.getCause()!=null)t=t.getCause();return t instanceof com.google.firebase.firestore.FirebaseFirestoreException?s("Check your connection and account access, then retry.","Проверь подключение и доступ к аккаунту, затем повтори."):t instanceof java.util.concurrent.TimeoutException?s("The connection timed out. Try again.","Истекло время ожидания. Повтори попытку."):s("Retry when no workout or walk is active.","Повтори, когда нет активной тренировки или прогулки.");}
}
