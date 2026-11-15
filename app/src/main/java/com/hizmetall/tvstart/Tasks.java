package com.hizmetall.tvstart;
import android.content.*;
public class Tasks extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){final PendingResult p=goAsync();final String action=i.getAction();new Thread(()->{try{if(Engine.REBOOT.equals(action)){Engine.schedule(c);if(Engine.prefs(c).getBoolean("daily",false))Engine.reboot(c);}else if(Engine.OPEN.equals(action)&&Engine.ready(c)&&Engine.prefs(c).getBoolean("auto",false)){Engine.attemptOpen(c);}}finally{p.finish();}}).start();}
}