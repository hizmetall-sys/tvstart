package com.hizmetall.tvstart;
import android.content.*;
public class Tasks extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if(Engine.REBOOT.equals(i.getAction())){Engine.schedule(c);if(Engine.prefs(c).getBoolean("daily",false))Engine.reboot(c);}else if(Engine.OPEN.equals(i.getAction())&&Engine.owner(c)&&Engine.prefs(c).getBoolean("auto",false))Engine.open(c);}
}