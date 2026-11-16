package br.com.urielchat;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MainActivity extends Activity {
 public static volatile boolean licensed = false;
 private static final String APP = "Urielcaguei's Application";
 private static final String OWNER = "1A59vpRXen";
 private static final String API = "https://keyauth.win/api/1.3/";
 private TextView status;
 private EditText license;
 private Button enter;
 private LinearLayout content;
 private int white=Color.WHITE, green=0xff10a37f;
 @Override public void onCreate(Bundle b) {
  super.onCreate(b);
  getWindow().setStatusBarColor(0xff202123);
  getWindow().setNavigationBarColor(0xff171717);
  ScrollView scroll = new ScrollView(this);
  content = new LinearLayout(this);
  content.setBackgroundColor(0xff202123);
  content.setPadding(25,25,25,30);
  content.setOrientation(LinearLayout.VERTICAL);
  scroll.addView(content); setContentView(scroll);
  text("Uriel Chat",25,green);
  text("Aviso do administrador: digite sua chave de licença para liberar os controles. Não digite a senha do Google.",15,white);
  text("Aplicativo independente. Um gesto vertical manual não identifica cabeças e não é aimbot inteligente.",13,white);
  license = new EditText(this);
  license.setTextColor(white); license.setHintTextColor(0xffbbbbbb);
  license.setSingleLine(true); license.setHint("Licença KeyAuth");
  license.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  content.addView(license);
  enter=button("Validar licença KeyAuth");
  enter.setOnClickListener(v->login());
  status=text("Sem licença verificada",14,white);
  button("Abrir configurações de acessibilidade").setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
  button("Autorizar sobreposição").setOnClickListener(v->startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName()))));
  text("Puxada para cima, em pixels (gesto de teste manual)",14,white);
  SeekBar bar=new SeekBar(this); bar.setMax(220);
  bar.setProgress(getPreferences().getInt("delta",55));
  bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
    public void onProgressChanged(SeekBar b,int p,boolean u){getPreferences().edit().putInt("delta",p).apply();}
    public void onStartTrackingTouch(SeekBar b){}
    public void onStopTrackingTouch(SeekBar b){}
  });
  content.addView(bar);
  button("Iniciar botão flutuante").setOnClickListener(v->{
    if(!licensed){status.setText("Valide a licença primeiro.");return;}
    if(GestureService.instance==null){status.setText("Habilite a acessibilidade do Uriel Chat.");return;}
    if(!Settings.canDrawOverlays(this)){status.setText("Autorize sobreposição de tela.");return;}
    startForegroundService(new Intent(this,OverlayService.class));
    status.setText("Botão flutuante iniciado. Gesto único manual; não detecta inimigos.");
  });
  button("Parar botão flutuante").setOnClickListener(v->{stopService(new Intent(this,OverlayService.class)); status.setText("Controle parado.");});
  text("Esta versão implementa login e gesto via acessibilidade. ESP, Chams e detecção da cabeça NÃO estão implementados.",13,0xffffcc88);
 }
 private android.content.SharedPreferences getPreferences(){return getSharedPreferences("settings",0);}
 private TextView text(String s,int size,int color){
  TextView t=new TextView(this); t.setText(s); t.setTextColor(color);t.setTextSize(size);
  t.setPadding(0,12,0,12);content.addView(t);return t;
 }
 private Button button(String s){
  Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(white);
  b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(green));
  content.addView(b);return b;
 }
 private void login(){
  String key=license.getText().toString().trim();
  if(key.isEmpty()){status.setText("Digite uma licença.");return;}
  license.setText("");
  enter.setEnabled(false);status.setText("Conectando ao KeyAuth...");
  new Thread(()->{
   String result;
   try {
     JSONObject init=post("type=init&name="+URLEncoder.encode(APP,"UTF-8")+"&ownerid="+OWNER+"&ver=1.0&hash=undefined&token=undefined&thash=undefined");
     if(!init.optBoolean("success",false)) throw new IOException(init.optString("message","Falha na inicialização"));
     String session=init.optString("sessionid");
     if(session.isEmpty())throw new IOException("Sessão não recebida");
     String androidId=Settings.Secure.getString(getContentResolver(),Settings.Secure.ANDROID_ID);
     if(androidId==null)androidId="no-id";
     byte[] bs=MessageDigest.getInstance("SHA-256").digest(androidId.getBytes(StandardCharsets.UTF_8));
     StringBuilder hwid=new StringBuilder();
     for(byte x:bs)hwid.append(String.format("%02x",x&255));
     JSONObject auth=post("type=license&name="+URLEncoder.encode(APP,"UTF-8")+"&ownerid="+OWNER+
         "&sessionid="+URLEncoder.encode(session,"UTF-8")+"&hwid="+hwid+"&key="+URLEncoder.encode(key,"UTF-8")+"&code=");
     licensed=auth.optBoolean("success",false);
     result=licensed?"Licença validada pelo servidor KeyAuth.":"Acesso negado: "+auth.optString("message","Licença inválida");
   } catch(Exception ex){licensed=false;result="Erro KeyAuth: "+ex.getMessage();}
   final String msg=result;
   runOnUiThread(()->{enter.setEnabled(true);status.setText(msg);});
  }).start();
 }
 private static JSONObject post(String params)throws Exception{
  HttpURLConnection c=(HttpURLConnection)new URL(API).openConnection();
  c.setRequestMethod("POST");c.setDoOutput(true);c.setConnectTimeout(12000);c.setReadTimeout(12000);
  c.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset=UTF-8");
  byte[] b=params.getBytes(StandardCharsets.UTF_8);
  try(OutputStream out=c.getOutputStream()){out.write(b);}
  InputStream stream=c.getResponseCode()<400?c.getInputStream():c.getErrorStream();
  if(stream==null)throw new IOException("Sem resposta");
  try(BufferedReader reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
   StringBuilder v=new StringBuilder();String line;
   while((line=reader.readLine())!=null){if(v.length()>32768)throw new IOException("Resposta excessiva");v.append(line);}
   return new JSONObject(v.toString());
  } finally {c.disconnect();}
 }
}
