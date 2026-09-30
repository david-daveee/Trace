package com.podhod.app;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import java.util.*;
import java.io.ByteArrayOutputStream;

/** Account-scoped voluntary profile. Photos are cropped and re-encoded without EXIF metadata. */
final class FriendsProfile {
 static String s(String en,String ru){return FriendsPanel.s(en,ru);}
 static String name(Map<String,Object> p,String fallback){String n=String.valueOf(p.getOrDefault("displayName",fallback));return n.trim().isEmpty()?fallback:n;}
 static View avatar(MainActivity a,Map<String,Object> p,String name,int size){Portrait view=new Portrait(a,PlanImages.decode(String.valueOf(p.getOrDefault("avatar",""))),name,false);view.setContentDescription(name);return view;}
 static void header(MainActivity a,LinearLayout top){a.profileButton=new FrameLayout(a);top.addView(a.profileButton,new LinearLayout.LayoutParams(a.dp(48),a.dp(48)));a.profileButton.setContentDescription(s("My profile","Мой профиль"));a.profileButton.setTooltipText(s("My profile","Мой профиль"));a.clickable(a.profileButton,()->{if(a.page.equals("profile"))return;a.page="profile";a.render();});headerImage(a);}
 // One account-scoped disk cache provides the first frame; server refresh is silent.
 static final class Header {
  final String uid;final Map<String,Object> portrait=new HashMap<>();long checked;int revision;boolean loading;
  Header(MainActivity a,String uid){this.uid=uid;android.content.SharedPreferences p=a.getSharedPreferences("profile-avatar-cache",0);if(!uid.isEmpty()&&uid.equals(p.getString("uid",""))){portrait.put("avatar",p.getString("avatar",""));portrait.put("displayName",p.getString("name","T"));}else p.edit().clear().apply();}
 }
 static Header cache(MainActivity a){String uid=FriendsPanel.uid(a);if(a.headerProfile==null||!a.headerProfile.uid.equals(uid))a.headerProfile=new Header(a,uid);return a.headerProfile;}
 static void remember(MainActivity a,Map<String,Object> portrait){Header h=cache(a);if(h.uid.isEmpty())return;h.revision++;h.portrait.clear();h.portrait.putAll(portrait);h.checked=android.os.SystemClock.elapsedRealtime();a.getSharedPreferences("profile-avatar-cache",0).edit().putString("uid",h.uid).putString("avatar",String.valueOf(portrait.getOrDefault("avatar",""))).putString("name",name(portrait,"T")).apply();headerImage(a);}
 static void headerImage(MainActivity a){if(a.profileButton==null)return;Header h=cache(a);a.profileButton.removeAllViews();View picture;if(!h.uid.isEmpty()&&!h.portrait.isEmpty()){picture=avatar(a,h.portrait,name(h.portrait,"T"),30);}else{ImageView icon=new ImageView(a);icon.setImageDrawable(new LineIcon("person",a.MUTED));icon.setPadding(a.dp(3),a.dp(3),a.dp(3),a.dp(3));picture=icon;}picture.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(a.dp(30),a.dp(30),Gravity.CENTER);a.profileButton.addView(picture,lp);
  if(h.uid.isEmpty()||h.loading||(h.checked>0&&android.os.SystemClock.elapsedRealtime()-h.checked<60000))return;h.loading=true;int revision=h.revision;
  FriendsPanel.worker.execute(()->{try{FriendsStore store=new FriendsStore();if(!store.uid.equals(h.uid))return;Map<String,Object> data=store.portrait(h.uid);a.runOnUiThread(()->{h.loading=false;if(a.isDestroyed()||a.headerProfile!=h||!h.uid.equals(FriendsPanel.uid(a))||h.revision!=revision)return;remember(a,data);});}catch(Exception ignored){a.runOnUiThread(()->{h.loading=false;h.checked=android.os.SystemClock.elapsedRealtime();});}});
 }
 static void edit(MainActivity a,FriendsPanel.State state){LinearLayout box=a.form();EditText name=a.input(box,s("Display name","Имя"),name(state.portrait,String.valueOf(state.profile.get("name"))),false);EditText bio=a.input(box,s("About your training","О твоих тренировках"),String.valueOf(state.portrait.getOrDefault("bio","")),false);bio.setHint(s("Streetlifting · getting stronger","Стритлифтинг · становлюсь сильнее"));box.addView(a.text(s("Friends can see your profile. Submitting a plan for publication also includes a copy of your name and photo.","Профиль виден друзьям. При отправке плана на публикацию также передаётся копия имени и фото."),13,a.MUTED));
  AlertDialog dialog=a.formDialog(s("Edit profile","Изменить профиль"),box,s("Save","Сохранить"),()->{String n=name.getText().toString().trim(),b=bio.getText().toString().trim();if(n.isEmpty()||n.length()>40||b.length()>160)throw new IllegalArgumentException(s("Name: 1–40 characters. Bio: up to 160.","Имя: 1–40 символов. Описание: до 160."));Map<String,Object> fields=new HashMap<>();fields.put("displayName",n);fields.put("bio",b);FriendsPanel.run(a,store->{store.updatePortrait(fields);return true;},ok->FriendsPanel.refresh(a));});
  a.link(box,s("Change photo","Изменить фото"),()->{dialog.dismiss();pickPhoto(a);});
  if(!String.valueOf(state.portrait.getOrDefault("avatar","")).isEmpty())a.link(box,s("Remove photo","Убрать фото"),()->{dialog.dismiss();FriendsPanel.run(a,store->{store.updatePortrait(Collections.singletonMap("avatar",""));return true;},ok->FriendsPanel.refresh(a));});
 }
 static void pickPhoto(MainActivity a){a.friendsPhotoUid=FriendsPanel.uid(a);a.startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE),46);}
 static void photoSelected(MainActivity a,Uri uri){String owner=a.friendsPhotoUid;a.friendsPhotoUid="";if(owner.isEmpty()||!owner.equals(FriendsPanel.uid(a)))return;FriendsPanel.worker.execute(()->{try{String encoded=PlanImages.read(a,uri);a.runOnUiThread(()->{if(a.isDestroyed()||!owner.equals(FriendsPanel.uid(a)))return;crop(a,encoded,owner);});}catch(Exception e){a.runOnUiThread(()->{if(!a.isDestroyed())a.error(e);});}});}
 static void crop(MainActivity a,String encoded,String owner){Bitmap image=PlanImages.decode(encoded);if(image==null)return;LinearLayout box=a.form();box.addView(a.text(s("Move and pinch to frame your photo.","Двигай и увеличивай фото пальцами."),14,a.MUTED));a.space(box,16);Portrait preview=new Portrait(a,image,"",true);box.addView(preview,new LinearLayout.LayoutParams(-1,-2));a.space(box,12);box.addView(a.text(s("Visible to your accepted friends after saving.","После сохранения фото увидят подтверждённые друзья."),13,a.MUTED));
  a.formDialog(s("Profile photo","Фото профиля"),box,s("Save photo","Сохранить фото"),()->{if(!owner.equals(FriendsPanel.uid(a)))throw new IllegalStateException(s("Account changed","Аккаунт изменился"));String photo=preview.encode();FriendsPanel.run(a,store->{store.updatePortrait(Collections.singletonMap("avatar",photo));return true;},ok->FriendsPanel.refresh(a));});
 }
 static final class Portrait extends View {
  final Bitmap image;final String initials;final boolean editable;final CoverFrame frame=new CoverFrame(.5f,.5f,1);final Paint paint=new Paint(3);final ScaleGestureDetector pinch;float x,y;
  Portrait(MainActivity a,Bitmap image,String name,boolean editable){super(a);this.image=image;this.editable=editable;initials=name.isEmpty()?"T":name.substring(0,name.offsetByCodePoints(0,1)).toUpperCase(Locale.ROOT);pinch=new ScaleGestureDetector(a,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){if(image!=null){frame.scaleTo(frame.zoom*d.getScaleFactor(),d.getFocusX(),d.getFocusY(),image.getWidth(),image.getHeight(),getWidth(),getHeight());invalidate();}return true;}});}
  protected void onMeasure(int w,int h){int size=MeasureSpec.getSize(w);setMeasuredDimension(size,size);}
  void photo(Canvas c,int size){float[] b=frame.bounds(image.getWidth(),image.getHeight(),size,size);c.drawBitmap(image,null,new RectF(b[0],b[1],b[0]+b[2],b[1]+b[3]),paint);}
  protected void onDraw(Canvas c){int w=getWidth();Path clip=new Path();clip.addCircle(w/2f,w/2f,w/2f,Path.Direction.CW);c.save();c.clipPath(clip);if(image!=null)photo(c,w);else{paint.setShader(new LinearGradient(0,0,w,w,0xFF358A80,0xFF24404D,Shader.TileMode.CLAMP));c.drawRect(0,0,w,w,paint);paint.setShader(null);paint.setColor(0xFFE4FFF5);paint.setTextSize(w*.38f);paint.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));paint.setTextAlign(Paint.Align.CENTER);c.drawText(initials,w/2f,w/2f-(paint.ascent()+paint.descent())/2,paint);}c.restore();}
  String encode(){Bitmap out=Bitmap.createBitmap(256,256,Bitmap.Config.ARGB_8888);photo(new Canvas(out),256);ByteArrayOutputStream bytes=new ByteArrayOutputStream();out.compress(Bitmap.CompressFormat.JPEG,85,bytes);out.recycle();return android.util.Base64.encodeToString(bytes.toByteArray(),android.util.Base64.NO_WRAP);}
  public boolean onTouchEvent(MotionEvent e){if(!editable)return super.onTouchEvent(e);pinch.onTouchEvent(e);switch(e.getActionMasked()){case MotionEvent.ACTION_DOWN:x=e.getX();y=e.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;case MotionEvent.ACTION_MOVE:if(!pinch.isInProgress()&&e.getPointerCount()==1&&image!=null){frame.drag(e.getX()-x,e.getY()-y,image.getWidth(),image.getHeight(),getWidth(),getHeight());invalidate();}x=e.getX();y=e.getY();return true;case MotionEvent.ACTION_UP:performClick();case MotionEvent.ACTION_CANCEL:getParent().requestDisallowInterceptTouchEvent(false);return true;default:return true;}}
  public boolean performClick(){super.performClick();return true;}
 }
}
