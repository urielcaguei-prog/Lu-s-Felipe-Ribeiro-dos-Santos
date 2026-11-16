package br.com.urielchat;
import android.accessibilityservice.*;
import android.graphics.Path;
import android.view.accessibility.AccessibilityEvent;
import android.os.Handler;
public class GestureService extends AccessibilityService {
 public static volatile GestureService instance;
 @Override protected void onServiceConnected(){super.onServiceConnected();instance=this;}
 @Override public void onAccessibilityEvent(AccessibilityEvent event){}
 @Override public void onInterrupt(){}
 @Override public void onDestroy(){instance=null;super.onDestroy();}
 public void perform(int x,int y,int delta,Runnable finish){
  if(!MainActivity.licensed){finish.run();return;}
  Path path=new Path(); path.moveTo(x,y);path.lineTo(x,Math.max(0,y-Math.max(1,delta)));
  GestureDescription g=new GestureDescription.Builder()
    .addStroke(new GestureDescription.StrokeDescription(path,0,150)).build();
  if(!dispatchGesture(g,new GestureResultCallback(){
    @Override public void onCompleted(GestureDescription g){finish.run();}
    @Override public void onCancelled(GestureDescription g){finish.run();}
  },new Handler(getMainLooper())))finish.run();
 }
}
