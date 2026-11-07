package br.com.npanel;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
final class Ui {
 static final int RED=Color.rgb(229,9,20),BG=Color.rgb(10,10,10),WHITE=Color.WHITE;
 static int dp(Context c,float v){return (int)(v*c.getResources().getDisplayMetrics().density+.5f);}
 static GradientDrawable box(Context c,int color,int stroke,float radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(c,radius));if(stroke!=0)g.setStroke(dp(c,1),stroke);return g;}
 static TextView text(Context c,String s,int size,boolean bold,int color){TextView t=new TextView(c);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setGravity(Gravity.CENTER_VERTICAL);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return t;}
 static Button button(Context c,String s){Button b=new Button(c);b.setText(s);b.setAllCaps(false);b.setTextSize(14);b.setTextColor(WHITE);b.setBackground(box(c,RED,0,10));return b;}
 static LinearLayout column(Context c){LinearLayout l=new LinearLayout(c);l.setOrientation(1);return l;}
 static LinearLayout.LayoutParams lp(Context c,int w,int h,int top){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w<0?w:dp(c,w),h<0?h:dp(c,h));p.topMargin=dp(c,top);return p;}
}