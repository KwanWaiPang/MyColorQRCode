package com.king.wechat.qrcode.scanning.analyze;

import android.graphics.Bitmap;
import android.graphics.Color;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;

import com.king.mlkit.vision.camera.AnalyzeResult;
import com.king.mlkit.vision.camera.analyze.Analyzer;
import com.king.mlkit.vision.camera.util.BitmapUtils;
import com.king.mlkit.vision.camera.util.LogUtils;
import com.king.wechat.qrcode.WeChatQRCodeDetector;

import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

/**
 * 微信二维码分析器：分析相机预览的帧数据，从中检测识别二维码
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 */
public class GreenScanningAnalyzer implements Analyzer<List<String>> {

    /**
     * 是否需要输出二维码的各个顶点
     */
    private boolean isOutputVertices;

    public GreenScanningAnalyzer() {
        this(false);
    }

    /**
     * 构造
     *
     * @param isOutputVertices 是否需要返回二维码的各个顶点
     */
    public GreenScanningAnalyzer(boolean isOutputVertices) {
        this.isOutputVertices = isOutputVertices;
    }

    @Override
    public void analyze(@NonNull ImageProxy imageProxy, @NonNull OnAnalyzeListener<AnalyzeResult<List<String>>> listener) {
        AnalyzeResult<List<String>> result = null;
        try {
            final Bitmap bitmap = BitmapUtils.getBitmap(imageProxy);
            result = detectAndDecode(bitmap, isOutputVertices);
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
            final List<Mat> points = new ArrayList<>();

            // 获取图片的宽度和高度
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            // 创建一个新的 Bitmap 来存储红色通道
            Bitmap bitmap_red = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = bitmap.getPixel(x, y);

//                    int red = Color.red(pixel);
//                    int Blue=Color.blue(pixel);
                    int Green=Color.green(pixel);

                    // 将红色通道的值设置到新的 Bitmap 中
                    int newPixel = Color.rgb(0, Green, 0);
                    bitmap_red.setPixel(x, y, newPixel);
                }
            }
            bitmap=bitmap_red;
            List<String> result = WeChatQRCodeDetector.detectAndDecode(bitmap, points);

            if (result != null && !result.isEmpty()) {
                return new QRCodeAnalyzeResult<>(bitmap, result, points);
            }
        } else {
            // 反之则需识别结果即可
            List<String> result = WeChatQRCodeDetector.detectAndDecode(bitmap);
            if (result != null && !result.isEmpty()) {
                return new QRCodeAnalyzeResult<>(bitmap, result);
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
        private List<Mat> points;

        public QRCodeAnalyzeResult(Bitmap bitmap, T result) {
            super(bitmap, result);
        }

        public QRCodeAnalyzeResult(Bitmap bitmap, T result, List<Mat> points) {
            super(bitmap, result);
            this.points = points;
        }

        /**
         * 获取二维码的位置点信息
         *
         * @return
         */
        public List<Mat> getPoints() {
            return points;
        }

        @Deprecated
        public void setPoints(List<Mat> points) {
            this.points = points;
        }
    }
}
