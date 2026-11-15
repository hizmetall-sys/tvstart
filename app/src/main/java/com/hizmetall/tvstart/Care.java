package com.hizmetall.tvstart;
import android.app.*;
import android.content.*;
import android.net.*;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import java.text.SimpleDateFormat;
import java.util.*;

final class Care {
 static final String CLEAN="com.hizmetall.tvstart.CARE", NET="com.hizmetall.tvstart.NET";
 static synchronized void schedule(Context c,boolean reset){
  AlarmManager a=c.getSystemService(AlarmManager.class);if(a==null)return;
  if(!Engine.prefs(c).getBoolean("care",true)||!Engine.prefs(c).getBoolean("adb",false)){a.cancel(Engine.pi(c,CLEAN));Engine.prefs(c).edit().remove("careNext").apply();}
  else if(reset||Engine.prefs(c).getLong("careNext",0)<=SystemClock.elapsedRealtime()){
   long when=SystemClock.elapsedRealtime()+Engine.prefs(c).getInt("careHours",2)*3600000L;
   a.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,Engine.pi(c,CLEAN));Engine.prefs(c).edit().putLong("careNext",when).apply();
  }
  if(!Engine.prefs(c).getBoolean("netCare",false)||!Engine.prefs(c).getBoolean("adb",false)){a.cancel(Engine.pi(c,NET));Engine.prefs(c).edit().remove("netNext").remove("netBadSince").apply();}
  else if(reset||Engine.prefs(c).getLong("netNext",0)<=SystemClock.elapsedRealtime()){planNet(c,300000);}
 }
 static synchronized void planNet(Context c,long delay){AlarmManager a=c.getSystemService(AlarmManager.class);if(a==null)return;long when=SystemClock.elapsedRealtime()+delay;a.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,Engine.pi(c,NET));Engine.prefs(c).edit().putLong("netNext",when).apply();}
 static long available(Context c){ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();c.getSystemService(ActivityManager.class).getMemoryInfo(m);return m.availMem/1048576;}
 static int pressure(Context c){ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();c.getSystemService(ActivityManager.class).getMemoryInfo(m);return m.totalMem<=0?0:(int)(100*(m.totalMem-m.availMem)/m.totalMem);}
 static void note(Context c,String key,String text){Engine.prefs(c).edit().putString(key,new SimpleDateFormat("dd.MM HH:mm",Locale.getDefault()).format(new Date())+" • "+text).apply();}
 static synchronized void clean(Context c,boolean manual){
  if(!manual){Engine.prefs(c).edit().remove("careNext").apply();schedule(c,false);if(!Engine.prefs(c).getBoolean("care",true))return;}
  if(!Engine.prefs(c).getBoolean("adb",false)){note(c,"careLast","Yerel ADB testi gerekli");return;}
  long before=available(c);int p=pressure(c);
  if(!manual&&p<80&&before>=250){note(c,"careLast","RAM yeterli: %"+p+" / "+before+" MB; işlem gerekmedi");return;}
  try{LocalAdb.trimCached(c);Thread.sleep(700);note(c,"careLast","Arka plan bakımı: "+before+" → "+available(c)+" MB kullanılabilir");}catch(Exception e){note(c,"careLast","Bakım hatası: "+e.getMessage());}
 }
 static synchronized void network(Context c){
  if(!Engine.prefs(c).getBoolean("netCare",false)||!Engine.prefs(c).getBoolean("adb",false))return;
  planNet(c,300000);
  try{
   WifiManager wm=(WifiManager)c.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
   ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);Network n=cm.getActiveNetwork();NetworkCapabilities cap=n==null?null:cm.getNetworkCapabilities(n);
   if(wm==null||!wm.isWifiEnabled()||(cap!=null&&!cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI))||Engine.internetReady(c)){Engine.prefs(c).edit().remove("netBadSince").apply();return;}
   long now=SystemClock.elapsedRealtime(),bad=Engine.prefs(c).getLong("netBadSince",0),last=Engine.prefs(c).getLong("netRepairAt",0);
   if(bad==0||bad>now){Engine.prefs(c).edit().putLong("netBadSince",now).apply();planNet(c,60000);note(c,"netLast","İnternet doğrulanmadı; tekrar kontrol edilecek");return;}
   if(now-bad<60000){planNet(c,60000);return;}
   if(last>0&&last<=now&&now-last<1800000){note(c,"netLast","Kesinti sürüyor; onarım bekleme süresi");return;}
   Engine.prefs(c).edit().putLong("netRepairAt",now).remove("netBadSince").apply();
   LocalAdb.reconnectWifi(c);note(c,"netLast","Wi-Fi yeniden bağlantı isteği gönderildi");planNet(c,60000);
  }catch(Exception e){note(c,"netLast","Ağ bakım hatası: "+e.getMessage());}
 }
}