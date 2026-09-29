package com.podhod.app;
import android.app.*;
import android.content.Intent;
import android.graphics.*;
import android.os.Bundle;
import android.view.View;
import java.io.FileOutputStream;

/** Visual preview only; does not reset onboarding or change the signed-in account. */
public class WelcomeSmoke extends Instrumentation {
 boolean settings,friends,liveFriends;
 public void onCreate(Bundle b){super.onCreate(b);settings="settings".equals(b.getString("preview"));friends="friends".equals(b.getString("preview"));liveFriends="friends-live".equals(b.getString("preview"));start();}
 public void onStart(){Bundle result=new Bundle();final Throwable[] failure={null};
  MainActivity a=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  FriendsPanel.State social=new FriendsPanel.State();
  if(liveFriends)try{FriendsStore backend=new FriendsStore();social.profile=backend.profileData();social.privacy=backend.privacyData();social.connections=backend.connections();social.items=backend.ownItems();}catch(Exception e){failure[0]=e;}
  runOnMainSync(()->{try{
   if(failure[0]!=null)throw new IllegalStateException(failure[0]);
   if(liveFriends){a.page="friends";a.render();a.body.removeAllViews();a.heading("Friends","Your circle. Your choice of what to share.");if(social.profile==null)throw new AssertionError("Expected created profile");FriendsPanel.draw(a,a.body,social);}else if(friends){a.page="friends";a.render();a.body.removeAllViews();a.heading("Friends","Your circle. Your choice of what to share.");FriendsPanel.create(a,a.body);}else if(settings){a.page="settings";a.render();}else WelcomeScreen.show(a);View root=a.root;
   root.measure(View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2200,View.MeasureSpec.EXACTLY));root.layout(0,0,1080,2200);
   Bitmap bitmap=Bitmap.createBitmap(1080,2200,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));
   try(FileOutputStream out=a.openFileOutput(liveFriends?"friends-live.png":friends?"friends-preview.png":settings?"settings-preview.png":"welcome-preview.png",0)){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}bitmap.recycle();
  }catch(Throwable e){failure[0]=e;}finally{a.render();}});
  result.putString("stream",failure[0]==null?"PASS: welcome preview rendered without changing account or onboarding state.\n":"FAIL: "+failure[0]);finish(failure[0]==null?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
 }
}
