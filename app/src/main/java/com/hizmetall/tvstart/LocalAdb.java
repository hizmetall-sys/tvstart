package com.hizmetall.tvstart;
import android.content.Context;
import android.util.Base64;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.math.BigInteger;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import javax.crypto.Cipher;

final class LocalAdb implements Closeable {
 private static final int CNXN=0x4e584e43, AUTH=0x48545541, OPEN=0x4e45504f, OKAY=0x59414b4f, WRTE=0x45545257, CLSE=0x45534c43;
 private final Socket sock=new Socket(); private InputStream in; private OutputStream out;
 private static class Packet {int cmd,a,b; byte[] data; Packet(int c,int x,int y,byte[] d){cmd=c;a=x;b=y;data=d;}}
 private static byte[] str(String v){return v.getBytes(StandardCharsets.UTF_8);}
 private static synchronized KeyPair keys(Context c)throws Exception {
  File f=new File(c.getFilesDir(),"local-adb.key"); KeyFactory k=KeyFactory.getInstance("RSA");
  if(f.exists()){try(DataInputStream d=new DataInputStream(new FileInputStream(f))){int n=d.readInt();if(n<1||n>8192)throw new IOException("Anahtar hatalı");byte[] priv=new byte[n];d.readFully(priv);n=d.readInt();if(n<1||n>8192)throw new IOException("Anahtar hatalı");byte[] pub=new byte[n];d.readFully(pub);return new KeyPair(k.generatePublic(new X509EncodedKeySpec(pub)),k.generatePrivate(new PKCS8EncodedKeySpec(priv)));}}
  KeyPairGenerator g=KeyPairGenerator.getInstance("RSA");g.initialize(2048);KeyPair p=g.generateKeyPair();
  File tmp=new File(c.getFilesDir(),"local-adb.tmp");try(DataOutputStream d=new DataOutputStream(new FileOutputStream(tmp))){byte[] a=p.getPrivate().getEncoded(),b=p.getPublic().getEncoded();d.writeInt(a.length);d.write(a);d.writeInt(b.length);d.write(b);}if(!tmp.renameTo(f))throw new IOException("Anahtar kaydedilemedi");return p;
 }
 private static void little(ByteBuffer b,BigInteger x){for(int i=0;i<256;i++)b.put(x.shiftRight(i*8).byteValue());}
 private static byte[] publicKey(RSAPublicKey k){BigInteger n=k.getModulus(),r=BigInteger.ONE.shiftLeft(32);ByteBuffer b=ByteBuffer.allocate(524).order(ByteOrder.LITTLE_ENDIAN);b.putInt(64);b.putInt(n.modInverse(r).negate().intValue());little(b,n);little(b,BigInteger.ONE.shiftLeft(4096).mod(n));b.putInt(k.getPublicExponent().intValue());return str(Base64.encodeToString(b.array(),Base64.NO_WRAP)+" TVStart@localhost\0");}
 private static byte[] sign(PrivateKey key,byte[] token)throws Exception {if(token.length!=20)throw new IOException("Beklenmeyen ADB token");byte[] prefix={0x30,0x21,0x30,0x09,0x06,0x05,0x2b,0x0e,0x03,0x02,0x1a,0x05,0x00,0x04,0x14};byte[] v=new byte[35];System.arraycopy(prefix,0,v,0,15);System.arraycopy(token,0,v,15,20);Cipher c=Cipher.getInstance("RSA/ECB/PKCS1Padding");c.init(Cipher.ENCRYPT_MODE,key);return c.doFinal(v);}
 private void send(int cmd,int a,int b,byte[] data)throws IOException{int sum=0;for(byte x:data)sum+=x&255;ByteBuffer h=ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);h.putInt(cmd).putInt(a).putInt(b).putInt(data.length).putInt(sum).putInt(~cmd);out.write(h.array());out.write(data);out.flush();}
 private byte[] read(int n)throws IOException{byte[] x=new byte[n];new DataInputStream(in).readFully(x);return x;}
 private Packet receive()throws IOException{ByteBuffer h=ByteBuffer.wrap(read(24)).order(ByteOrder.LITTLE_ENDIAN);int c=h.getInt(),a=h.getInt(),b=h.getInt(),n=h.getInt();h.getInt();if(h.getInt()!=~c||n<0||n>1048576)throw new IOException("Geçersiz ADB yanıtı");return new Packet(c,a,b,read(n));}
 private void connect(Context c,boolean interactive)throws Exception{sock.connect(new InetSocketAddress("127.0.0.1",5555),2500);sock.setSoTimeout(interactive?60000:4000);in=sock.getInputStream();out=sock.getOutputStream();send(CNXN,0x01000000,4096,str("host::\0"));int attempt=0;KeyPair k=null;while(true){Packet p=receive();if(p.cmd==CNXN)return;if(p.cmd!=AUTH||p.a!=1||attempt>=2)throw new IOException("ADB yetkisi verilmedi");if(k==null)k=keys(c);if(attempt++==0)send(AUTH,2,0,sign(k.getPrivate(),p.data));else{if(!interactive)throw new IOException("Önce ADB bağlantısını onaylayın");send(AUTH,3,0,publicKey((RSAPublicKey)k.getPublic()));}}}
 private String command(String service,boolean reboot)throws Exception{send(OPEN,1,0,str(service+"\0"));boolean accepted=false;ByteArrayOutputStream result=new ByteArrayOutputStream();while(true){Packet p;try{p=receive();}catch(IOException e){if(reboot&&accepted)return "";throw e;}if(p.cmd==OKAY&&p.b==1){accepted=true;}else if(p.cmd==WRTE&&p.b==1){if(result.size()+p.data.length>65536)throw new IOException("ADB yanıtı çok uzun");result.write(p.data);send(OKAY,1,p.a,new byte[0]);}else if(p.cmd==CLSE&&p.b==1){send(CLSE,1,p.a,new byte[0]);if(!accepted)throw new IOException("ADB komutu reddedildi");return result.toString("UTF-8");}else throw new IOException("Beklenmeyen ADB yanıtı");}}
 static boolean test(Context c)throws Exception{try(LocalAdb a=new LocalAdb()){a.connect(c,true);return a.command("shell:echo TVSTART_OK",false).contains("TVSTART_OK");}}
 static void reboot(Context c)throws Exception{try(LocalAdb a=new LocalAdb()){a.connect(c,false);a.command("reboot:",true);}}
 static void launch(Context c,String component)throws Exception{if(!component.matches("[A-Za-z0-9_.$]+/[A-Za-z0-9_.$]+"))throw new IOException("Uygulama bileşeni geçersiz");try(LocalAdb a=new LocalAdb()){a.connect(c,false);String v=a.command("shell:am start --user current -n "+component,false);if(v.contains("Error")||v.contains("Exception"))throw new IOException(v.trim());}}
 static void channel(Context c,boolean up)throws Exception{try(LocalAdb a=new LocalAdb()){a.connect(c,false);a.command("shell:input keyevent "+(up?166:167),false);}}
 @Override public void close()throws IOException{sock.close();}
}