package br.com.npanel;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayService extends Service {
 WindowManager wm;View current;WindowManager.LayoutParams params;SharedPreferences prefs;boolean expanded;
 @Override public void onCreate(){super.onCreate();prefs=getSharedPreferences("panel",MODE_PRIVATE);
  wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  nm.createNotificationChannel(new NotificationChannel("panel","Painel flutuante",NotificationManager.IMPORTANCE_LOW));
  Intent i=new Intent(this,MainActivity.class);i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  PendingIntent pending=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification n=new Notification.Builder(this,"panel").setSmallIcon(android.R.drawable.ic_menu_view)
   .setContentTitle("Netflix — Painel flutuante").setContentText("Toque para abrir o aplicativo")
   .setContentIntent(pending).setOngoing(true).build();
  startForeground(19,n);show(false);
 }
 @Override public int onStartCommand(Intent i,int flags,int id){return START_STICKY;}
 WindowManager.LayoutParams paramsFor(int width,int height){
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(width,height,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
   WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  p.gravity=Gravity.TOP|Gravity.START;
  p.x=prefs.getInt("x",Ui.dp(this,16));p.y=prefs.getInt("y",Ui.dp(this,160));p.alpha=prefs.getInt("alpha",100)/100f;return p;
 }
 void show(boolean panel){
  if(!Settings.canDrawOverlays(this))return;
  if(current!=null){try{wm.removeView(current);}catch(Exception ignored){}current=null;}
  expanded=panel;if(panel)showPanel();else showBubble();
 }
 void attach(View v){current=v;try{wm.addView(v,params);}catch(Exception e){current=null;stopSelf();}}
 void showBubble(){
  int size=prefs.getInt("bubble",58);
  TextView bubble=Ui.text(this,"N",36,true,Ui.RED);bubble.setGravity(Gravity.CENTER);
  bubble.setBackground(Ui.box(this,Ui.BG,Ui.RED,size));
  params=paramsFor(Ui.dp(this,size),Ui.dp(this,size));attach(bubble);
  bubble.setOnTouchListener(drag());
 }
 void showPanel(){
  LinearLayout pane=Ui.column(this);pane.setPadding(Ui.dp(this,15),Ui.dp(this,12),Ui.dp(this,15),Ui.dp(this,14));
  pane.setBackground(Ui.box(this,Ui.BG,Ui.RED,18));
  TextView h=Ui.text(this,"N   NETFLIX   •   PAINEL",19,true,Ui.RED);pane.addView(h,Ui.lp(this,-1,42,0));
  TextView info=Ui.text(this,"Painel nativo Android",12,false,Ui.WHITE);pane.addView(info,Ui.lp(this,-1,28,0));
  Button mini=Ui.button(this,"— Minimizar");pane.addView(mini,Ui.lp(this,-1,44,8));mini.setOnClickListener(v->show(false));
  pane.addView(Ui.text(this,"Opacidade",13,false,Ui.WHITE),Ui.lp(this,-1,26,12));
  SeekBar alpha=new SeekBar(this);alpha.setMax(60);alpha.setProgress(prefs.getInt("alpha",100)-40);pane.addView(alpha,Ui.lp(this,-1,34,0));
  alpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
   public void onProgressChanged(SeekBar s,int value,boolean from){
    if(!from)return;int v=value+40;prefs.edit().putInt("alpha",v).apply();params.alpha=v/100f;
    try{wm.updateViewLayout(current,params);}catch(Exception ignored){}
   }
  });
  pane.addView(Ui.text(this,"Tamanho do ícone",13,false,Ui.WHITE),Ui.lp(this,-1,26,9));
  SeekBar size=new SeekBar(this);size.setMax(48);size.setProgress(prefs.getInt("bubble",58)-40);pane.addView(size,Ui.lp(this,-1,34,0));
  size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
   public void onProgressChanged(SeekBar s,int value,boolean from){if(from)prefs.edit().putInt("bubble",value+40).apply();}
  });
  Button ff=Ui.button(this,"Abrir Free Fire");pane.addView(ff,Ui.lp(this,-1,44,12));ff.setOnClickListener(v->openGame("com.dts.freefireth"));
  Button max=Ui.button(this,"Abrir Free Fire MAX");pane.addView(max,Ui.lp(this,-1,44,8));max.setOnClickListener(v->openGame("com.dts.freefiremax"));
  pane.addView(Ui.text(this,"Ferramenta visual independente. Aimbot, ESP e mudanças no jogo não estão implementados.",12,false,0xffaaaaaa),Ui.lp(this,-1,-2,14));
  Button close=Ui.button(this,"Encerrar painel");pane.addView(close,Ui.lp(this,-1,44,13));close.setOnClickListener(v->stopSelf());
  params=paramsFor(Ui.dp(this,304),WindowManager.LayoutParams.WRAP_CONTENT);attach(pane);
  h.setOnTouchListener(drag());
 }
 void openGame(String pkg){
  Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
  if(i==null){Toast.makeText(this,"Jogo não encontrado",Toast.LENGTH_SHORT).show();return;}
  i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);startActivity(i);
 }
 View.OnTouchListener drag(){return new View.OnTouchListener(){
  float sx,sy;int x,y;boolean moved;
  public boolean onTouch(View v,MotionEvent e){
   if(e.getAction()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();x=params.x;y=params.y;moved=false;return true;}
   if(e.getAction()==MotionEvent.ACTION_MOVE){
    int dx=(int)(e.getRawX()-sx),dy=(int)(e.getRawY()-sy);
    if(Math.abs(dx)+Math.abs(dy)>Ui.dp(OverlayService.this,7))moved=true;
    if(moved){params.x=x+dx;params.y=y+dy;try{wm.updateViewLayout(current,params);}catch(Exception ignored){}}
    return true;
   }
   if(e.getAction()==MotionEvent.ACTION_UP){
    prefs.edit().putInt("x",params.x).putInt("y",params.y).apply();
    if(!moved&&!expanded)show(true);return true;
   }
   return false;
  }
 };}
 @Override public void onDestroy(){if(current!=null){try{wm.removeView(current);}catch(Exception ignored){}}current=null;super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}