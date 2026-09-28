package com.podhod.app;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;

/** Device-local onboarding; never restored from another phone or account. */
final class WelcomeScreen {
    static boolean pending(Context c) {
        if(c.getSharedPreferences("trace-onboarding",0).getBoolean("completed",false))return false;
        if(AccountSync.get(c).user()!=null){complete(c);return false;}
        return true;
    }
    static void complete(Context c) {
        if(!c.getSharedPreferences("trace-onboarding",0).edit().putBoolean("completed",true).commit())
            throw new IllegalStateException(AccountPanel.s("Could not save your choice. Please try again.","Не удалось сохранить выбор. Попробуй ещё раз."));
    }
    static void enter(MainActivity a) {
        try{complete(a);if(!a.isDestroyed()){a.render();a.receivePlan(a.getIntent());}}catch(Exception e){if(!a.isDestroyed())a.error(e);}
    }
    static void show(MainActivity a) {
        a.stepsView=null;a.quickActions=null;
        LinearLayout root=a.column();a.root=root;
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xFF193D38,a.BG,0xFF172339}));
        ScrollView scroll=new ScrollView(a);scroll.setFillViewport(true);scroll.setVerticalScrollBarEnabled(false);
        LinearLayout content=a.column();content.setGravity(Gravity.CENTER_VERTICAL);content.setPadding(a.dp(28),a.dp(40),a.dp(28),a.dp(28));
        scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,-1));
        ImageView logo=new ImageView(a);logo.setImageResource(R.drawable.ic_trace);logo.setContentDescription("Trace");
        logo.setPadding(a.dp(22),a.dp(22),a.dp(22),a.dp(22));logo.setBackground(a.shape(0xFF213F3C,30));
        content.addView(logo,new LinearLayout.LayoutParams(a.dp(112),a.dp(112)));
        a.space(content,24);content.addView(a.title("Trace",48));a.space(content,10);
        content.addView(a.title(AccountPanel.s("Your progress starts here.","Твой прогресс начинается здесь."),27));
        a.space(content,16);
        content.addView(a.text(AccountPanel.s("Train with a plan. Track your progress. Keep moving.","Тренируйся по плану. Следи за прогрессом. Продолжай двигаться."),17,a.MUTED));
        a.space(content,40);
        boolean configured=AccountSync.configured(a);
        TextView google=a.button(content,AccountPanel.s("Continue with Google","Войти через Google"),true,()->AccountPanel.signIn(a,()->enter(a)));
        google.setEnabled(configured);google.setAlpha(configured?1f:.45f);
        a.button(content,AccountPanel.s("Continue without an account","Продолжить без аккаунта"),false,()->enter(a));
        a.space(content,18);
        content.addView(a.text(AccountPanel.s("Sign in to automatically restore your account data. An empty account saves this phone’s data. You can also start without an account.","Войди — данные аккаунта восстановятся автоматически. В пустой аккаунт сохранятся данные телефона. Можно начать и без аккаунта."),13,a.MUTED));
        if(!configured){a.space(content,8);content.addView(a.text(AccountPanel.s("Google sign-in is unavailable in this build.","В этой сборке вход через Google недоступен."),13,a.MUTED));}
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        a.setContentView(root);root.requestApplyInsets();
    }
}
