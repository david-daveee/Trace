package com.podhod.app;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.*;
import org.json.JSONObject;

/** Bounded transfer format: compressed chunks plus a digest checked before any restore. */
final class CloudSnapshot {
 static final int LIMIT=50*1024*1024, CHUNK=256*1024;
 final List<byte[]> chunks;final String sha256;final int bytes;
 private CloudSnapshot(List<byte[]> chunks,String sha256,int bytes){this.chunks=chunks;this.sha256=sha256;this.bytes=bytes;}
 static CloudSnapshot encode(JSONObject snapshot)throws IOException{
  byte[] raw=snapshot.toString().getBytes(StandardCharsets.UTF_8);
  if(raw.length>LIMIT)throw new IOException("Snapshot exceeds 50 MB");
  ByteArrayOutputStream compressed=new ByteArrayOutputStream();
  try(GZIPOutputStream gzip=new GZIPOutputStream(compressed)){gzip.write(raw);}
  byte[] bytes=compressed.toByteArray();if(bytes.length>LIMIT)throw new IOException("Compressed snapshot exceeds limit");
  List<byte[]> chunks=new ArrayList<>();for(int i=0;i<bytes.length;i+=CHUNK)chunks.add(Arrays.copyOfRange(bytes,i,Math.min(bytes.length,i+CHUNK)));
  return new CloudSnapshot(chunks,hash(bytes),bytes.length);
 }
 static JSONObject decode(List<byte[]> chunks,String sha256,int expectedBytes)throws IOException{
  if(expectedBytes<=0||expectedBytes>LIMIT||chunks==null||chunks.isEmpty()||chunks.size()!=(expectedBytes+CHUNK-1)/CHUNK)throw new IOException("Invalid snapshot manifest");
  ByteArrayOutputStream compressed=new ByteArrayOutputStream();
  for(int i=0;i<chunks.size();i++){byte[] chunk=chunks.get(i);int expected=Math.min(CHUNK,expectedBytes-i*CHUNK);if(chunk==null||chunk.length!=expected)throw new IOException("Missing or invalid snapshot chunk");compressed.write(chunk);}
  byte[] bytes=compressed.toByteArray();if(!hash(bytes).equals(sha256))throw new IOException("Snapshot checksum mismatch");
  ByteArrayOutputStream raw=new ByteArrayOutputStream();
  try(GZIPInputStream gzip=new GZIPInputStream(new ByteArrayInputStream(bytes))){byte[] buffer=new byte[8192];int n;while((n=gzip.read(buffer))!=-1){if(raw.size()+n>LIMIT)throw new IOException("Expanded snapshot exceeds limit");raw.write(buffer,0,n);}}
  try{return new JSONObject(raw.toString(StandardCharsets.UTF_8.name()));}catch(Exception e){throw new IOException("Invalid snapshot JSON",e);}
 }
 static String hash(byte[] bytes){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder text=new StringBuilder();for(byte b:digest)text.append(String.format(Locale.US,"%02x",b&255));return text.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
