package com.podhod.app;
import android.graphics.*;
import android.graphics.drawable.Drawable;
/** Small, consistent vector icons; no font-dependent symbol rendering. */
public final class LineIcon extends Drawable {
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);private final String name;
    public LineIcon(String name,int color){this.name=name;p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.8f);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);}
    void line(Canvas c,float x,float y,float xx,float yy){c.drawLine(x,y,xx,yy,p);}
    @Override public void draw(Canvas c){c.save();c.translate(getBounds().left,getBounds().top);c.scale(getBounds().width()/24f,getBounds().height()/24f);
        switch(name){
            case "plus":line(c,12,5,12,19);line(c,5,12,19,12);break;
            case "search":c.drawCircle(10.5f,10.5f,6.5f,p);line(c,15.5f,15.5f,21,21);break;
            case "heart":case "heart-filled":{Path h=new Path();h.moveTo(12,21);h.cubicTo(9,18.5f,2,13.5f,2,8);h.cubicTo(2,2.5f,9,1.5f,12,6);h.cubicTo(15,1.5f,22,2.5f,22,8);h.cubicTo(22,13.5f,15,18.5f,12,21);h.close();if(name.equals("heart-filled"))p.setStyle(Paint.Style.FILL);c.drawPath(h,p);p.setStyle(Paint.Style.STROKE);break;}

            case "eye":case "eye-off":case "eye-pending":{Path eye=new Path();eye.moveTo(2,12);eye.cubicTo(7,3,17,3,22,12);eye.cubicTo(17,21,7,21,2,12);c.drawPath(eye,p);c.drawCircle(12,12,3,p);if(name.equals("eye-off")){p.setStrokeWidth(2.4f);line(c,3,3,21,21);}if(name.equals("eye-pending")){c.drawCircle(19,5,3,p);}break;}
            case "back":line(c,19,12,5,12);line(c,5,12,11,6);line(c,5,12,11,18);break;
            case "refresh":c.drawArc(4,4,20,20,40,285,false,p);line(c,19,3,19,9);line(c,19,9,13,9);break;
            case "chat":{Path q=new Path();q.moveTo(5,3);q.lineTo(19,3);q.quadTo(21,3,21,5);q.lineTo(21,15);q.quadTo(21,17,19,17);q.lineTo(10,17);q.lineTo(4,21);q.lineTo(4,17);q.quadTo(3,17,3,15);q.lineTo(3,5);q.quadTo(3,3,5,3);c.drawPath(q,p);line(c,7,8,17,8);line(c,7,12,14,12);break;}
            case "person":c.drawCircle(12,7,4,p);c.drawArc(3,13,21,29,180,180,false,p);break;
            case "moderator":{Path shield=new Path();shield.moveTo(12,2);shield.lineTo(20,5);shield.lineTo(20,11);shield.cubicTo(20,16,16,20,12,22);shield.cubicTo(8,20,4,16,4,11);shield.lineTo(4,5);shield.close();c.drawPath(shield,p);line(c,8,11,11,14);line(c,11,14,16,9);break;}
            case "friends":c.drawCircle(9,8,3,p);c.drawCircle(18,9,2.5f,p);c.drawArc(2,13,16,26,180,180,false,p);c.drawArc(14,14,23,24,190,155,false,p);break;
            case "steps":c.drawOval(4,3,10,12,p);c.drawOval(14,9,20,18,p);c.drawArc(4,13,10,19,0,180,false,p);c.drawArc(14,19,20,23,0,180,false,p);break;
            case "file":c.drawRoundRect(5,3,19,21,2,2,p);line(c,8,8,16,8);line(c,8,12,16,12);line(c,8,16,13,16);break;
            case "export":line(c,12,3,12,15);line(c,8,7,12,3);line(c,12,3,16,7);line(c,4,13,4,21);line(c,4,21,20,21);line(c,20,21,20,13);break;
            case "home": {Path q=new Path();q.moveTo(3,10);q.lineTo(12,3);q.lineTo(21,10);q.moveTo(5,9);q.lineTo(5,21);q.lineTo(10,21);q.lineTo(10,15);q.lineTo(14,15);q.lineTo(14,21);q.lineTo(19,21);q.lineTo(19,9);c.drawPath(q,p);break;}
            case "mine": line(c,8,12,16,12);c.drawRoundRect(5,5,8,19,1,1,p);c.drawRoundRect(16,5,19,19,1,1,p);line(c,2,9,2,15);line(c,22,9,22,15);break;
            case "workout": {c.drawCircle(12,12,9,p);Path q=new Path();q.moveTo(10,8);q.lineTo(16,12);q.lineTo(10,16);q.close();c.drawPath(q,p);break;}
            case "library": c.drawRoundRect(4,4,11,11,1.5f,1.5f,p);c.drawRoundRect(15,4,21,11,1.5f,1.5f,p);c.drawRoundRect(4,15,11,21,1.5f,1.5f,p);c.drawRoundRect(15,15,21,21,1.5f,1.5f,p);break;
            case "progress":line(c,4,20,4,13);line(c,10,20,10,9);line(c,16,20,16,4);line(c,22,20,22,7);break;
            case "history": c.drawArc(4,4,21,21,-140,320,false,p);line(c,3,4,3,10);line(c,3,10,9,10);line(c,12,8,12,13);line(c,12,13,16,15);break;
            case "settings":line(c,3,6,21,6);line(c,3,12,21,12);line(c,3,18,21,18);p.setStyle(Paint.Style.FILL);c.drawCircle(8,6,2.6f,p);c.drawCircle(16,12,2.6f,p);c.drawCircle(10,18,2.6f,p);p.setStyle(Paint.Style.STROKE);break;
            case "check":line(c,5,12,10,17);line(c,10,17,20,6);break;
            case "arrow":line(c,5,12,19,12);line(c,14,7,19,12);line(c,19,12,14,17);break;
        }c.restore();}
    @Override public void setAlpha(int a){p.setAlpha(a);}@Override public void setColorFilter(ColorFilter f){p.setColorFilter(f);}@Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
