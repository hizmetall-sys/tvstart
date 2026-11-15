package com.hizmetall.tvstart;
import android.content.*;
public class Boot extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){Engine.schedule(c);if(Intent.ACTION_BOOT_COMPLETED.equals(i.getAction())){Engine.log(c,"Cihaz açıldı");if(Engine.ready(c))Engine.queueOpen(c);else if(Engine.prefs(c).getBoolean("auto",false))Engine.log(c,"Otomatik açılış için cihaz sahibi veya harici ADB gerekli");}}
}