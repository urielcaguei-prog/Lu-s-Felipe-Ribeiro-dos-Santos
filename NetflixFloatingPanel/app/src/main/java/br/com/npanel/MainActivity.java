package br.com.npanel;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
 private TextView status;
 @Override public void onCreate(Bundle state){super.onCreate(state);build();}
 private void build(){
  LinearLayout root=Ui.column(this);root.setPadding(Ui.dp(this,22),Ui.dp(this,34),Ui.dp(this,22),Ui.dp(this,20));root.setBackgroundColor(Ui.BG);
  ScrollView sc=new ScrollView(this);sc.addView(root);setContentView(sc);
  TextView logo=Ui.text(this,"N   NETFLIX",31,true,Ui.RED);logo.setGravity(Gravity.CENTER);root.addView(logo,Ui.lp(this,-1,64,3));
  TextView sub=Ui.text(this,"PAINEL FLUTUANTE  •  ANDROID",12,true,Ui.WHITE);sub.setGravity(Gravity.CENTER);root.addView(sub,Ui.lp(this,-1,30,0));
  TextView explain=Ui.text(this,"Visual personalizado em preto e vermelho. O ícone flutuante abre um painel real sobre outros aplicativos. Não altera partidas de Free Fire.",14,false,0xffcccccc);
  root.addView(explain,Ui.lp(this,-1,-2,20));
  status=Ui.text(this,"",14,false,Ui.WHITE);root.addView(status,Ui.lp(this,-1,-2,20));
  Button permission=Ui.button(this,"1. Autorizar janela flutuante");root.addView(permission,Ui.lp(this,-1,54,14));
  permission.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
  Button start=Ui.button(this,"2. Ativar ícone flutuante");root.addView(start,Ui.lp(this,-1,54,12));
  start.setOnClickListener(v->{
   if(!Settings.canDrawOverlays(this)){permission.performClick();return;}
   Intent i=new Intent(this,OverlayService.class);startForegroundService(i);updateStatus();
  });
  Button stop=Ui.button(this,"Desativar painel");root.addView(stop,Ui.lp(this,-1,50,12));
  stop.setOnClickListener(v->{stopService(new Intent(this,OverlayService.class));updateStatus();});
  Button normal=Ui.button(this,"Abrir Free Fire normal");root.addView(normal,Ui.lp(this,-1,52,25));
  normal.setOnClickListener(v->openGame("com.dts.freefireth"));
  Button max=Ui.button(this,"Abrir Free Fire MAX");root.addView(max,Ui.lp(this,-1,52,12));
  max.setOnClickListener(v->openGame("com.dts.freefiremax"));
  root.addView(Ui.text(this,"Sem chave de acesso. Inclui painel arrastável, minimizar, opacidade, tamanho do ícone e abertura de jogos. Não oferece aimbot, ESP ou antiban.",13,false,0xffaaaaaa),Ui.lp(this,-1,-2,22));
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
   requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
  }
 }
 private void openGame(String pkg){Intent i=getPackageManager().getLaunchIntentForPackage(pkg);
  if(i==null){status.setText("Aplicativo não encontrado: "+pkg);return;}startActivity(i);}
 private void updateStatus(){status.setText(Settings.canDrawOverlays(this)?"● Permissão de flutuação autorizada":"○ Falta autorização para janela flutuante");}
 @Override protected void onResume(){super.onResume();if(status!=null)updateStatus();}
}