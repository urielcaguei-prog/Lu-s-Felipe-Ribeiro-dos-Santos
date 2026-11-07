package hev.sockstun;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class FloatingProxyService extends Service {
 private WindowManager wm;
 private ImageView icon;
 private WindowManager.LayoutParams pos;
 private int dp(float val) {return (int)(val*getResources().getDisplayMetrics().density+.5f);}
 @Override public void onCreate(){
  super.onCreate();
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  if(Build.VERSION.SDK_INT>=26) {
   nm.createNotificationChannel(new NotificationChannel("nx_float_proxy","Ícone flutuante proxy",NotificationManager.IMPORTANCE_LOW));
  }
  Intent back=new Intent(this,MainActivity.class);
  back.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  PendingIntent pending=PendingIntent.getActivity(this,32,back,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
  Notification.Builder b=Build.VERSION.SDK_INT>=26 ? new Notification.Builder(this,"nx_float_proxy") : new Notification.Builder(this);
  b.setSmallIcon(android.R.drawable.ic_secure)
   .setContentTitle("Netflix X Proxy — flutuante ativo")
   .setContentText("Toque para abrir os ajustes de VPN").setContentIntent(pending).setOngoing(true);
  startForeground(131,b.build());
  if(!Settings.canDrawOverlays(this)){stopSelf();return;}
  wm=(WindowManager)getSystemService(Context.WINDOW_SERVICE);
  icon=new ImageView(this);
  icon.setImageResource(R.drawable.skull_neon);
  icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
  icon.setPadding(dp(3),dp(3),dp(3),dp(3));
  GradientDrawable bg=new GradientDrawable();
  bg.setColor(0xff090b12);bg.setCornerRadius(dp(40));
  bg.setStroke(dp(2),0xffe50914);icon.setBackground(bg);icon.setElevation(dp(12));
  pos=new WindowManager.LayoutParams(dp(68),dp(68),WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
    PixelFormat.TRANSLUCENT);
  pos.gravity=Gravity.TOP|Gravity.START;
  pos.x=getSharedPreferences("float_position",0).getInt("x",dp(12));
  pos.y=getSharedPreferences("float_position",0).getInt("y",dp(125));
  icon.setOnTouchListener(new View.OnTouchListener(){
   private float originX,originY;private int beginX,beginY;private boolean moved;
   @Override public boolean onTouch(View v,MotionEvent event) {
    int action=event.getAction();
    if(action==MotionEvent.ACTION_DOWN){
     originX=event.getRawX();originY=event.getRawY();beginX=pos.x;beginY=pos.y;moved=false;return true;
    }
    if(action==MotionEvent.ACTION_MOVE){
     int dx=(int)(event.getRawX()-originX),dy=(int)(event.getRawY()-originY);
     if(Math.abs(dx)+Math.abs(dy)>dp(7))moved=true;
     if(moved){pos.x=beginX+dx;pos.y=beginY+dy;try{wm.updateViewLayout(icon,pos);}catch(Exception ignored){}}
     return true;
    }
    if(action==MotionEvent.ACTION_UP){
     getSharedPreferences("float_position",0).edit().putInt("x",pos.x).putInt("y",pos.y).apply();
     if(!moved){Intent open=new Intent(FloatingProxyService.this,MainActivity.class);
      open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP);
      startActivity(open);}
     return true;
    }
    return false;
   }
  });
  try{wm.addView(icon,pos);}catch(Exception e){icon=null;stopSelf();}
 }
 @Override public int onStartCommand(Intent intent,int flags,int id){return START_NOT_STICKY;}
 @Override public void onDestroy(){if(wm!=null&&icon!=null){try{wm.removeView(icon);}catch(Exception ignored){}}super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}