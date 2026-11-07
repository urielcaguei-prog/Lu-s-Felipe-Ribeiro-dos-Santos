package br.com.npanel;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
final class Ui {
 static final int BG=0xff08090c,CARD=0xff171a20,RED=0xffe50914,WHITE=0xfff7f8fa,MUTED=0xffaab0bd,GREEN=0xff58d88a;
 static int dp(Context c,float v){return (int)(v*c.getResources().getDisplayMetrics().density+0.5f);}
 static GradientDrawable box(Context c,int color,int stroke,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(c,radius));if(stroke!=0)g.setStroke(dp(c,1),stroke);return g;}
 static TextView text(Context c,String s,int size,boolean bold,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 static Button button(Context c,String s,boolean primary){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(WHITE);b.setBackground(box(c,primary?RED:CARD,primary?0:0xff4e343b,11));return b;}
 static LinearLayout column(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(1);return v;}
 static LinearLayout row(Context c){LinearLayout v=new LinearLayout(c);v.setOrientation(0);v.setGravity(Gravity.CENTER_VERTICAL);return v;}
 static LinearLayout.LayoutParams lp(Context c,int w,int h,int mt){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(c,w),h<0?h:dp(c,h));p.topMargin=dp(c,mt);return p;}
 static LinearLayout.LayoutParams weight(){return new LinearLayout.LayoutParams(0,-1,1);}
 static void pad(Context c,View v,int x,int y){v.setPadding(dp(c,x),Ui.dp(c,y),dp(c,x),Ui.dp(c,y));}
}