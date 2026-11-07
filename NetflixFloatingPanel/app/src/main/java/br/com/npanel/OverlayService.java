package br.com.npanel;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class OverlayService extends Service {
 private WindowManager wm;private View current;private WindowManager.LayoutParams params;
 private SharedPreferences prefs;private boolean expanded=false;private String tab="PRÉVIA";
 @Override public void onCreate(){super.onCreate();prefs=getSharedPreferences("panel",MODE_PRIVATE);wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
  nm.createNotificationChannel(new NotificationChannel("panel","Painel flutuante",NotificationManager.IMPORTANCE_LOW));
  Intent open=new Intent(this,MainActivity.class);open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  PendingIntent pi=PendingIntent.getActivity(this,0,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
  Notification note=new Notification.Builder(this,"panel").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Netflix • Painel flutuante")
  .setContentText("Painel ativo. Toque para abrir os controles.").setContentIntent(pi).setOngoing(true).build();
  startForeground(19,note);show(false);
 }
 @Override public int onStartCommand(Intent i,int flags,int id){return START_STICKY;}
 private WindowManager.LayoutParams placement(int w,int h){
  WindowManager.LayoutParams p=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
  p.gravity=Gravity.TOP|Gravity.START;p.x=prefs.getInt("x",Ui.dp(this,15));p.y=prefs.getInt("y",Ui.dp(this,130));
  p.alpha=Math.max(.4f,prefs.getInt("alpha",100)/100f);return p;
 }
 private void attach(View v){current=v;try{wm.addView(v,params);}catch(Exception ex){current=null;Toast.makeText(this,"Permissão de janela necessária",Toast.LENGTH_SHORT).show();stopSelf();}}
 private void show(boolean panel){
  if(!Settings.canDrawOverlays(this)){stopSelf();return;}
  if(current!=null){try{wm.removeView(current);}catch(Exception ignored){}current=null;}
  expanded=panel;if(panel)buildPanel();else buildBubble();
 }
 private void buildBubble(){
  int size=prefs.getInt("bubble",58);
  TextView b=Ui.text(this,"N",36,true,Ui.RED);b.setGravity(Gravity.CENTER);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  b.setBackground(Ui.box(this,Ui.BG,Ui.RED,26));b.setElevation(Ui.dp(this,10));
  params=placement(Ui.dp(this,size),Ui.dp(this,size));attach(b);b.setOnTouchListener(drag());
 }
 private TextView note(String s,int sz,int color){return Ui.text(this,s,sz,false,color);}
 private void line(LinearLayout root){View v=new View(this);v.setBackgroundColor(0xff303139);root.addView(v,Ui.lp(this,-1,1,12));}
 private void button(LinearLayout root,String label,boolean primary,Runnable click){
  Button b=Ui.button(this,label,primary);root.addView(b,Ui.lp(this,-1,43,9));b.setOnClickListener(v->click.run());
 }
 private void header(LinearLayout root){
  LinearLayout bar=Ui.row(this);root.addView(bar,Ui.lp(this,-1,46,0));
  TextView icon=Ui.text(this,"N",30,true,Ui.RED);bar.addView(icon,new LinearLayout.LayoutParams(Ui.dp(this,40),-1));
  TextView title=Ui.text(this,"NETFLIX  /  FLOAT PANEL",15,true,Ui.WHITE);
  bar.addView(title,new LinearLayout.LayoutParams(0,-1,1));
  Button mini=Ui.button(this,"—",false);bar.addView(mini,new LinearLayout.LayoutParams(Ui.dp(this,45),Ui.dp(this,38)));
  mini.setOnClickListener(v->show(false));
  title.setOnTouchListener(drag());icon.setOnTouchListener(drag());
  root.addView(note("PERSONALIZE • ARRASTE PELO TÍTULO",10,Ui.MUTED),Ui.lp(this,-1,21,0));
  line(root);
 }
 private void tabs(LinearLayout root){
  LinearLayout row=Ui.row(this);root.addView(row,Ui.lp(this,-1,43,10));
  for(String item:new String[]{"PRÉVIA","AJUSTES","STATUS"}){
   Button b=Ui.button(this,item,item.equals(tab));b.setTextSize(10);
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,38),1);p.setMargins(Ui.dp(this,2),0,Ui.dp(this,2),0);
   row.addView(b,p);b.setOnClickListener(v->{tab=item;show(true);});
  }
 }
 private void preview(LinearLayout root){
  root.addView(note("VISUALIZAÇÃO 360°  •  MODELO ILUSTRATIVO",10,Ui.MUTED),Ui.lp(this,-1,24,10));
  OrbitManView model=new OrbitManView(this);root.addView(model,Ui.lp(this,-1,217,6));
  root.addView(note("POSIÇÃO DESTACADA NA PRÉVIA",11,Ui.MUTED),Ui.lp(this,-1,25,10));
  LinearLayout row=Ui.row(this);root.addView(row,Ui.lp(this,-1,42,2));
  for(String name:new String[]{"CABEÇA","PESCOÇO","PEITO"}){
   Button b=Ui.button(this,name,name.equals(prefs.getString("target","CABEÇA")));b.setTextSize(10);
   LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,Ui.dp(this,39),1);p.setMargins(Ui.dp(this,2),0,Ui.dp(this,2),0);
   row.addView(b,p);
   b.setOnClickListener(v->{prefs.edit().putString("target",name).apply();show(true);});
  }
  root.addView(note("Os marcadores são apenas elementos da animação. Não apontam para jogadores.",11,Ui.MUTED),Ui.lp(this,-1,-2,9));
 }
 private void slider(LinearLayout root,String label,int progress,int max,java.util.function.IntConsumer action){
  root.addView(note(label,13,Ui.WHITE),Ui.lp(this,-1,26,12));
  SeekBar seek=new SeekBar(this);seek.setMax(max);seek.setProgress(progress);root.addView(seek,Ui.lp(this,-1,34,0));
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}
   public void onProgressChanged(SeekBar s,int v,boolean user){if(user)action.accept(v);}
  });
 }
 private void settings(LinearLayout root){
  root.addView(note("CONTROLES REAIS DO APLICATIVO",11,Ui.GREEN),Ui.lp(this,-1,30,12));
  slider(root,"Opacidade do painel",Math.max(0,prefs.getInt("alpha",100)-40),60,v->{
   int value=v+40;prefs.edit().putInt("alpha",value).apply();params.alpha=value/100f;
   try{wm.updateViewLayout(current,params);}catch(Exception ignored){}
  });
  slider(root,"Tamanho do ícone flutuante",Math.max(0,prefs.getInt("bubble",58)-40),48,v->prefs.edit().putInt("bubble",v+40).apply());
  slider(root,"Velocidade de rotação do boneco",Math.max(0,prefs.getInt("spin",65)-15),135,v->prefs.edit().putInt("spin",v+15).apply());
  Switch rotate=new Switch(this);rotate.setText("Animação 360°");rotate.setTextColor(Ui.WHITE);rotate.setTextSize(13);
  rotate.setChecked(prefs.getBoolean("rotate",true));root.addView(rotate,Ui.lp(this,-1,56,6));
  rotate.setOnCheckedChangeListener((b,on)->prefs.edit().putBoolean("rotate",on).apply());
  button(root,"Restaurar tamanho, posição e aparência",false,()->{prefs.edit().remove("x").remove("y").remove("alpha").remove("bubble").remove("spin").remove("target").putBoolean("rotate",true).apply();show(true);});
 }
 private boolean installed(String pkg){return getPackageManager().getLaunchIntentForPackage(pkg)!=null;}
 private void status(LinearLayout root){
  root.addView(note("DIAGNÓSTICO DO SISTEMA",12,Ui.GREEN),Ui.lp(this,-1,35,9));
  root.addView(note("● Serviço flutuante: ativo",13,Ui.WHITE),Ui.lp(this,-1,30,2));
  root.addView(note((Settings.canDrawOverlays(this)?"●":"○")+" Sobreposição Android: "+(Settings.canDrawOverlays(this)?"permitida":"negada"),13,Ui.WHITE),Ui.lp(this,-1,32,0));
  root.addView(note((installed("com.dts.freefireth")?"●":"○")+" Free Fire normal: "+(installed("com.dts.freefireth")?"instalado":"não encontrado"),13,Ui.WHITE),Ui.lp(this,-1,32,0));
  root.addView(note((installed("com.dts.freefiremax")?"●":"○")+" Free Fire MAX: "+(installed("com.dts.freefiremax")?"instalado":"não encontrado"),13,Ui.WHITE),Ui.lp(this,-1,32,0));
  line(root);
  root.addView(note("PREVISUALIZAÇÃO: FUNCIONAL",12,Ui.GREEN),Ui.lp(this,-1,32,9));
  root.addView(note("Aimbot / ESP em partida: não implementados.",12,Ui.MUTED),Ui.lp(this,-1,-2,2));
  root.addView(note("Proxy do jogo / proteção antiban: não implementados.",12,Ui.MUTED),Ui.lp(this,-1,-2,8));
  button(root,"Atualizar verificação",false,()->show(true));
 }
 private void launch(String pkg){
  Intent open=getPackageManager().getLaunchIntentForPackage(pkg);
  if(open==null){Toast.makeText(this,"Jogo não instalado",Toast.LENGTH_SHORT).show();return;}
  open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
  try{startActivity(open);show(false);}catch(Exception e){Toast.makeText(this,"Não foi possível iniciar o jogo",Toast.LENGTH_SHORT).show();}
 }
 private void buildPanel(){
  DisplayMetrics m=new DisplayMetrics();wm.getDefaultDisplay().getMetrics(m);
  int maxW=Math.min(Ui.dp(this,350),m.widthPixels-Ui.dp(this,16));
  int maxH=Math.min(Ui.dp(this,655),m.heightPixels-Ui.dp(this,65));
  LinearLayout root=Ui.column(this);Ui.pad(this,root,13,12);
  root.setBackground(Ui.box(this,Ui.BG,Ui.RED,17));root.setElevation(Ui.dp(this,12));
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);
  scroll.addView(root);
  header(root);tabs(root);
  if(tab.equals("PRÉVIA"))preview(root);
  else if(tab.equals("AJUSTES"))settings(root);
  else status(root);
  line(root);
  root.addView(note("ATALHOS / JOGOS INSTALADOS",10,Ui.MUTED),Ui.lp(this,-1,24,8));
  button(root,"INICIAR FREE FIRE",true,()->launch("com.dts.freefireth"));
  button(root,"INICIAR FREE FIRE MAX",false,()->launch("com.dts.freefiremax"));
  button(root,"Encerrar painel",false,()->stopSelf());
  root.addView(note("Interface independente. Não oficial da Netflix nem da Garena.",10,Ui.MUTED),Ui.lp(this,-1,-2,13));
  params=placement(maxW,Math.max(Ui.dp(this,170),maxH));
  attach(scroll);
 }
 private View.OnTouchListener drag(){return new View.OnTouchListener(){
  float startX,startY;int oldX,oldY;boolean moved;
  @Override public boolean onTouch(View v,MotionEvent e){
   if(e.getAction()==MotionEvent.ACTION_DOWN){startX=e.getRawX();startY=e.getRawY();oldX=params.x;oldY=params.y;moved=false;return true;}
   if(e.getAction()==MotionEvent.ACTION_MOVE){
    int dx=(int)(e.getRawX()-startX),dy=(int)(e.getRawY()-startY);
    if(Math.abs(dx)+Math.abs(dy)>Ui.dp(OverlayService.this,7))moved=true;
    if(moved){params.x=oldX+dx;params.y=oldY+dy;try{wm.updateViewLayout(current,params);}catch(Exception ignored){}}
    return true;
   }
   if(e.getAction()==MotionEvent.ACTION_UP){
    prefs.edit().putInt("x",params.x).putInt("y",params.y).apply();
    if(!moved&&!expanded)show(true);
    return true;
   }
   return false;
  }
 };}
 @Override public void onDestroy(){if(current!=null){try{wm.removeView(current);}catch(Exception ignored){}}current=null;super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}