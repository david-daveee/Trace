package com.podhod.app;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Locally bundled photo, cropped without stretching and shaded beneath foreground text. */
final class PhotoBackdrop extends Drawable {
    private final Bitmap photo;
    private final float density;
    private final int mode;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    PhotoBackdrop(Bitmap photo,float density,int mode){this.photo=photo;this.density=density;this.mode=mode;}
    @Override public void draw(Canvas canvas){
        Rect bounds=getBounds();float w=bounds.width(),h=bounds.height();
        if(w<=0||h<=0)return;
        canvas.save();canvas.translate(bounds.left,bounds.top);
        if(mode!=0){Path clip=new Path();clip.addRoundRect(new RectF(0,0,w,h),24*density,24*density,Path.Direction.CW);canvas.clipPath(clip);}
        canvas.drawColor(0xFF0D131C);
        float photoHeight=mode==0?Math.min(h,440*density):h;
        float scale=Math.max(w/photo.getWidth(),photoHeight/photo.getHeight());
        float pw=photo.getWidth()*scale,ph=photo.getHeight()*scale;
        paint.setShader(null);paint.setAlpha(mode==0?80:mode==2?95:255);
        canvas.drawBitmap(photo,null,new RectF((w-pw)/2,(photoHeight-ph)/2,(w+pw)/2,(photoHeight+ph)/2),paint);
        paint.setAlpha(255);
        if(mode==0)paint.setShader(new LinearGradient(0,0,0,photoHeight,new int[]{0x280D131C,0x600D131C,0xFF0D131C},new float[]{0,.5f,1},Shader.TileMode.CLAMP));
        else if(mode==3)paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xA00D131C,0x400D131C,0xC80D131C},null,Shader.TileMode.CLAMP));
        else paint.setShader(new LinearGradient(0,0,w,h,new int[]{0xC80D131C,0x790D131C,0xE80D131C},null,Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,w,mode==0?photoHeight:h,paint);paint.setShader(null);
        if(mode!=0){paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(density);paint.setColor(0xFF354850);canvas.drawRoundRect(new RectF(density/2,density/2,w-density/2,h-density/2),24*density,24*density,paint);paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);}
        canvas.restore();
    }
    @Override public void setAlpha(int alpha){}
    @Override public void setColorFilter(ColorFilter filter){}
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
