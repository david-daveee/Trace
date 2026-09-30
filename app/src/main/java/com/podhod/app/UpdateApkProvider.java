package com.podhod.app;
import android.content.*;
import android.database.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
/** Read-only, per-file URI grants for verified update APK. */
public final class UpdateApkProvider extends ContentProvider {
    public boolean onCreate(){return true;}
    private File file(Uri uri)throws FileNotFoundException{
        if(!"content".equals(uri.getScheme())||!(getContext().getPackageName()+".updates").equals(uri.getAuthority())||!"/Trace.apk".equals(uri.getPath()))throw new FileNotFoundException("Invalid update URI");
        File f=UpdateDownload.apk(getContext());if(!f.isFile())throw new FileNotFoundException("Update expired");return f;
    }

    public String getType(Uri uri){return "application/vnd.android.package-archive";}
    public ParcelFileDescriptor openFile(Uri uri,String mode)throws FileNotFoundException{if(!"r".equals(mode))throw new FileNotFoundException("Read only");return ParcelFileDescriptor.open(file(uri),ParcelFileDescriptor.MODE_READ_ONLY);}
    public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort){
        try{File f=file(uri);String[] columns=projection==null?new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE}:projection;MatrixCursor cursor=new MatrixCursor(columns);Object[] row=new Object[columns.length];for(int i=0;i<columns.length;i++){if(OpenableColumns.DISPLAY_NAME.equals(columns[i]))row[i]=f.getName();else if(OpenableColumns.SIZE.equals(columns[i]))row[i]=f.length();}cursor.addRow(row);return cursor;}catch(FileNotFoundException e){return null;}
    }
    public Uri insert(Uri uri,ContentValues values){throw new UnsupportedOperationException("Read only");}
    public int update(Uri uri,ContentValues values,String where,String[] args){throw new UnsupportedOperationException("Read only");}
    public int delete(Uri uri,String where,String[] args){throw new UnsupportedOperationException("Read only");}
}
