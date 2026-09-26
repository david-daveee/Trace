package com.podhod.app;
import android.content.Context;
import android.graphics.*;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Base64;
import android.util.LruCache;
import java.io.*;
/** Small embedded covers travel with JSON exports and backups. No gallery-wide access. */
final class PlanImages {
    private static final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(8*1024*1024){
        protected int sizeOf(String key,Bitmap value){return value.getByteCount();}
    };
    static Bitmap decode(String encoded){
        if(encoded.isEmpty()||encoded.length()>2000000)return null;
        Bitmap cached=cache.get(encoded);if(cached!=null)return cached;
        try{
            byte[] bytes=Base64.decode(encoded,Base64.DEFAULT);
            Bitmap b=sample(bytes);if(b!=null)cache.put(encoded,b);return b;
        }catch(Exception e){return null;}
    }
    private static Bitmap sample(byte[] bytes){
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
        if(o.outWidth<=0||o.outHeight<=0)return null;
        o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>1024)o.inSampleSize*=2;
        o.inJustDecodeBounds=false;return BitmapFactory.decodeByteArray(bytes,0,bytes.length,o);
    }
    static String read(Context c,Uri uri)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(InputStream in=c.getContentResolver().openInputStream(uri)){
            if(in==null)throw new IOException(Lang.t("Не удалось открыть изображение"));
            byte[] buffer=new byte[8192];int n;
            while((n=in.read(buffer))!=-1){if(bytes.size()+n>20*1024*1024)throw new IOException(Lang.t("Выбери изображение размером до 20 МБ"));bytes.write(buffer,0,n);}
        }
        byte[] raw=bytes.toByteArray();Bitmap photo=sample(raw);
        if(photo==null)throw new IOException(Lang.t("Не удалось открыть изображение"));
        Matrix m=new Matrix();
        try{
            int orientation=new ExifInterface(new ByteArrayInputStream(raw)).getAttributeInt(ExifInterface.TAG_ORIENTATION,1);
            switch(orientation){
                case 2:m.setScale(-1,1);break;
                case 3:m.setRotate(180);break;
                case 4:m.setScale(1,-1);break;
                case 5:m.setRotate(90);m.postScale(-1,1);break;
                case 6:m.setRotate(90);break;
                case 7:m.setRotate(-90);m.postScale(-1,1);break;
                case 8:m.setRotate(-90);break;
            }
        }catch(IOException ignored){}
        Bitmap oriented=Bitmap.createBitmap(photo,0,0,photo.getWidth(),photo.getHeight(),m,true);
        ByteArrayOutputStream out=new ByteArrayOutputStream();oriented.compress(Bitmap.CompressFormat.JPEG,85,out);
        if(oriented!=photo)oriented.recycle();photo.recycle();
        return Base64.encodeToString(out.toByteArray(),Base64.NO_WRAP);
    }
}
