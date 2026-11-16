package br.com.urielchat;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.pose.Pose;
import com.google.mlkit.vision.pose.PoseDetection;
import com.google.mlkit.vision.pose.PoseDetector;
import com.google.mlkit.vision.pose.PoseLandmark;
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions;

public class GestureService extends AccessibilityService {
    public static volatile GestureService instance;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private PoseDetector detector;

    public interface ResultHandler { void complete(String message); }
    private interface HeadCallback { void done(Head head, int width, int height, String error); }
    private static class Head {
        final float x, y, likelihood;
        Head(float x, float y, float likelihood) {
            this.x=x; this.y=y; this.likelihood=likelihood;
        }
    }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance=this;
        detector=PoseDetection.getClient(
            new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE).build());
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
    @Override public void onDestroy() {
        instance=null;
        if(detector!=null) detector.close();
        super.onDestroy();
    }

    /** At most two on-device screenshots, a stationary check, and one manual-triggered gesture.
     *  This is an experimental screen-only algorithm, not Free Fire game-state access.
     */
    public void detectStationaryAndShoot(ResultHandler done) {
        if (!MainActivity.licensed) { done.complete("Valide a licença KeyAuth."); return; }
        if (instance!=this || detector==null) { done.complete("Acessibilidade indisponível."); return; }
        final long deadline=SystemClock.uptimeMillis()+8000;
        captureHead(deadline,(first,w,h,error)-> {
            if(error!=null) { done.complete("Primeira captura: "+error); return; }
            if(first==null) { done.complete("Nenhuma cabeça detectada com confiança suficiente na primeira captura."); return; }
            handler.postDelayed(()->captureHead(deadline,(second,w2,h2,error2)->{
                if(error2!=null) { done.complete("Segunda captura: "+error2); return; }
                if(second==null) { done.complete("A cabeça desapareceu da segunda captura; sem ação."); return; }
                if(w!=w2 || h!=h2) { done.complete("Orientação da tela mudou; sem ação."); return; }

                final float delta=(float)Math.hypot(first.x-second.x,first.y-second.y);
                final float maxMotion=Math.max(9f,Math.min(w,h)*0.018f);
                if(delta>maxMotion) {
                    done.complete("Alvo visualmente em movimento ("+Math.round(delta)+" pixels); sem gesto.");
                    return;
                }
                if(!MainActivity.licensed || SystemClock.uptimeMillis()>deadline || instance!=this) {
                    done.complete("Sessão encerrada ou operação expirada; sem ação.");
                    return;
                }
                doGestureOnce(second,w,h,deadline,done);
            }),330);
        });
    }

    private void captureHead(long deadline,HeadCallback cb) {
        if(SystemClock.uptimeMillis()>deadline || instance!=this) {
            cb.done(null,0,0,"A análise expirou."); return;
        }
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY,getMainExecutor(),new TakeScreenshotCallback() {
                @Override public void onSuccess(ScreenshotResult shot) {
                    Bitmap copy=null;
                    try {
                        HardwareBuffer buffer=shot.getHardwareBuffer();
                        if(buffer==null){cb.done(null,0,0,"Sem imagem capturada.");return;}
                        try {
                            Bitmap source=Bitmap.wrapHardwareBuffer(buffer,shot.getColorSpace());
                            if(source!=null) copy=source.copy(Bitmap.Config.ARGB_8888,false);
                        } finally {buffer.close();}
                        if(copy==null){cb.done(null,0,0,"Falha ao copiar a imagem.");return;}
                        final Bitmap bitmap=copy;
                        final int w=bitmap.getWidth(),h=bitmap.getHeight();
                        detector.process(InputImage.fromBitmap(bitmap,0))
                            .addOnSuccessListener(pose -> {
                                Head head=detectHead(pose,w,h);
                                bitmap.recycle();
                                if(SystemClock.uptimeMillis()>deadline || instance!=GestureService.this)
                                    cb.done(null,w,h,"Captura expirou ou serviço desligado.");
                                else cb.done(head,w,h,null);
                            }).addOnFailureListener(e -> {
                                bitmap.recycle();
                                cb.done(null,w,h,"ML Kit: "+e.getClass().getSimpleName());
                            });
                    } catch(Exception e) {
                        if(copy!=null && !copy.isRecycled()) copy.recycle();
                        cb.done(null,0,0,"Imagem: "+e.getClass().getSimpleName());
                    }
                }
                @Override public void onFailure(int code) {
                    cb.done(null,0,0,"Android bloqueou a captura (código "+code+").");
                }
            });
        } catch(Exception e){cb.done(null,0,0,"Captura: "+e.getClass().getSimpleName());}
    }

    private Head detectHead(Pose pose,int w,int h) {
        if(pose==null) return null;
        PoseLandmark nose=pose.getPoseLandmark(PoseLandmark.NOSE);
        PoseLandmark left=pose.getPoseLandmark(PoseLandmark.LEFT_EYE);
        PoseLandmark right=pose.getPoseLandmark(PoseLandmark.RIGHT_EYE);
        if(nose==null || left==null || right==null) return null;
        if(nose.getInFrameLikelihood()<0.72f ||
           left.getInFrameLikelihood()<0.60f ||
           right.getInFrameLikelihood()<0.60f) return null;
        float x=(left.getPosition().x+right.getPosition().x)/2f;
        float y=(left.getPosition().y+right.getPosition().y)/2f;
        if(x<0 || y<0 || x>=w || y>=h) return null;
        float confidence=Math.min(nose.getInFrameLikelihood(),
                 Math.min(left.getInFrameLikelihood(),right.getInFrameLikelihood()));
        return new Head(x,y,confidence);
    }

    private void doGestureOnce(Head head,int w,int h,long deadline,ResultHandler done){
        float offX=head.x-w*0.5f,offY=head.y-h*0.5f;
        float limit= Math.max(0,getSharedPreferences("settings",0).getInt("delta",55));
        float gain=Math.max(0,getSharedPreferences("settings",0).getInt("strength",30))/100f;
        float distance=(float)Math.hypot(offX,offY);
        float scale=distance<1 ? 0 : Math.min(gain,limit/distance);
        final int dx=Math.round(offX*scale);
        final int dy=Math.round(offY*scale);
        if(dx==0 && dy==0){
            shootAfterGesture(w,h,deadline,done,"Alvo no centro");
            return;
        }
        int touchX=Math.round(w*.78f),touchY=Math.round(h*.67f);
        Path path=new Path(); path.moveTo(touchX,touchY);
        path.lineTo(Math.max(0,Math.min(w-1,touchX+dx)),Math.max(0,Math.min(h-1,touchY+dy)));
        GestureDescription gesture=new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path,0,140)).build();
        try {
            boolean accepted=dispatchGesture(gesture,new GestureResultCallback(){
                @Override public void onCompleted(GestureDescription g){
                    shootAfterGesture(w,h,deadline,done,"Correção "+dx+","+dy);
                }
                @Override public void onCancelled(GestureDescription g){
                    done.complete("O Android cancelou a correção; nenhum disparo foi emitido.");
                }
            },handler);
            if(!accepted) done.complete("O Android rejeitou o gesto; sem disparo.");
        } catch(Exception e){done.complete("Gesto: "+e.getClass().getSimpleName());}
    }

    private void shootAfterGesture(int w,int h,long deadline,ResultHandler done,String description){
        if(!MainActivity.licensed || instance!=this || SystemClock.uptimeMillis()>deadline){
            done.complete("Operação expirada; sem disparo."); return;
        }
        int xp=Math.max(1,Math.min(99,getSharedPreferences("settings",0).getInt("fireX",86)));
        int yp=Math.max(1,Math.min(99,getSharedPreferences("settings",0).getInt("fireY",59)));
        int x=Math.round(w*xp/100f),y=Math.round(h*yp/100f);
        Path path=new Path();path.moveTo(x,y);
        GestureDescription tap=new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path,0,75)).build();
        try {
            boolean accepted=dispatchGesture(tap,new GestureResultCallback(){
                @Override public void onCompleted(GestureDescription g){
                    done.complete(description+"; toque de disparo enviado. Efeito no Free Fire não verificado.");
                }
                @Override public void onCancelled(GestureDescription g){
                    done.complete(description+"; toque de disparo cancelado.");
                }
            },handler);
            if(!accepted)done.complete(description+"; toque não aceito.");
        } catch(Exception e){done.complete(description+"; toque: "+e.getClass().getSimpleName());}
    }
}
