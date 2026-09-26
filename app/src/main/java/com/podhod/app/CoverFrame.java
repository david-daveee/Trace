package com.podhod.app;
/** Normalized crop state shared by the editor and every plan cover. */
final class CoverFrame {
    float x,y,zoom;
    CoverFrame(float x,float y,float zoom){this.x=clamp(x,0,1);this.y=clamp(y,0,1);this.zoom=clamp(zoom,1,4);}
    static float clamp(float n,float low,float high){return Float.isFinite(n)?Math.max(low,Math.min(high,n)):low;}
    float[] bounds(float iw,float ih,float w,float h){
        float scale=Math.max(w/iw,h/ih)*zoom,pw=iw*scale,ph=ih*scale;
        return new float[]{-(pw-w)*x,-(ph-h)*y,pw,ph};
    }
    void drag(float dx,float dy,float iw,float ih,float w,float h){float[] b=bounds(iw,ih,w,h);if(b[2]>w)x=clamp(x-dx/(b[2]-w),0,1);if(b[3]>h)y=clamp(y-dy/(b[3]-h),0,1);}
    void scaleTo(float value,float fx,float fy,float iw,float ih,float w,float h){
        float[] before=bounds(iw,ih,w,h);float px=(fx-before[0])/before[2],py=(fy-before[1])/before[3];
        zoom=clamp(value,1,4);float[] after=bounds(iw,ih,w,h);
        x=after[2]>w?clamp((px*after[2]-fx)/(after[2]-w),0,1):.5f;
        y=after[3]>h?clamp((py*after[3]-fy)/(after[3]-h),0,1):.5f;
    }
}
