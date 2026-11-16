package br.com.urielchat;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.widget.*;
public class OverlayService extends Service {
 private WindowManager wm; private Button bubble; private WindowManager.LayoutParams lp;
 private Handler handler=new Handler(Looper.getMainLooper());
 private boolean working=false;
 @Override public void onCreate(){
  super.onCreate();
  String id="uriel_controls";
  NotificationChannel channel=new NotificationChannel(id,"Uriel Chat controles",NotificationManager.IMPORTANCE_LOW);
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
  Intent stop=new Intent(this,OverlayService.class);stop.setAction("STOP");
  PendingIntent pi=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification note=new Notification.Builder(this,id).setSmallIcon(android.R.drawable.ic_media_play)
   .setContentTitle("Uriel Chat").setContentText("Toque no controle flutuante para um gesto vertical")
   .addAction(android.R.drawable.ic_delete,"Parar",pi).build();
  startForeground(11,note);
  wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  bubble=new Button(this);
  bubble.setText("↑");bubble.setTextColor(Color.WHITE);bubble.setTextSize(21);
  bubble.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff10a37f));
  lp=new WindowManager.LayoutParams(100,100,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
  lp.gravity=Gravity.TOP|Gravity.LEFT;
  android.graphics.Point p=new android.graphics.Point();wm.getDefaultDisplay().getRealSize(p);
  lp.x=(int)(p.x*.78);lp.y=(int)(p.y*.62);
  try{wm.addView(bubble,lp);}catch(Exception e){stopSelf();return;}
  bubble.setOnClickListener(v->fire());
 }
 private void fire(){
  if(working||GestureService.instance==null||!MainActivity.licensed)return;
  working=true;
  int x=lp.x+50,y=lp.y+50;
  try{wm.removeView(bubble);}catch(Exception e){working=false;return;}
  handler.postDelayed(()->{
   if(GestureService.instance==null){restore();return;}
   int delta=getSharedPreferences("settings",0).getInt("delta",55);
   GestureService.instance.perform(x,y,delta,()->handler.postDelayed(this::restore,180));
  },80);
 }
 private void restore(){if(!working)return;try{wm.addView(bubble,lp);}catch(Exception ignored){}working=false;}
 @Override public int onStartCommand(Intent i,int flags,int startId){
  if(i!=null&&"STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  return START_NOT_STICKY;
 }
 @Override public void onDestroy(){try{wm.removeView(bubble);}catch(Exception ignored){}super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
