package com.king.opencv.qrcode.scanning.analyze;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import com.king.mlkit.vision.camera.AnalyzeResult;
import com.king.mlkit.vision.camera.analyze.Analyzer;
import com.king.mlkit.vision.camera.util.BitmapUtils;
import com.king.mlkit.vision.camera.util.LogUtils;
import com.king.opencv.qrcode.OpenCVQRCodeDetector;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

/**
 * OpenCV二维码分析器：分析相机预览的帧数据，从中检测识别二维码
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 */
public class OpenCVScanningAnalyzer implements Analyzer<List<String>> {

    private OpenCVQRCodeDetector mDetector = new OpenCVQRCodeDetector();
    /**
     * 是否需要输出二维码的各个顶点
     */
    private boolean isOutputVertices;

    public OpenCVScanningAnalyzer() {
        this(false);
    }

    /**
     * 构造
     *
     * @param isOutputVertices 是否需要返回二维码的各个顶点
     */
    public OpenCVScanningAnalyzer(boolean isOutputVertices) {
        this.isOutputVertices = isOutputVertices;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy, @NonNull OnAnalyzeListener<AnalyzeResult<List<String>>> listener) {
        AnalyzeResult<List<String>> result = null;
        try {
            final Bitmap bitmap = BitmapUtils.getBitmap(imageProxy);//获取bitmap
            result = detectAndDecode(bitmap, isOutputVertices);//对bitmap进行解码并且获取二维码的各个顶点
        } catch (Exception e) {
            LogUtils.w(e);
        }
        if (result != null && !result.getResult().isEmpty()) {
            listener.onSuccess(result);
        } else {
            listener.onFailure(null);
        }
    }

    /**
     * 检测并识别二维码
     *
     * @param bitmap
     * @param isOutputVertices
     * @return
     */
    @Nullable
    private AnalyzeResult<List<String>> detectAndDecode(Bitmap bitmap, boolean isOutputVertices) {
        if (isOutputVertices) {
            // 如果需要返回二维码的各个顶点
            final Mat points = new Mat();//二维码各个顶点的信息
//            String result = mDetector.detectAndDecode(bitmap, points);//识别二维码
            //将这个bitmap转换为rgb三通道
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            // Create bitmaps to store individual channels
            Bitmap R_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Bitmap G_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Bitmap B_bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            int[] pixels = new int[width * height];
            int[] R_pixels = new int[width * height];
            int[] G_pixels = new int[width * height];
            int[] B_pixels = new int[width * height];

            // Get the pixels of the original bitmap
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

            for (int i = 0; i < width * height; i++) {
                int pixel = pixels[i];
                int R = Color.red(pixel);
                int G = Color.green(pixel);
                int B = Color.blue(pixel);

                R_pixels[i] = Color.rgb(R, 0, 0); // Set R channel with R value and G, B values as 0
                G_pixels[i] = Color.rgb(0, G, 0); // Set G channel with G value and R, B values as 0
                B_pixels[i] = Color.rgb(0, 0, B); // Set B channel with B value and R, G values as 0
            }

            // Set the pixels of individual channel bitmaps
            R_bitmap.setPixels(R_pixels, 0, width, 0, 0, width, height);
            G_bitmap.setPixels(G_pixels, 0, width, 0, 0, width, height);
            B_bitmap.setPixels(B_pixels, 0, width, 0, 0, width, height);

            String result = mDetector.detectAndDecode(bitmap, points);//识别二维码
            String R_result = mDetector.detectAndDecode(R_bitmap);
            String G_result = mDetector.detectAndDecode(G_bitmap);
            String B_result = mDetector.detectAndDecode(B_bitmap);

//            if (R_result.isEmpty())
//                R_result="null";
//            if (G_result.isEmpty())
//                G_result="null";
//            if (B_result.isEmpty())
//                B_result="null";

            if (result != null && !result.isEmpty()) {

                //将结果输出到log中（输出结果到 Logcat 中）
                Log.d("Color_QR_Code_Result", "R channel: " + R_result);
                Log.d("Color_QR_Code_Result", "G channel: " + G_result);
                Log.d("Color_QR_Code_Result", "B channel: " + B_result);
                Log.d("Color_QR_Code_Result", "raw result: " + result);

                List<String> list = new ArrayList();
                result=R_result + " ; "+ G_result +  " ; "+ B_result;
                list.add(result);//原本的
                //下面把三个通道的值也加入
                list.add("R channel: " + R_result);
                list.add("G channel: " + G_result);
                list.add("B channel: " + B_result);
                return new QRCodeAnalyzeResult<>(bitmap, list, points);
            }
        } else {
            // 反之则需识别结果即可
            String result = mDetector.detectAndDecode(bitmap);
            if (result != null && !result.isEmpty()) {
                List<String> list = new ArrayList();
                list.add(result);
                return new QRCodeAnalyzeResult(bitmap, list);
            }
        }
        return null;
    }

    /**
     * 二维码分析结果
     *
     * @param <T>
     */
    public static class QRCodeAnalyzeResult<T> extends AnalyzeResult<T> {

        /**
         * 二维码的位置点信息
         */
        private Mat points;

        public QRCodeAnalyzeResult(Bitmap bitmap, T result) {
            super(bitmap, result);
        }

        public QRCodeAnalyzeResult(Bitmap bitmap, T result, Mat points) {
            super(bitmap, result);
            this.points = points;
        }

        /**
         * 获取二维码的位置点信息
         *
         * @return
         */
        public Mat getPoints() {
            return points;
        }

        @Deprecated
        public void setPoints(Mat points) {
            this.points = points;
        }
    }
}
