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

    static final class State {Map<String,Object> profile,privacy;List<DocumentSnapshot> connections,items;}
    static void show(MainActivity a){
        a.heading(s("Friends","Друзья"),s("Your circle. Your choice of what to share.","Твой круг. Ты решаешь, чем делиться."));
        if(AccountSync.get(a).user()==null){LinearLayout c=a.card(a.body);a.largeIcon(c,"friends",a.GREEN);a.space(c,18);c.addView(a.title(s("Train together","Тренируйтесь вместе"),25));a.space(c,10);c.addView(a.text(s("Add friends, exchange plans and share selected walks. Your library stays private until you choose an item to share.","Добавляй друзей, обменивайся планами и делись выбранными прогулками. Твоя библиотека скрыта, пока ты сам не выберешь, чем поделиться."),15,a.MUTED));a.button(c,s("Open account settings","Открыть настройки аккаунта"),true,()->{a.page="settings";a.render();});return;}
        LinearLayout host=a.column();a.body.addView(host);host.addView(a.text(s("Loading your circle…","Загружаем друзей…"),15,a.MUTED));
        run(a,store->{State state=new State();state.profile=store.profileData();if(state.profile!=null){state.connections=store.connections();state.privacy=store.privacyData();state.items=store.ownItems();}return state;},state->{if(!host.isAttachedToWindow())return;host.removeAllViews();if(state.profile==null){create(a,host);return;}draw(a,host,state);},()->{if(!host.isAttachedToWindow())return;host.removeAllViews();LinearLayout c=a.card(host);c.addView(a.title(s("Friends is unavailable","Друзья пока недоступны"),22));a.space(c,10);c.addView(a.text(s("We could not load your circle. Your private plans and workouts are still available.","Не удалось загрузить друзей. Твои личные планы и тренировки по-прежнему доступны."),15,a.MUTED));a.button(c,s("Try again","Повторить"),true,a::render);});
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
            run(a,store->store.createProfile(label),p->a.render(),()->{submit[0].setEnabled(true);submit[0].setText(s("Create my friend code","Создать мой код"));});
        });
    }
    static void draw(MainActivity a,LinearLayout host,State state){
        LinearLayout hero=a.card(host);a.largeIcon(hero,"friends",a.GREEN);a.space(hero,14);hero.addView(a.title(String.valueOf(state.profile.get("name")),26));a.space(hero,6);a.badge(hero,s("YOUR FRIEND CODE","ТВОЙ КОД ДРУГА"),a.GREEN);a.space(hero,12);
        String code=SocialPayload.displayCode(String.valueOf(state.profile.get("code")));hero.addView(a.title(code,26));
        a.button(hero,s("Share my code","Поделиться кодом"),true,()->a.startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,s("Add me in Trace: ","Добавь меня в Trace: ")+code+"\nhttps://github.com/david-daveee/Trace/releases/latest"),s("Share friend code","Поделиться кодом"))));
        a.button(hero,s("Add a friend","Добавить друга"),false,()->{LinearLayout box=a.form();EditText input=a.input(box,s("Friend code","Код друга"),"",false);a.formDialog(s("Send a friend request","Отправить запрос в друзья"),box,s("Send request","Отправить запрос"),()->run(a,store->{store.request(input.getText().toString());return true;},ok->{Toast.makeText(a,s("Request sent","Запрос отправлен"),Toast.LENGTH_SHORT).show();a.render();}));});
        a.link(hero,s("Refresh","Обновить"),a::render);
        a.label(host,s("YOUR CIRCLE","ТВОЙ КРУГ"));
        if(state.connections.isEmpty()){LinearLayout empty=a.card(host);empty.addView(a.text(s("Share your code with a friend. They will appear here after you accept their request.","Отправь другу свой код. Он появится здесь после подтверждения запроса."),15,a.MUTED));}
        String uid=AccountSync.get(a).user().getUid();
        for(DocumentSnapshot c:state.connections){String peer=uid.equals(c.getString("from"))?c.getString("to"):c.getString("from"),name=uid.equals(c.getString("from"))?c.getString("toName"):c.getString("fromName");LinearLayout row=a.card(host);row.addView(a.title(name,20));
            if("accepted".equals(c.getString("status"))){a.link(row,s("View plans & walks","Планы и прогулки"),()->profile(a,peer,name));a.link(row,s("Remove friend","Удалить из друзей"),()->new AlertDialog.Builder(a).setTitle(s("Remove friend?","Удалить друга?")).setMessage(s("Access to each other's shared items will stop. Copies already added to a library cannot be removed remotely.","Доступ к публикациям друг друга закроется. Уже добавленные в библиотеку копии нельзя удалить удалённо.")).setNegativeButton(Lang.t("Отмена"),null).setPositiveButton(s("Remove","Удалить"),(d,w)->run(a,store->{store.remove(c.getId());return true;},ok->a.render())).show());}
            else if(uid.equals(c.getString("to"))){row.addView(a.text(s("Wants to add you","Хочет добавить тебя"),14,a.MUTED));a.button(row,s("Accept","Принять"),true,()->run(a,store->{store.accept(c.getId());return true;},ok->a.render()));a.link(row,s("Decline","Отклонить"),()->run(a,store->{store.remove(c.getId());return true;},ok->a.render()));}
            else{row.addView(a.text(s("Request sent · waiting for acceptance","Запрос отправлен · ждём подтверждения"),14,a.MUTED));a.link(row,s("Cancel request","Отменить запрос"),()->run(a,store->{store.remove(c.getId());return true;},ok->a.render()));}
        }
        a.label(host,s("WHAT FRIENDS CAN SEE","ЧТО ВИДЯТ ДРУЗЬЯ"));LinearLayout privacy=a.card(host);
        privacy.addView(a.text(s("Only items you publish below. Turning a category off hides all its shared items, including direct shares.","Только выбранные ниже публикации. Выключение категории скроет все её публикации, в том числе отправленные отдельным друзьям."),14,a.MUTED));
        privacyToggle(a,privacy,s("Selected plans","Выбранные планы"),"plansVisible",Boolean.TRUE.equals(state.privacy.get("plansVisible")));
        privacyToggle(a,privacy,s("Selected walks","Выбранные прогулки"),"walksVisible",Boolean.TRUE.equals(state.privacy.get("walksVisible")));
        a.button(privacy,s("Share a plan","Поделиться планом"),true,()->choosePlan(a));a.button(privacy,s("Share a walk","Поделиться прогулкой"),false,()->chooseWalk(a));
        a.space(host,8);a.label(host,s("YOUR SHARED ITEMS","ТВОИ ПУБЛИКАЦИИ"));
        if(state.items.isEmpty())host.addView(a.text(s("Nothing shared yet. Your plans and walks are private.","Пока ничего не опубликовано. Твои планы и прогулки скрыты."),14,a.MUTED));
        for(DocumentSnapshot item:state.items){LinearLayout c=a.card(host);boolean visible=Boolean.TRUE.equals(item.getBoolean("visible"));c.addView(a.title(item.getString("name"),18));c.addView(a.text(visible?("direct".equals(item.getString("audience"))?s("Selected friends","Выбранные друзья"):s("All friends","Все друзья")):s("Hidden","Скрыто"),13,a.MUTED));if(visible)a.link(c,s("Hide this item","Скрыть эту публикацию"),()->run(a,store->{store.hide(item.getId());return true;},ok->a.render()));}
        a.space(host,12);host.addView(a.text(s("Shared items are snapshots. Share again to update one. Hiding stops new access; it cannot erase copies someone already saved.","Публикация — отдельная копия. Поделись ещё раз, чтобы обновить её. Скрытие закрывает новый доступ, но не удаляет уже сохранённые кем-то копии."),12,a.MUTED));
    }
    static void privacyToggle(MainActivity a,LinearLayout box,String label,String key,boolean checked){
        Switch toggle=new Switch(a);toggle.setText(label);toggle.setTextColor(a.TEXT);toggle.setTextSize(16);toggle.setMinHeight(a.dp(60));toggle.setChecked(checked);box.addView(toggle,new LinearLayout.LayoutParams(-1,-2));
        toggle.setOnCheckedChangeListener((v,on)->{toggle.setEnabled(false);run(a,store->{store.privacy(key,on);return true;},ok->a.render(),a::render);});
    }
    static void choosePlan(MainActivity a){LinearLayout box=a.form();JSONArray plans=a.store.data.optJSONArray("programs");AlertDialog[] dialog={null};int count=0;for(int i=0;i<plans.length();i++){JSONObject p=plans.optJSONObject(i);if(!p.optBoolean("mine"))continue;count++;a.actionRow(box,"library",Lang.content(p.optString("name")),s("Choose audience","Выбрать, кому показать"),()->{dialog[0].dismiss();sharePlan(a,p);});}if(count==0)box.addView(a.text(s("Add a plan to My plans first.","Сначала добавь план в «Мои планы»."),15,a.MUTED));dialog[0]=a.panel(s("Share a plan","Поделиться планом"),box,Lang.t("Закрыть"),()->{},false);}
    static void chooseWalk(MainActivity a){LinearLayout box=a.form();JSONArray walks=Walks.data(a).optJSONArray("history");AlertDialog[] dialog={null};for(int i=walks.length()-1;i>=0;i--){JSONObject w=walks.optJSONObject(i);a.actionRow(box,"steps",walkName(w),Walks.summary(w),()->{dialog[0].dismiss();shareWalk(a,w);});}if(walks.length()==0)box.addView(a.text(s("Finish a walk first.","Сначала заверши прогулку."),15,a.MUTED));dialog[0]=a.panel(s("Share a walk","Поделиться прогулкой"),box,Lang.t("Закрыть"),()->{},false);}
    static String walkName(JSONObject walk){return new java.text.SimpleDateFormat("d MMM yyyy · HH:mm",Lang.locale()).format(new Date(walk.optLong("started")));}
    static void sharePlan(MainActivity a,JSONObject plan){audience(a,"plan",plan.optString("id"),Lang.content(plan.optString("name")),SocialPayload.plan(plan));}
    static void shareWalk(MainActivity a,JSONObject walk){new AlertDialog.Builder(a).setTitle(s("Share this walk","Поделиться прогулкой")).setMessage(s("Share distance and duration only, or include the exact GPS route? The route can reveal home and other private places.","Показать только расстояние и время или добавить точный GPS-маршрут? Маршрут может раскрыть дом и другие личные места.")).setNegativeButton(Lang.t("Отмена"),null).setNeutralButton(s("With route","С маршрутом"),(d,w)->audience(a,"walk",walk.optString("id"),walkName(walk),SocialPayload.walk(walk,true))).setPositiveButton(s("Stats only","Только результат"),(d,w)->audience(a,"walk",walk.optString("id"),walkName(walk),SocialPayload.walk(walk,false))).show();}
    static void audience(MainActivity a,String kind,String sourceId,String name,JSONObject payload){
        run(a,store->{if(store.profileData()==null)return null;return store.connections();},connections->{if(connections==null){a.page="friends";a.render();Toast.makeText(a,s("Create your friend code first","Сначала создай свой код друга"),Toast.LENGTH_LONG).show();return;}
            String uid=AccountSync.get(a).user().getUid();List<DocumentSnapshot> accepted=new ArrayList<>();for(DocumentSnapshot c:connections)if("accepted".equals(c.getString("status")))accepted.add(c);
            LinearLayout box=a.form();box.addView(a.text(kind.equals("plan")?s("Shares exercises, weights and cover. Workout history and personal exercise notes stay private.","Показывает упражнения, веса и обложку. История занятий и личные заметки к упражнениям остаются скрыты."):s("Choose who can see this walk.","Выбери, кому показать прогулку."),14,a.MUTED));
            AlertDialog[] dialog={null};Consumer<List<String>> publish=recipients->{dialog[0].dismiss();run(a,store->{store.publish(kind,sourceId,name,payload,recipients);return store.privacyData();},privacy->{Toast.makeText(a,Boolean.TRUE.equals(privacy.get(kind.equals("plan")?"plansVisible":"walksVisible"))?s("Shared with friends","Опубликовано для друзей"):s("Saved, but this category is hidden in Friends privacy settings.","Сохранено, но категория скрыта в настройках видимости друзей."),Toast.LENGTH_LONG).show();if(a.page.equals("friends"))a.render();});};
            a.button(box,s("All friends","Все друзья"),true,()->publish.accept(null));
            for(DocumentSnapshot c:accepted){String peer=uid.equals(c.getString("from"))?c.getString("to"):c.getString("from"),label=uid.equals(c.getString("from"))?c.getString("toName"):c.getString("fromName");a.button(box,s("Only ","Только ")+label,false,()->publish.accept(Collections.singletonList(peer)));}
            if(accepted.isEmpty())box.addView(a.text(s("No friends yet. A publication becomes visible only after a request is accepted.","Друзей пока нет. Публикация станет видна только после подтверждения дружбы."),13,a.MUTED));
            dialog[0]=a.panel(s("Share with…","Поделиться с…"),box,Lang.t("Отмена"),()->{},false);
        });
    }
    static void profile(MainActivity a,String owner,String name){
        LinearLayout box=a.form();box.addView(a.title(name,26));a.space(box,8);box.addView(a.text(s("Only what your friend chose to share.","Только то, чем друг решил поделиться."),14,a.MUTED));
        LinearLayout content=a.column();box.addView(content);a.space(box,12);
        LinearLayout tabs=new LinearLayout(a);box.addView(tabs,0);String[] kinds={"plan","walk"};final int[] generation={0};
        Consumer<String> load=kind->{for(int i=0;i<tabs.getChildCount();i++){TextView tab=(TextView)tabs.getChildAt(i);boolean selected=kinds[i].equals(kind);tab.setBackground(a.shape(selected?a.GREEN:a.LINE,14));tab.setTextColor(selected?a.BG:a.TEXT);}int token=++generation[0];content.removeAllViews();content.addView(a.text(s("Loading…","Загрузка…"),14,a.MUTED));run(a,store->store.feed(owner,kind),items->{if(!content.isAttachedToWindow()||generation[0]!=token)return;content.removeAllViews();if(items.isEmpty())content.addView(a.text(s("Nothing shared here yet.","Здесь пока нет открытых публикаций."),15,a.MUTED));for(DocumentSnapshot item:items){LinearLayout card=a.card(content);card.addView(a.title(item.getString("name"),20));a.button(card,kind.equals("plan")?s("Preview & add to my plans","Посмотреть и добавить себе"):s("View walk","Посмотреть прогулку"),true,()->run(a,store->store.download(owner,item.getId()),data->{if(kind.equals("plan"))a.preview(data.optJSONObject("plan"));else viewWalk(a,data);}));}});};
        for(String kind:kinds){TextView tab=a.title(kind.equals("plan")?s("Plans","Планы"):s("Walks","Прогулки"),16);tab.setGravity(android.view.Gravity.CENTER);tab.setMinHeight(a.dp(52));tab.setBackground(a.shape(a.LINE,14));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(a.dp(3),0,a.dp(3),a.dp(16));tabs.addView(tab,lp);a.clickable(tab,()->load.accept(kind));}
        a.panel(name,box,Lang.t("Закрыть"),()->{},false);load.accept("plan");
    }
    static void viewWalk(MainActivity a,JSONObject data){JSONObject walk=data.optJSONObject("walk");LinearLayout box=a.form();box.addView(a.title(walkName(walk),22));a.space(box,12);box.addView(a.text(Walks.summary(walk),18,a.GREEN));a.space(box,14);if(data.optBoolean("routeShared")){WalkMap map=new WalkMap(a);map.route(walk);WalkPanel.mapFrame(a,box,map);WalkPanel.mapTools(a,box,map);}else box.addView(a.text(s("Your friend shared the result without a GPS route.","Друг поделился результатом без GPS-маршрута."),15,a.MUTED));a.panel(s("Friend's walk","Прогулка друга"),box,Lang.t("Закрыть"),()->{},false);}
}
