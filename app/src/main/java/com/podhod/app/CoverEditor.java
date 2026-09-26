package com.podhod.app;
import android.app.AlertDialog;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
/** Adjusts a local draft; cancel never changes the stored cover. */
final class CoverEditor {
    static AlertDialog show(MainActivity a,JSONObject plan,String encoded,boolean existing){
        Bitmap photo=PlanImages.decode(encoded);if(photo==null){a.error(new IllegalArgumentException(Lang.t("Не удалось открыть изображение")));return null;}
        CoverFrame frame=new CoverFrame(existing?(float)plan.optDouble("coverX",.5):.5f,existing?(float)plan.optDouble("coverY",.5):.5f,existing?(float)plan.optDouble("coverZoom",1):1);
        LinearLayout panel=a.column();panel.setPadding(a.dp(20),a.dp(12),a.dp(20),a.dp(8));
        panel.addView(a.text(Lang.t("Двигай фото пальцем. Разведи два пальца, чтобы увеличить."),14,a.MUTED));a.space(panel,16);
        CropView preview=new CropView(a,photo,frame);panel.addView(preview,new LinearLayout.LayoutParams(-1,-2));a.space(panel,16);
        TextView label=a.text("",14,a.TEXT);panel.addView(label);
        SeekBar zoom=new SeekBar(a);zoom.setMax(300);zoom.setProgress(Math.round((frame.zoom-1)*100));zoom.setContentDescription(Lang.t("Масштаб обложки"));panel.addView(zoom,new LinearLayout.LayoutParams(-1,a.dp(48)));
        Runnable sync=()->{label.setText(Lang.t("Масштаб обложки")+" · "+String.format(java.util.Locale.ROOT,"%.1f×",frame.zoom));zoom.setProgress(Math.round((frame.zoom-1)*100));};preview.changed=sync;
        zoom.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}public void onProgressChanged(SeekBar s,int value,boolean user){if(user){frame.scaleTo(1+value/100f,preview.getWidth()/2f,preview.getHeight()/2f,photo.getWidth(),photo.getHeight(),Math.max(1,preview.getWidth()),Math.max(1,preview.getHeight()));preview.invalidate();sync.run();}}});
        a.link(panel,Lang.t("Сбросить положение"),()->{frame.x=.5f;frame.y=.5f;frame.zoom=1;preview.invalidate();sync.run();});sync.run();
        AlertDialog dialog=new AlertDialog.Builder(a).setTitle(Lang.t("Настроить обложку")).setView(panel).setNegativeButton(Lang.t("Отмена"),null).setPositiveButton(Lang.t("Сохранить"),null).create();
        dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
            JSONObject current=a.store.program(plan.optString("id"));if(current==null){dialog.dismiss();return;}
            try{Engine.put(current,"coverImage",encoded);Engine.put(current,"coverX",frame.x);Engine.put(current,"coverY",frame.y);Engine.put(current,"coverZoom",frame.zoom);a.store.save();dialog.dismiss();a.open(current);}catch(Exception e){a.error(e);}
        }));dialog.show();return dialog;
    }
    static final class CropView extends View {
        final Bitmap photo;final CoverFrame frame;final PhotoBackdrop backdrop;final ScaleGestureDetector detector;
        Runnable changed=()->{};float lastX,lastY;int pointer=-1;
        CropView(MainActivity a,Bitmap photo,CoverFrame frame){super(a);this.photo=photo;this.frame=frame;backdrop=new PhotoBackdrop(photo,getResources().getDisplayMetrics().density,3,frame);setContentDescription(Lang.t("Двигай фото пальцем. Разведи два пальца, чтобы увеличить."));
            detector=new ScaleGestureDetector(a,new ScaleGestureDetector.SimpleOnScaleGestureListener(){public boolean onScale(ScaleGestureDetector d){frame.scaleTo(frame.zoom*d.getScaleFactor(),d.getFocusX(),d.getFocusY(),photo.getWidth(),photo.getHeight(),getWidth(),getHeight());invalidate();changed.run();return true;}});
        }
        protected void onMeasure(int widthSpec,int heightSpec){int width=MeasureSpec.getSize(widthSpec);setMeasuredDimension(width,Math.round(width/2.2f));}
        protected void onDraw(Canvas c){backdrop.setBounds(0,0,getWidth(),getHeight());backdrop.draw(c);}
        public boolean onTouchEvent(MotionEvent e){
            detector.onTouchEvent(e);
            switch(e.getActionMasked()){
                case MotionEvent.ACTION_DOWN:pointer=e.getPointerId(0);lastX=e.getX();lastY=e.getY();getParent().requestDisallowInterceptTouchEvent(true);return true;
                case MotionEvent.ACTION_MOVE:
                    int i=e.findPointerIndex(pointer);if(i<0)return true;
                    float x=e.getX(i),y=e.getY(i);if(!detector.isInProgress()&&e.getPointerCount()==1){frame.drag(x-lastX,y-lastY,photo.getWidth(),photo.getHeight(),getWidth(),getHeight());invalidate();}
                    lastX=x;lastY=y;return true;
                case MotionEvent.ACTION_POINTER_UP:
                    int keep=e.getActionIndex()==0?1:0;pointer=e.getPointerId(keep);lastX=e.getX(keep);lastY=e.getY(keep);return true;
                case MotionEvent.ACTION_UP:performClick();pointer=-1;getParent().requestDisallowInterceptTouchEvent(false);return true;
                case MotionEvent.ACTION_CANCEL:pointer=-1;getParent().requestDisallowInterceptTouchEvent(false);return true;
                default:return true;
            }
        }
        public boolean performClick(){super.performClick();return true;}
    }
}
