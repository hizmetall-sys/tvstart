package com.hizmetall.tvstart;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.Color;
import android.net.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private TextView status; private Switch daily, auto; private Button time, choose; private final Handler h=new Handler(Looper.getMainLooper());
    private final Runnable refreshLoop=new Runnable(){public void run(){refresh();h.postDelayed(this,5000);}};
    int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    TextView label(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.WHITE);t.setPadding(0,dp(6),0,dp(6));return t;}
    Button btn(String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setOnClickListener(l);return b;}
    @Override public void onCreate(Bundle b){super.onCreate(b);ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(0xff101b2b);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(40),dp(24),dp(40),dp(24));sc.addView(root);setContentView(sc);
        root.addView(label("TVStart  •  1.4",32));root.addView(label("Akıllı TV bakım ve açılış yönetimi",17));status=label("",17);root.addView(status);
        choose=btn("Açılış uygulamasını seç",v->pick());root.addView(choose);
        auto=new Switch(this);auto.setText("İnternet hazır olduğunda açılış uygulamasını çalıştır");auto.setTextColor(Color.WHITE);auto.setPadding(0,dp(8),0,dp(8));auto.setChecked(Engine.prefs(this).getBoolean("auto",false));auto.setOnCheckedChangeListener((v,on)->{Engine.prefs(this).edit().putBoolean("auto",on).apply();refresh();});root.addView(auto);
        daily=new Switch(this);daily.setText("Günlük otomatik yeniden başlatma");daily.setTextColor(Color.WHITE);daily.setPadding(0,dp(8),0,dp(8));daily.setChecked(Engine.prefs(this).getBoolean("daily",false));daily.setOnCheckedChangeListener((v,on)->{Engine.prefs(this).edit().putBoolean("daily",on).apply();Engine.schedule(this);refresh();});root.addView(daily);
        time=btn("",v->{new TimePickerDialog(this,(view,hr,min)->{Engine.prefs(this).edit().putInt("hour",hr).putInt("minute",min).apply();Engine.schedule(this);refresh();},Engine.prefs(this).getInt("hour",4),Engine.prefs(this).getInt("minute",0),true).show();});root.addView(time);
        root.addView(btn("Seçili uygulamayı şimdi aç",v->{if(!Engine.open(this))toast("Uygulama açılamadı");refresh();}));
        root.addView(btn("Yerel ADB bağlantısını test et",v->{v.setEnabled(false);toast("ADB bağlantısı deneniyor. İzin sorulursa her zaman izin ver seçeneğini işaretleyin.");new Thread(()->{boolean ok=false;String msg;try{ok=LocalAdb.test(this);msg=ok?"Yerel ADB bağlantısı başarılı":"ADB testi başarısız";}catch(Exception e){msg="ADB: "+e.getMessage();}Engine.prefs(this).edit().putBoolean("adb",ok).apply();Engine.log(this,msg);Engine.schedule(this);final String result=msg;h.post(()->{v.setEnabled(true);toast(result);refresh();});}).start();}));
        root.addView(btn("Şimdi yeniden başlat",v->{if(!Engine.ready(this)){toast("Önce yerel ADB bağlantısını test edin.");return;}new AlertDialog.Builder(this).setTitle("Cihaz yeniden başlatılsın mı?").setNegativeButton("Vazgeç",null).setPositiveButton("Yeniden başlat",(d,w)->{new Thread(()->{boolean ok=Engine.reboot(this);h.post(()->{if(!ok)toast("İşlem başarısız; son kaydı kontrol edin");refresh();});}).start();}).show();}));
        root.addView(label("Akıllı açılış: internet doğrulanana kadar bekler (en fazla yaklaşık 5 dakika). TV+ profil ve kanal seçimi henüz otomatik değil.",15));
        root.addView(btn("Yetki ve kurulum bilgisi",v->new AlertDialog.Builder(this).setTitle("Yeniden başlatma yetkisi").setMessage("Geliştirici seçeneklerinde ağ ADB etkin olmalı (5555). Yerel ADB testini çalıştırın ve Android izin penceresini onaylayın. Cihazı sıfırlamayın. ADB portunu internete açmayın.").setPositiveButton("Tamam",null).show()));
        Engine.schedule(this);refresh();choose.requestFocus();
    }
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onResume(){super.onResume();h.removeCallbacks(refreshLoop);h.post(refreshLoop);}
    @Override protected void onPause(){h.removeCallbacks(refreshLoop);super.onPause();}
    private void refresh(){if(status==null)return;ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();getSystemService(ActivityManager.class).getMemoryInfo(m);long used=m.totalMem>0?100*(m.totalMem-m.availMem)/m.totalMem:0;
        ConnectivityManager cm=getSystemService(ConnectivityManager.class);Network n=cm.getActiveNetwork();NetworkCapabilities cap=n==null?null:cm.getNetworkCapabilities(n);String net=cap==null?"Bağlantı yok":(cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":"Diğer ağ")+(cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)?" • internet doğrulandı":" • internet doğrulanmadı");
        long next=Engine.prefs(this).getLong("next",0);String planned=next==0?"Plan yok / yetki yok":new java.text.SimpleDateFormat("dd.MM HH:mm",Locale.getDefault()).format(new Date(next));
        status.setText("RAM: %"+used+" kullanım • "+(m.availMem/1048576)+" MB kullanılabilir\nAğ: "+net+"\nÇalışma süresi: "+(SystemClock.elapsedRealtime()/3600000)+" saat\nYetki: "+(Engine.owner(this)?"Cihaz sahibi etkin":(Engine.ready(this)?"Yerel ADB onaylandı":"Yerel ADB testi gerekli"))+"\nSonraki bakım (yaklaşık): "+planned+"\nSon kayıt: "+Engine.prefs(this).getString("last","Henüz yok"));
        time.setText(String.format(Locale.ROOT,"Bakım saati: %02d:%02d",Engine.prefs(this).getInt("hour",4),Engine.prefs(this).getInt("minute",0)));String pkg=Engine.prefs(this).getString("package","");choose.setText(pkg.isEmpty()?"Açılış uygulamasını seç":"Açılış: "+Engine.prefs(this).getString("label",pkg));
    }
    private void pick(){PackageManager pm=getPackageManager();LinkedHashMap<String,String> apps=new LinkedHashMap<>();for(String category:new String[]{Intent.CATEGORY_LEANBACK_LAUNCHER,Intent.CATEGORY_LAUNCHER}){for(ResolveInfo ri:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(category),0)){String p=ri.activityInfo.packageName;if(!p.equals(getPackageName()))apps.put(p,ri.loadLabel(pm).toString());}}
        ArrayList<String> keys=new ArrayList<>(apps.keySet());keys.sort((a,b)->apps.get(a).compareToIgnoreCase(apps.get(b)));String[] titles=new String[keys.size()];for(int i=0;i<keys.size();i++)titles[i]=apps.get(keys.get(i))+"  ("+keys.get(i)+")";if(keys.isEmpty()){toast("Başlatılabilir uygulama bulunamadı");return;}
        new AlertDialog.Builder(this).setTitle("TV+ veya başka uygulama seçin").setItems(titles,(d,w)->{String p=keys.get(w);Engine.prefs(this).edit().putString("package",p).putString("label",apps.get(p)).apply();refresh();}).setNegativeButton("İptal",null).show();}
}