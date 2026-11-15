package com.hizmetall.tvstart;
import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.SystemClock;
import java.util.Calendar;

final class Engine {
    static final String REBOOT="com.hizmetall.tvstart.REBOOT", OPEN="com.hizmetall.tvstart.OPEN";
    static SharedPreferences prefs(Context c){return c.getSharedPreferences("tvstart",0);}
    static DevicePolicyManager policy(Context c){return c.getSystemService(DevicePolicyManager.class);}
    static boolean owner(Context c){return policy(c)!=null && policy(c).isDeviceOwnerApp(c.getPackageName());}
    static boolean ready(Context c){return owner(c)||prefs(c).getBoolean("adb",false);}
    static void log(Context c,String msg){prefs(c).edit().putString("last",new java.text.SimpleDateFormat("dd.MM HH:mm",java.util.Locale.getDefault()).format(new java.util.Date())+" • "+msg).apply();}
    static PendingIntent pi(Context c,String action){return PendingIntent.getBroadcast(c,action.equals(REBOOT)?10:11,new Intent(c,Tasks.class).setAction(action),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    static void schedule(Context c){
        AlarmManager a=c.getSystemService(AlarmManager.class); if(a==null)return;
        PendingIntent p=pi(c,REBOOT); a.cancel(p);
        if(!prefs(c).getBoolean("daily",false)||!ready(c)){prefs(c).edit().remove("next").apply();return;}
        Calendar next=Calendar.getInstance();next.set(Calendar.HOUR_OF_DAY,prefs(c).getInt("hour",4));next.set(Calendar.MINUTE,prefs(c).getInt("minute",0));next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
        if(next.getTimeInMillis()<=System.currentTimeMillis())next.add(Calendar.DAY_OF_YEAR,1);
        a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),p);
        prefs(c).edit().putLong("next",next.getTimeInMillis()).apply();
    }
    static void queueOpen(Context c){if(!prefs(c).getBoolean("auto",false))return; AlarmManager a=c.getSystemService(AlarmManager.class);if(a!=null)a.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime()+45000,pi(c,OPEN));}
    static boolean openBackground(Context c){if(owner(c))return open(c);try{String pkg=prefs(c).getString("package","");Intent i=c.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(i==null)i=c.getPackageManager().getLaunchIntentForPackage(pkg);if(i==null||i.getComponent()==null)throw new Exception("Uygulama seçilmedi");LocalAdb.launch(c,i.getComponent().flattenToString());log(c,"ADB uygulama açılışı");return true;}catch(Exception e){log(c,"ADB açılış hatası: "+e.getMessage());return false;}}
    static boolean open(Context c){String pkg=prefs(c).getString("package",""); if(pkg.isEmpty()){log(c,"Uygulama seçilmedi");return false;}
        Intent i=c.getPackageManager().getLeanbackLaunchIntentForPackage(pkg);if(i==null)i=c.getPackageManager().getLaunchIntentForPackage(pkg);
        if(i==null){log(c,"Uygulama bulunamadı");return false;}
        try{c.startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));log(c,"Uygulama açma isteği gönderildi");return true;}catch(RuntimeException e){log(c,"Açılış engellendi: "+e.getClass().getSimpleName());return false;}
    }
    static boolean reboot(Context c){if(!owner(c)){if(!ready(c)){log(c,"Önce yerel ADB bağlantısını test edin");return false;}try{log(c,"ADB yeniden başlatma isteği");LocalAdb.reboot(c);return true;}catch(Exception e){log(c,"ADB hatası: "+e.getMessage());return false;}}try{log(c,"Yeniden başlatma isteği");policy(c).reboot(new ComponentName(c,Admin.class));return true;}catch(RuntimeException e){log(c,"Yeniden başlatma hatası: "+e.getClass().getSimpleName());return false;}}
}