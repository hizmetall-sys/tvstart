package com.hizmetall.tvstart;
import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
public class RemoteService extends AccessibilityService {
 private static final String PKG="com.turkcell.ott", LIVE=PKG+".androidtv.presentation.ui.player.live.TvLivePlayerActivity";
 private boolean live=false; private int held=0; private final ExecutorService executor=Executors.newSingleThreadExecutor();
 @Override public void onAccessibilityEvent(AccessibilityEvent event){
  if(event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)return;
  live=PKG.contentEquals(event.getPackageName()==null?"":event.getPackageName()) && LIVE.contentEquals(event.getClassName()==null?"":event.getClassName());
 }
 @Override protected boolean onKeyEvent(KeyEvent e){
  int key=e.getKeyCode();if(key!=KeyEvent.KEYCODE_DPAD_UP && key!=KeyEvent.KEYCODE_DPAD_DOWN)return false;
  if(e.getAction()==KeyEvent.ACTION_UP && held==key){held=0;return true;}
  if(!live||!Engine.prefs(this).getBoolean("remote",false)||!Engine.prefs(this).getBoolean("adb",false))return false;
  AccessibilityNodeInfo root=getRootInActiveWindow();if(root==null)return false;
  boolean same=PKG.contentEquals(root.getPackageName()==null?"":root.getPackageName());root.recycle();if(!same)return false;
  if(e.getAction()==KeyEvent.ACTION_DOWN){held=key;if(e.getRepeatCount()==0){executor.execute(()->{try{LocalAdb.channel(this,key==KeyEvent.KEYCODE_DPAD_UP);}catch(Exception ex){Engine.log(this,"Kanal geçişi: "+ex.getMessage());}});}return true;}
  return false;
 }
 @Override public void onInterrupt(){live=false;}
 @Override public void onDestroy(){executor.shutdownNow();super.onDestroy();}
}