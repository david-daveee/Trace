package com.podhod.app;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import org.json.*;

final class FriendsPanel {
    static String s(String en,String ru){return AccountPanel.s(en,ru);}
    interface Work<T>{T run(FriendsStore store)throws Exception;}
    static final ExecutorService worker=Executors.newFixedThreadPool(2);
    static <T>void run(MainActivity a,Work<T> work,Consumer<T> done){run(a,work,done,()->{});}
    static <T>void run(MainActivity a,Work<T> work,Consumer<T> done,Runnable failed){
        if(AccountSync.get(a).user()==null){a.error(new IllegalStateException(s("Sign in through Settings to use Friends.","Войди в аккаунт через настройки, чтобы пользоваться друзьями.")));return;}
        FriendsStore store=new FriendsStore();
        worker.execute(()->{try{T result=work.run(store);a.runOnUiThread(()->{if(a.isDestroyed())return;try{store.check();done.accept(result);}catch(Exception e){a.error(e);}});}catch(Exception e){android.util.Log.e("TraceFriends","Operation failed",e);a.runOnUiThread(()->{if(a.isDestroyed())return;try{store.check();}catch(Exception changed){return;}failed.run();a.error(new IllegalStateException(errorMessage(e)));});}});
    }
    static String errorMessage(Throwable error){
        if(error instanceof GmailAddress.NotFound)return s("No available profile at this Gmail. Ask your friend to enable Find me by Gmail in My profile, or use their friend code.","Профиль по этому Gmail недоступен. Попроси друга включить поиск по Gmail в своём профиле или используй его код.");
        if(error instanceof IllegalArgumentException)return error.getMessage();
        for(Throwable cause=error;cause!=null;cause=cause.getCause()){
            if(cause instanceof com.google.firebase.firestore.FirebaseFirestoreException){
                switch(((com.google.firebase.firestore.FirebaseFirestoreException)cause).getCode()){
                    case PERMISSION_DENIED:return s("Friends access is unavailable. The cloud access rules may not be enabled yet, or this item is no longer shared. Your private training data is unchanged.","Доступ к друзьям пока недоступен. Возможно, облачные правила ещё не включены или публикация больше не открыта. Твои личные тренировки не изменены.");
                    case FAILED_PRECONDITION:return s("Friends is not ready on the server yet. A database index may still need to be configured. Please try again later.","Раздел друзей ещё не готов на сервере. Возможно, требуется настройка индекса базы данных. Попробуй позже.");
                    case UNAVAILABLE:return s("Cannot connect to Friends. Check your internet connection and try again.","Не удалось подключиться к друзьям. Проверь интернет и попробуй снова.");
                    default:break;
                }
            }
        }
        return s("Could not finish. Check your connection and refresh. The code may be invalid, or access may have changed.","Не удалось завершить. Проверь интернет и обнови экран. Возможно, код неверный или доступ изменился.");
    }

    static final class State {Map<String,Object> profile,privacy;List<DocumentSnapshot> connections,items;Map<String,Object> portrait=new HashMap<>();Map<String,Map<String,Object>> portraits=new HashMap<>();}
    static String uid(MainActivity a){return AccountSync.get(a).user()==null?"":AccountSync.get(a).user().getUid();}
    static boolean keepScreen(MainActivity a){return (a.page.equals("friends")||a.page.equals("profile"))&&a.root!=null&&a.friendsUid.equals(uid(a));}
    static void refresh(MainActivity a){if(!a.page.equals("friends")&&!a.page.equals("profile")){a.render();return;}if(a.friendsHost==null||!a.friendsHost.isAttachedToWindow()){a.render();return;}load(a,a.friendsHost);}
    static void load(MainActivity a,LinearLayout host){
        int generation=++a.friendsGeneration;
        run(a,store->{State state=new State();state.profile=store.profileData();if(state.profile!=null){state.connections=store.connections();state.privacy=store.privacyData();state.items=store.ownItems();state.portrait=store.portrait(store.uid);for(DocumentSnapshot friend:state.connections)if("accepted".equals(friend.getString("status"))){String peer=store.peer(friend);try{state.portraits.put(peer,store.portrait(peer));}catch(Exception ignored){/* A removed friend must not block the rest of the list. */}}}return state;},state->{if(!host.isAttachedToWindow()||generation!=a.friendsGeneration)return;State old=a.friendsState;a.friendsState=state;
            if(state.profile==null&&old!=null&&old.profile==null)return;
            android.widget.ScrollView scroll=(android.widget.ScrollView)a.body.getParent();int y=scroll.getScrollY();host.removeAllViews();if(state.profile==null)create(a,host);else if(a.page.equals("profile"))own(a,host,state);else draw(a,host,state);FriendsProfile.remember(a,state.portrait);scroll.post(()->scroll.scrollTo(0,y));
        },()->{if(!host.isAttachedToWindow()||generation!=a.friendsGeneration||a.friendsState!=null)return;host.removeAllViews();host.addView(a.text(s("Friends is unavailable. Try again when connected.","Друзья пока недоступны. Попробуй ещё раз при подключении."),15,a.MUTED));a.button(host,s("Try again","Повторить"),true,()->refresh(a));});
    }
    static void show(MainActivity a){
        a.heading(a.page.equals("profile")?s("My profile","Мой профиль"):s("Friends","Друзья"),a.page.equals("profile")?s("Your photo, your name, your training.","Твоё фото, имя и тренировки."):s("Your circle. Your choice of what to share.","Твой круг. Ты решаешь, чем делиться."));
        if(AccountSync.get(a).user()==null){a.friendsUid="";a.friendsState=null;LinearLayout c=a.card(a.body);a.largeIcon(c,"friends",a.GREEN);a.space(c,18);c.addView(a.title(s("Train together","Тренируйтесь вместе"),25));a.space(c,10);c.addView(a.text(s("Add friends, exchange plans and share selected walks. Your library stays private until you choose an item to share.","Добавляй друзей, обменивайся планами и делись выбранными прогулками. Твоя библиотека скрыта, пока ты сам не выберешь, чем поделиться."),15,a.MUTED));a.button(c,s("Open account settings","Открыть настройки аккаунта"),true,()->{a.page="settings";a.render();});return;}
        String account=uid(a);if(!account.equals(a.friendsUid)){a.friendsState=null;a.friendsUid=account;}
        LinearLayout host=a.column();a.friendsHost=host;a.body.addView(host);
        if(a.friendsState!=null){if(a.friendsState.profile==null)create(a,host);else if(a.page.equals("profile"))own(a,host,a.friendsState);else draw(a,host,a.friendsState);}else host.addView(a.text(s("Loading your circle…","Загружаем друзей…"),15,a.MUTED));
        load(a,host);
    }

    static void create(MainActivity a,LinearLayout host){
        LinearLayout c=a.card(host);a.largeIcon(c,"friends",a.GREEN);a.space(c,18);c.addView(a.title(s("Meet your training circle","Собери свой круг"),25));a.space(c,10);
        c.addView(a.text(s("Choose a name visible with your friend code. Plans and walks stay hidden until you share them. Your email is never shown to friends.","Выбери имя для друзей. Планы и прогулки останутся скрыты, пока ты ими не поделишься. Друзья не увидят твою почту."),15,a.MUTED));
        a.space(c,16);EditText name=a.input(c,s("Name for friends","Имя для друзей"),"",false);
        TextView[] submit={null};
        submit[0]=a.button(c,s("Create my friend code","Создать мой код"),true,()->{
            String label=name.getText().toString().trim();
            if(label.isEmpty()||label.length()>40){name.setError(s("Enter a name using 1–40 characters","Введи имя от 1 до 40 символов"));name.requestFocus();return;}
            submit[0].setEnabled(false);submit[0].setText(s("Creating…","Создаём…"));
            run(a,store->store.createProfile(label),p->refresh(a),()->{submit[0].setEnabled(true);submit[0].setText(s("Create my friend code","Создать мой код"));});
        });
    }
    static void own(MainActivity a,LinearLayout host,State state){
        String uid=uid(a),ownName=FriendsProfile.name(state.portrait,String.valueOf(state.profile.get("name")));
        LinearLayout hero=a.card(host);android.graphics.drawable.GradientDrawable heroBackground=new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR,new int[]{0xFF21483F,a.CARD});heroBackground.setCornerRadius(a.dp(24));hero.setBackground(heroBackground);
        LinearLayout intro=new LinearLayout(a);intro.setGravity(android.view.Gravity.CENTER_VERTICAL);hero.addView(intro);
        android.view.View portrait=FriendsProfile.avatar(a,state.portrait,ownName,76);intro.addView(portrait,new LinearLayout.LayoutParams(a.dp(76),a.dp(76)));a.clickable(portrait,()->FriendsProfile.pickPhoto(a));
        LinearLayout title=a.column();title.setPadding(a.dp(16),0,0,0);intro.addView(title,new LinearLayout.LayoutParams(0,-2,1));a.badge(title,s("YOUR PROFILE","ТВОЙ ПРОФИЛЬ"),a.GREEN);a.space(title,8);title.addView(a.title(ownName,27));
        String bio=String.valueOf(state.portrait.getOrDefault("bio",""));a.space(hero,14);hero.addView(a.text(bio.isEmpty()?s("Your training. Your people.","Твои тренировки. Твой круг."):bio,15,a.MUTED));
        long count=state.connections.stream().filter(c->"accepted".equals(c.getString("status"))).count();a.space(hero,14);hero.addView(a.text(s("Friends: ","Друзья: ")+count+s(" · Shared: "," · Публикации: ")+state.items.stream().filter(c->Boolean.TRUE.equals(c.getBoolean("visible"))).count(),13,a.GREEN));
        a.link(hero,s("Edit profile","Изменить профиль"),()->FriendsProfile.edit(a,state));
        a.link(hero,s("My friend code","Мой код друга"),()->code(a,state));
        GmailFriends.settings(a,host);
        a.actionRow(host,"friends",s("My friends","Мои друзья"),s("Explore their plans and walks","Смотреть планы и прогулки друзей"),()->{a.page="friends";a.render();});
        a.space(host,16);host.addView(a.text(s("Your photo and bio are visible only to accepted friends. Manage each plan and walk with its privacy eye.","Фото и описание видны только подтверждённым друзьям. Видимость планов и прогулок меняется глазком у каждой записи."),14,a.MUTED));
    }
    static void draw(MainActivity a,LinearLayout host,State state){
        String uid=uid(a);long count=state.connections.stream().filter(c->"accepted".equals(c.getString("status"))).count();
        LinearLayout tools=new LinearLayout(a);host.addView(tools);compact(a,tools,s("Add friend","Добавить друга"),true,()->addFriend(a));compact(a,tools,s("My code","Мой код"),false,()->code(a,state));a.space(host,22);
        a.label(host,s("FRIENDS","ДРУЗЬЯ"));
        if(count==0){LinearLayout empty=a.card(host);empty.addView(a.title(s("A little company goes a long way","Вместе интереснее"),20));a.space(empty,8);empty.addView(a.text(s("Add your first friend to explore their plans and walks.","Добавь первого друга, чтобы смотреть его планы и прогулки."),14,a.MUTED));}
        for(DocumentSnapshot c:state.connections)if("accepted".equals(c.getString("status"))){String peer=uid.equals(c.getString("from"))?c.getString("to"):c.getString("from"),fallback=uid.equals(c.getString("from"))?c.getString("toName"):c.getString("fromName");Map<String,Object> details=state.portraits.getOrDefault(peer,new HashMap<>());String name=FriendsProfile.name(details,fallback);
            LinearLayout row=a.card(host);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(android.view.Gravity.CENTER_VERTICAL);row.setPadding(a.dp(14),a.dp(14),a.dp(14),a.dp(14));row.addView(FriendsProfile.avatar(a,details,name,52),new LinearLayout.LayoutParams(a.dp(52),a.dp(52)));
            LinearLayout label=a.column();label.setPadding(a.dp(14),0,a.dp(6),0);row.addView(label,new LinearLayout.LayoutParams(0,-2,1));label.addView(a.title(name,19));a.space(label,4);label.addView(a.text(String.valueOf(details.getOrDefault("bio",s("Plans & walks","Планы и прогулки"))),13,a.MUTED));android.widget.ImageView chat=new android.widget.ImageView(a);chat.setImageDrawable(new LineIcon("chat",a.GREEN));chat.setPadding(a.dp(12),a.dp(12),a.dp(12),a.dp(12));chat.setContentDescription(s("Message ","Написать ")+name);row.addView(chat,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));a.clickable(chat,()->FriendChat.open(a,c.getId(),name));row.addView(a.text("›",26,a.MUTED));a.clickable(row,()->profile(a,peer,name,details,c.getId()));
        }
        boolean pending=state.connections.stream().anyMatch(c->!"accepted".equals(c.getString("status")));if(pending){a.space(host,12);a.label(host,s("REQUESTS","ЗАПРОСЫ"));}
        for(DocumentSnapshot c:state.connections)if(!"accepted".equals(c.getString("status"))){String name=uid.equals(c.getString("from"))?c.getString("toName"):c.getString("fromName");LinearLayout row=a.card(host);row.addView(a.title(name,18));if(uid.equals(c.getString("to"))){row.addView(a.text(s("Wants to join your circle","Хочет добавить тебя в друзья"),14,a.MUTED));LinearLayout actions=new LinearLayout(a);row.addView(actions);compact(a,actions,s("Accept","Принять"),true,()->run(a,b->{b.accept(c.getId());return true;},ok->refresh(a)));compact(a,actions,s("Decline","Отклонить"),false,()->run(a,b->{b.remove(c.getId());return true;},ok->refresh(a)));}else{row.addView(a.text(s("Request sent","Запрос отправлен"),14,a.MUTED));a.link(row,s("Cancel request","Отменить запрос"),()->run(a,b->{b.remove(c.getId());return true;},ok->refresh(a)));}}
        a.space(host,16);a.actionRow(host,"settings",s("Privacy","Приватность"),s("Choose what your friends see","Выбери, что увидят друзья"),()->{LinearLayout box=a.form();privacy(a,box,state);a.panel(s("Privacy","Приватность"),box,Lang.t("Закрыть"),()->{},false);});
        a.link(host,s("Refresh friends","Обновить друзей"),()->refresh(a));
    }
    static void compact(MainActivity a,LinearLayout row,String title,boolean primary,Runnable action){TextView button=a.button(row,title,primary,action);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,a.dp(50),1);lp.setMargins(0,a.dp(8),a.dp(6),0);button.setLayoutParams(lp);button.setTextSize(14);button.setPadding(a.dp(8),0,a.dp(8),0);}
    static void addFriend(MainActivity a){GmailFriends.add(a);}
    static void code(MainActivity a,State state){LinearLayout box=a.form();String code=SocialPayload.displayCode(String.valueOf(state.profile.get("code")));box.addView(a.title(code,26));a.space(box,12);box.addView(a.text(s("Send this code to a friend. You choose which requests to accept.","Отправь код другу. Ты выбираешь, какие запросы принять."),15,a.MUTED));a.button(box,s("Share code","Поделиться кодом"),true,()->a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,"Trace: "+code+"\nhttps://github.com/david-daveee/Trace/releases/latest"),s("Share code","Поделиться кодом"))));a.panel(s("My friend code","Мой код друга"),box,Lang.t("Закрыть"),()->{},false);}
    static void privacy(MainActivity a,LinearLayout host,State state){
        host.addView(a.title(s("You control each item","Ты выбираешь для каждой записи"),23));a.space(host,16);
        host.addView(a.text(s("Crossed-out eye: hidden. Open eye: visible to all accepted friends. Use the eye on each plan or completed walk — no separate sharing step.","Перечёркнутый глаз — скрыто. Открытый глаз — видно всем подтверждённым друзьям. Нажимай на глаз у плана или завершённой прогулки — отдельно делиться не нужно."),16,a.TEXT));a.space(host,16);
        host.addView(a.text(s("New walks start hidden. Opening a walk also makes its GPS route visible. Hiding cannot erase a copy a friend already saved.","Новые прогулки скрыты по умолчанию. Открывая прогулку, ты показываешь и её GPS-маршрут. Скрытие не удаляет копии, которые друг уже сохранил."),14,a.MUTED));
    }
    static String walkName(JSONObject walk){return new java.text.SimpleDateFormat("d MMM yyyy · HH:mm",Lang.locale()).format(new Date(walk.optLong("started")));}
    static void profile(MainActivity a,String owner,String name,Map<String,Object> details,String connection){
        FriendPage.open(a,owner,name,details,connection);
    }
    static void viewWalk(MainActivity a,JSONObject data){JSONObject walk=data.optJSONObject("walk");LinearLayout box=a.form();box.addView(a.title(walkName(walk),22));a.space(box,12);box.addView(a.text(Walks.summary(walk),18,a.GREEN));a.space(box,14);if(data.optBoolean("routeShared")){WalkMap map=new WalkMap(a);map.route(walk);WalkPanel.mapFrame(a,box,map);WalkPanel.mapTools(a,box,map);}else box.addView(a.text(s("Your friend shared the result without a GPS route.","Друг поделился результатом без GPS-маршрута."),15,a.MUTED));a.panel(s("Friend's walk","Прогулка друга"),box,Lang.t("Закрыть"),()->{},false);}
}
