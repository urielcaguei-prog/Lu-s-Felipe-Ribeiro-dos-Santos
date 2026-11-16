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
 private final Handler handler=new Handler(Looper.getMainLooper());
 private boolean working=false; private boolean attached=false;
 private final Runnable timeout=()->{
   if(working){message("Análise expirou. Se houve resultado atrasado, ele não deve aplicar gesto.");restore();}
 };
 @Override public void onCreate(){
  super.onCreate();
  String id="uriel_controls";
  NotificationChannel channel=new NotificationChannel(id,"Uriel Chat controles",NotificationManager.IMPORTANCE_LOW);
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(channel);
  Intent stop=new Intent(this,OverlayService.class);stop.setAction("STOP");
  PendingIntent pi=PendingIntent.getService(this,1,stop,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification note=new Notification.Builder(this,id).setSmallIcon(android.R.drawable.ic_media_play)
    .setContentTitle("Uriel Chat").setContentText("Análise de duas capturas ao tocar no botão")
    .addAction(android.R.drawable.ic_delete,"Parar",pi).build();
  startForeground(11,note);
  wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  bubble=new Button(this);
  bubble.setText("◎");bubble.setTextColor(Color.WHITE);bubble.setTextSize(22);
  bubble.setContentDescription("Tentar correção e disparo para um alvo visualmente parado");
  bubble.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff10a37f));
  lp=new WindowManager.LayoutParams(108,108,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
     WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,PixelFormat.TRANSLUCENT);
  lp.gravity=Gravity.TOP|Gravity.LEFT;
  android.graphics.Point p=new android.graphics.Point();wm.getDefaultDisplay().getRealSize(p);
  lp.x=(int)(p.x*.76);lp.y=(int)(p.y*.60);
  try{wm.addView(bubble,lp);attached=true;}catch(Exception e){stopSelf();return;}
  bubble.setOnClickListener(v->analyzeOnce());
 }
 private void analyzeOnce(){
  if(working)return;
  if(GestureService.instance==null||!MainActivity.licensed){
    message("Valide uma licença e ative o serviço de acessibilidade.");return;
  }
  working=true;
  try{if(attached){wm.removeView(bubble);attached=false;}}catch(Exception e){working=false;return;}
  handler.postDelayed(timeout,9200);
  handler.postDelayed(()->{
    if(GestureService.instance==null){message("Acessibilidade desconectada.");restore();return;}
    GestureService.instance.detectStationaryAndShoot(msg->
       handler.post(()->{message(msg);restore();})
    );
  },250);
 }
 private void restore(){
  if(!working)return;
  working=false;handler.removeCallbacks(timeout);
  if(!attached)try{wm.addView(bubble,lp);attached=true;}catch(Exception ignored){}
 }
 private void message(String value){Toast.makeText(this,value,Toast.LENGTH_LONG).show();}
 @Override public int onStartCommand(Intent i,int f,int id){
  if(i!=null&&"STOP".equals(i.getAction())){stopSelf();return START_NOT_STICKY;}
  return START_NOT_STICKY;
 }
 @Override public void onDestroy(){
  handler.removeCallbacks(timeout);
  try{if(attached)wm.removeView(bubble);}catch(Exception ignored){}
  super.onDestroy();
 }
 @Override public IBinder onBind(Intent i){return null;}
}
