package br.com.urielchat;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
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
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private PoseDetector detector;

    public interface ResultHandler { void complete(String message); }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        detector = PoseDetection.getClient(
            new PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.SINGLE_IMAGE_MODE)
                .build());
    }
    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() {}
    @Override public void onDestroy() {
        instance = null;
        if (detector != null) detector.close();
        super.onDestroy();
    }

    /**
     * Experiment: one screenshot, one ML Kit inference, at most one gesture.
     * It does not know enemies from friends, nor does it have game integration.
     * No image is stored or sent to a server.
     */
    public void detectAndTryCorrection(ResultHandler done) {
        if (!MainActivity.licensed) { done.complete("Licença não validada."); return; }
        if (detector == null) { done.complete("Detector indisponível."); return; }
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override public void onSuccess(ScreenshotResult screenshot) {
                    Bitmap copy = null;
                    try {
                        HardwareBuffer hardware = screenshot.getHardwareBuffer();
                        if (hardware == null) { done.complete("A captura não retornou uma imagem."); return; }
                        try {
                            Bitmap wrapped = Bitmap.wrapHardwareBuffer(hardware, screenshot.getColorSpace());
                            if (wrapped != null) copy = wrapped.copy(Bitmap.Config.ARGB_8888, false);
                        } finally { hardware.close(); }
                        if (copy == null) { done.complete("Não foi possível ler a captura."); return; }
                        final int width=copy.getWidth(), height=copy.getHeight();
                        final Bitmap source=copy;
                        detector.process(InputImage.fromBitmap(source,0))
                            .addOnSuccessListener(pose -> {
                                try { correctOnlyWhenConfident(pose,width,height,done); }
                                finally { source.recycle(); }
                            })
                            .addOnFailureListener(e -> {
                                source.recycle();
                                done.complete("Falha na análise visual: "+e.getClass().getSimpleName());
                            });
                    } catch (Exception e) {
                        if (copy!=null && !copy.isRecycled()) copy.recycle();
                        done.complete("Captura não disponível: "+e.getClass().getSimpleName());
                    }
                }
                @Override public void onFailure(int code) {
                    done.complete("Android não permitiu capturar esta tela (código "+code+").");
                }
            });
        } catch(Exception e) { done.complete("Erro na captura: "+e.getClass().getSimpleName()); }
    }

    private void correctOnlyWhenConfident(Pose pose, int width, int height, ResultHandler done) {
        if (!MainActivity.licensed) { done.complete("Sessão sem licença."); return; }
        PoseLandmark nose = pose.getPoseLandmark(PoseLandmark.NOSE);
        PoseLandmark leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE);
        PoseLandmark rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE);
        if (nose==null || nose.getInFrameLikelihood()<0.75f ||
            (leftEye==null || leftEye.getInFrameLikelihood()<0.65f) &&
            (rightEye==null || rightEye.getInFrameLikelihood()<0.65f)) {
            done.complete("Cabeça não identificada com confiança suficiente. Nenhum gesto aplicado.");
            return;
        }

        float targetX=nose.getPosition().x;
        float targetY=nose.getPosition().y;
        if (leftEye!=null && rightEye!=null &&
            leftEye.getInFrameLikelihood()>0.65f &&
            rightEye.getInFrameLikelihood()>0.65f) {
            targetX=(leftEye.getPosition().x+rightEye.getPosition().x)/2f;
            targetY=(leftEye.getPosition().y+rightEye.getPosition().y)/2f;
        }
        float offsetX=targetX-width/2f;
        float offsetY=targetY-height/2f;
        if (Math.abs(offsetX) < 8 && Math.abs(offsetY) < 8) {
            done.complete("Cabeça detectada perto do centro. Nenhum gesto necessário.");
            return;
        }
        // Experimental conversion: image pixels are not the same as camera angles.
        int limit=getSharedPreferences("settings",0).getInt("delta",55);
        double dist=Math.hypot(offsetX,offsetY);
        float scale=(float)Math.min(0.35,Math.max(0,limit)/Math.max(1,dist));
        int dx=Math.round(offsetX*scale);
        int dy=Math.round(offsetY*scale);
        if (dx==0 && dy==0) { done.complete("Correção abaixo do limiar."); return; }
        int touchX=(int)(width*0.78f),touchY=(int)(height*0.68f);
        attemptSwipe(touchX,touchY,dx,dy,done);
    }

    private void attemptSwipe(int x,int y,int dx,int dy,ResultHandler done) {
        Path path = new Path();
        path.moveTo(x,y);
        path.lineTo(x+dx,y+dy);
        GestureDescription gesture = new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(path,0,160)).build();
        boolean submitted=dispatchGesture(gesture,new GestureResultCallback() {
            @Override public void onCompleted(GestureDescription g) {
                done.complete("Gesto experimental enviado. O resultado no jogo não foi verificado.");
            }
            @Override public void onCancelled(GestureDescription g) {
                done.complete("O Android cancelou o gesto.");
            }
        },mainHandler);
        if (!submitted) done.complete("O Android rejeitou o gesto.");
    }
}
