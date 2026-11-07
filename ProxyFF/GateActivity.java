package hev.sockstun;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class GateActivity extends Activity {
 private final int background=Color.rgb(8,10,16);
 private final int card=Color.rgb(20,24,32);
 private final int scarlet=Color.rgb(229,9,20);
 private final int white=Color.rgb(249,249,252);
 int dp(float value) {return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 GradientDrawable shape(int fill, int stroke, int radius) {
  GradientDrawable g=new GradientDrawable();
  g.setColor(fill);g.setCornerRadius(dp(radius));
  if(stroke!=0)g.setStroke(dp(1),stroke);
  return g;
 }
 TextView text(String s,int size,boolean bold,int color) {
  TextView t=new TextView(this);
  t.setText(s);t.setTextColor(color);t.setTextSize(size);
  if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  return t;
 }
 LinearLayout.LayoutParams lp(int width,int height,int top) {
  LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(width<0?width:dp(width),height<0?height:dp(height));
  p.topMargin=dp(top);return p;
 }
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  getWindow().setStatusBarColor(background);
  getWindow().setNavigationBarColor(background);
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(background);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
  root.setPadding(dp(24),dp(34),dp(24),dp(30));root.setGravity(Gravity.CENTER_HORIZONTAL);
  sc.addView(root);setContentView(sc);

  ImageView logo=new ImageView(this);
  logo.setImageResource(R.drawable.skull_neon);
  logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
  LinearLayout.LayoutParams logoP=lp(136,136,10);logoP.gravity=Gravity.CENTER_HORIZONTAL;
  root.addView(logo,logoP);
  TextView name=text("NETFLIX X PROXY",27,true,white);
  name.setGravity(Gravity.CENTER);
  root.addView(name,lp(-1,52,12));
  TextView sub=text("SOCKS5  •  TUNNEL  •  ANDROID",12,true,scarlet);
  sub.setGravity(Gravity.CENTER);
  root.addView(sub,lp(-1,34,0));

  LinearLayout form=new LinearLayout(this);
  form.setPadding(dp(19),dp(22),dp(19),dp(23));
  form.setOrientation(LinearLayout.VERTICAL);
  form.setBackground(shape(card,Color.rgb(82,35,47),18));
  root.addView(form,lp(-1,-2,26));
  form.addView(text("CHAVE DE ACESSO",13,true,white),lp(-1,26,0));
  TextView hint=text("Digite sua chave para abrir os controles reais da VPN.",13,false,Color.rgb(179,184,195));
  form.addView(hint,lp(-1,-2,5));
  EditText key=new EditText(this);
  key.setSingleLine(true);
  key.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
  key.setHint("Sua chave");key.setHintTextColor(Color.rgb(145,150,161));
  key.setTextColor(white);key.setTextSize(17);
  key.setPadding(dp(13),0,dp(13),0);
  key.setBackground(shape(Color.rgb(33,37,47),Color.rgb(65,68,81),12));
  form.addView(key,lp(-1,55,18));
  Button enter=new Button(this);enter.setAllCaps(false);enter.setText("ENTRAR NO PAINEL");
  enter.setTextSize(14);enter.setTypeface(Typeface.DEFAULT,Typeface.BOLD);enter.setTextColor(white);
  enter.setBackground(shape(scarlet,0,12));form.addView(enter,lp(-1,52,17));
  enter.setOnClickListener(v->{
   if("luisfreefire".equals(key.getText().toString().trim())) {
    getSharedPreferences("nx_gate",0).edit().putString("access","luisfreefire").apply();
    startActivity(new Intent(this, MainActivity.class));finish();
   } else {
    key.setError("Chave incorreta");Toast.makeText(this,"Acesso não autorizado",Toast.LENGTH_SHORT).show();
   }
  });

  TextView facts=text("PROXY DE REDE REAL  •  SERVIDOR SOCKS5 NECESSÁRIO",11,true,Color.rgb(111,231,167));
  facts.setGravity(Gravity.CENTER);
  root.addView(facts,lp(-1,43,20));
  TextView explain=text("Este aplicativo encaminha tráfego de rede por uma VPN local com servidor SOCKS5 que você configurar. Não dá capa, não mostra inimigos nem modifica a memória do Free Fire.",12,false,Color.rgb(166,171,183));
  explain.setGravity(Gravity.CENTER);root.addView(explain,lp(-1,-2,5));
  TextView credit=text("Base de tunelamento: SocksTun/HevSocks5Tunnel (licença MIT). Aplicativo não oficial da Netflix ou Garena.",11,false,Color.rgb(126,132,144));
  credit.setGravity(Gravity.CENTER);root.addView(credit,lp(-1,-2,22));
 }
}