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
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
public class MainActivity extends Activity {
 private LinearLayout root;private TextView status;
 @Override public void onCreate(Bundle state){super.onCreate(state);render();}
 void title(String s){TextView t=Ui.text(this,s,18,true,Ui.WHITE);root.addView(t,Ui.lp(this,-1,40,23));}
 void info(String s){root.addView(Ui.text(this,s,13,false,Ui.MUTED),Ui.lp(this,-1,-2,7));}
 void button(String s,boolean primary,Runnable action){Button b=Ui.button(this,s,primary);root.addView(b,Ui.lp(this,-1,52,10));b.setOnClickListener(v->action.run());}
 void render(){
  ScrollView sc=new ScrollView(this);sc.setBackgroundColor(Ui.BG);root=Ui.column(this);Ui.pad(this,root,22,24);sc.addView(root);setContentView(sc);
  LinearLayout brand=Ui.row(this);
  root.addView(brand,Ui.lp(this,-1,64,0));
  android.widget.ImageView logo=new android.widget.ImageView(this);
  logo.setImageResource(R.drawable.skull_neon);
  logo.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
  brand.addView(logo,new LinearLayout.LayoutParams(Ui.dp(this,58),Ui.dp(this,58)));
  TextView label=Ui.text(this,"  NETFLIX",27,true,Ui.RED);
  brand.addView(label,new LinearLayout.LayoutParams(-1,Ui.dp(this,57)));
  root.addView(Ui.text(this,"FLOAT PANEL  /  V2.0",11,true,Ui.MUTED),Ui.lp(this,-1,25,0));
  root.addView(Ui.text(this,"Seu painel.\nSeu estilo.",30,true,Ui.WHITE),Ui.lp(this,-1,85,17));
  info("Painel nativo Android com ícone flutuante, prévia animada, ajustes salvos e diagnóstico de conexões.");
  OrbitManView preview=new OrbitManView(this);root.addView(preview,Ui.lp(this,-1,240,18));
  title("CONTROLE FLUTUANTE");
  status=Ui.text(this,"",14,true,Ui.GREEN);root.addView(status,Ui.lp(this,-1,-2,2));
  button("01  •  Autorizar sobreposição",false,()->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))));
  button("02  •  Ativar ícone flutuante",true,()->{
   if(!Settings.canDrawOverlays(this)){startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));return;}
   startForegroundService(new Intent(this,OverlayService.class));status.setText("● Serviço iniciado. Procure o ícone na tela.");
  });
  button("Desativar painel",false,()->{stopService(new Intent(this,OverlayService.class));status.setText("○ Painel encerrado.");});
  title("ATALHOS DE JOGO");
  button("Abrir Free Fire normal",false,()->open("com.dts.freefireth"));
  button("Abrir Free Fire MAX",false,()->open("com.dts.freefiremax"));
  info("O ícone pode ficar visível sobre outros aplicativos, conforme as permissões do Android.");
  title("DIAGNÓSTICO SOCKS5");
  info("Teste um servidor SOCKS5 sob seu controle. O teste não redireciona o tráfego do jogo.");
  EditText host=new EditText(this);host.setTextColor(Ui.WHITE);host.setHintTextColor(Ui.MUTED);host.setSingleLine(true);host.setHint("Host / IP");host.setText(getPreferences(0).getString("host",""));root.addView(host,Ui.lp(this,-1,50,8));
  EditText port=new EditText(this);port.setTextColor(Ui.WHITE);port.setHintTextColor(Ui.MUTED);port.setSingleLine(true);port.setInputType(2);port.setHint("Porta");port.setText(getPreferences(0).getString("port","1080"));root.addView(port,Ui.lp(this,-1,50,4));
  TextView result=Ui.text(this,"Aguardando teste.",12,false,Ui.MUTED);root.addView(result,Ui.lp(this,-1,-2,7));
  button("Testar SOCKS5",false,()->{
   String h=host.getText().toString().trim(),s=port.getText().toString().trim();int n;
   getPreferences(0).edit().putString("host",h).putString("port",s).apply();
   try{n=Integer.parseInt(s);if(n<1||n>65535)throw new Exception();}catch(Exception e){result.setText("Porta inválida.");return;}
   if(h.isEmpty()){result.setText("Informe o host.");return;}
   result.setText("Verificando...");
   new Thread(()->{String message;try(Socket sk=new Socket()){
    sk.connect(new InetSocketAddress(h,n),3000);sk.setSoTimeout(3000);
    OutputStream out=sk.getOutputStream();InputStream in=sk.getInputStream();out.write(new byte[]{5,1,0});out.flush();
    int a=in.read(),b=in.read();message=(a==5&&b==0)?"● SOCKS5 ativo: método sem autenticação disponível.":"○ Resposta recebida, sem acesso SOCKS5 anônimo.";
   }catch(Exception e){message="○ Falhou: "+e.getClass().getSimpleName();}
   String m=message;runOnUiThread(()->result.setText(m));}).start();
  });
  title("TRANSPARÊNCIA");
  info("A animação do boneco e seleção de alvo são demonstrações visuais, não leitura de jogadores reais. Não inclui aimbot, ESP em partidas, proxy do jogo ou antiban. Aplicativo não oficial da Netflix ou Garena.");
  updateStatus();
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
    requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},10);
 }
 void open(String pkg){Intent i=getPackageManager().getLaunchIntentForPackage(pkg);if(i==null){status.setText("○ Este jogo não está instalado.");return;}startActivity(i);}
 void updateStatus(){if(status!=null)status.setText(Settings.canDrawOverlays(this)?"● Sobreposição autorizada":"○ Falta autorização de sobreposição");}
 @Override protected void onResume(){super.onResume();updateStatus();}
}