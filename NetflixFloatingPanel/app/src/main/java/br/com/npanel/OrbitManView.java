package br.com.npanel;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;
/** Animated illustration: does not track live game players. */
public class OrbitManView extends View {
 private Paint p=new Paint(3);private SharedPreferences prefs;private float angle=0;private long last=0;
 private float cx,cy,scale;
 public OrbitManView(Context c){super(c);prefs=c.getSharedPreferences("panel",Context.MODE_PRIVATE);}
 private float[] pt(float x,float y,float z){
  float xx=(float)(x*Math.cos(angle)+z*Math.sin(angle));
  float zz=(float)(-x*Math.sin(angle)+z*Math.cos(angle));
  float perspective=2.6f/(2.6f+zz*.25f);
  return new float[]{cx+xx*scale*perspective,cy-y*scale*perspective};
 }
 private void ink(int c){p.setColor(c);p.setStyle(Paint.Style.FILL);p.setStrokeWidth(1);}
 private void seg(Canvas c,float[] a,float[] b,int color,float width){
  ink(color);p.setStyle(Paint.Style.STROKE);p.setStrokeCap(Paint.Cap.ROUND);p.setStrokeJoin(Paint.Join.ROUND);p.setStrokeWidth(Ui.dp(getContext(),width));
  c.drawLine(a[0],a[1],b[0],b[1],p);p.setStyle(Paint.Style.FILL);
 }
 private void point(Canvas c,float[] a,int clr,float r){ink(clr);c.drawCircle(a[0],a[1],Ui.dp(getContext(),r),p);}
 private void tag(Canvas c,String s,int x,int y,int size,int color){
  ink(color);p.setTypeface(Typeface.create("monospace",Typeface.BOLD));p.setTextSize(Ui.dp(getContext(),size));c.drawText(s,Ui.dp(getContext(),x),Ui.dp(getContext(),y),p);
 }
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);int w=getWidth(),h=getHeight();if(w<30||h<30)return;
  cx=w*.52f;cy=h*.52f;scale=Math.min(w*.24f,h*.25f);
  long now=android.os.SystemClock.uptimeMillis();
  boolean animate=prefs.getBoolean("rotate",true);
  if(animate&&last!=0)angle+=(now-last)*.0005f*prefs.getInt("spin",65)/65f;
  last=now;
  ink(0xff0d151a);c.drawRoundRect(new RectF(0,0,w,h),Ui.dp(getContext(),16),Ui.dp(getContext(),16),p);
  p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Ui.dp(getContext(),1));p.setColor(0xff22343b);
  for(float x=0;x<w;x+=Ui.dp(getContext(),26))c.drawLine(x,0,x,h,p);
  for(float y=0;y<h;y+=Ui.dp(getContext(),26))c.drawLine(0,y,w,y,p);
  p.setColor(0xff3b5961);
  c.drawOval(new RectF(cx-scale*.85f,cy+scale*.95f,cx+scale*.85f,cy+scale*1.2f),p);
  p.setColor(0xff9a2934);c.drawCircle(cx,cy-scale*.3f,scale*.92f,p);p.setStyle(Paint.Style.FILL);
  float[] hip=pt(0,-.05f,0),neck=pt(0,1.05f,0),head=pt(0,1.41f,0);
  float[] ls=pt(-.44f,.86f,0),rs=pt(.44f,.86f,0);
  float[] le=pt(-.64f,.31f,0),re=pt(.64f,.31f,0);
  seg(c,pt(-.25f,-.1f,0),pt(-.3f,-.78f,0),0xff708b9a,14);
  seg(c,pt(.25f,-.1f,0),pt(.3f,-.78f,0),0xff879cab,14);
  seg(c,pt(-.3f,-.78f,0),pt(-.33f,-1.26f,-.06f),0xff667c89,12);
  seg(c,pt(.3f,-.78f,0),pt(.33f,-1.26f,-.06f),0xff8499a9,12);
  seg(c,ls,le,0xff8198a5,11);seg(c,rs,re,0xff91aab8,11);
  seg(c,le,pt(-.59f,-.12f,.2f),0xff6d8493,10);seg(c,re,pt(.59f,-.12f,.2f),0xff8199a8,10);
  seg(c,hip,neck,0xff718e9d,30);
  seg(c,pt(-.25f,.70f,-.04f),pt(.25f,.70f,-.04f),0xffd0e3ed,2);
  seg(c,neck,head,0xffa1b7c1,11);point(c,head,0xffc2dbe3,16);
  point(c,pt(0,1.41f,.18f),Ui.RED,3);
  String target=prefs.getString("target","CABEÇA");
  float[] marker=target.equals("PEITO")?pt(0,.63f,.15f):target.equals("PESCOÇO")?neck:head;
  ink(Ui.RED);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(Ui.dp(getContext(),2));
  c.drawCircle(marker[0],marker[1],Ui.dp(getContext(),24),p);
  float d=Ui.dp(getContext(),32);c.drawLine(marker[0]-d,marker[1],marker[0]-d*.55f,marker[1],p);
  c.drawLine(marker[0]+d*.55f,marker[1],marker[0]+d,marker[1],p);
  p.setStyle(Paint.Style.FILL);
  tag(c,"MODEL // 01",12,20,10,Ui.MUTED);
  tag(c,target,12,38,10,Ui.RED);
  tag(c,"360° PREVIEW",12,(int)(h/getResources().getDisplayMetrics().density-13),10,Ui.MUTED);
  if(animate&&isAttachedToWindow())postInvalidateDelayed(48);
 }
 public void refresh(){invalidate();}
}