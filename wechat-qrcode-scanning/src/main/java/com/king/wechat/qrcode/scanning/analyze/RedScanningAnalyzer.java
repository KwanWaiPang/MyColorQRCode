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

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

/**
 * 微信二维码分析器：分析相机预览的帧数据，从中检测识别二维码
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 */
public class RedScanningAnalyzer implements Analyzer<List<String>> {

    /**
     * 是否需要输出二维码的各个顶点
     */
    private boolean isOutputVertices;

    public RedScanningAnalyzer() {
        this(false);
    }

    /**
     * 构造
     *
     * @param isOutputVertices 是否需要返回二维码的各个顶点
     */
    public RedScanningAnalyzer(boolean isOutputVertices) {
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
//            List<String> result = WeChatQRCodeDetector.detectAndDecode(bitmap, points);

//            //// 将Bitmap转换为cv::Mat
//            Mat src= new Mat();
//            Utils.bitmapToMat(bitmap, src);//将位图转换为Mat数据。而对于位图，其由A、R、G、B通道组成
//            Mat image_input= new Mat();
//            Imgproc.cvtColor(src, image_input, Imgproc.COLOR_BGRA2BGR);//转换为BGR（opencv中数据存储方式）
//
//
//            // 分离通道
//            List<Mat> channels = new ArrayList<>();
//            Core.split(image_input, channels);
//
//            // 对绿色和蓝色通道置零
//            Mat redChannel = channels.get(2); // 红色通道在索引2
//            Mat greenChannel = channels.get(1); // 绿色通道在索引1
//            Mat blueChannel = channels.get(0); // 蓝色通道在索引0
//
//            greenChannel.setTo(new Scalar(0)); // 绿色通道置零
//            blueChannel.setTo(new Scalar(0));  // 蓝色通道置零
////            redChannel.setTo(new Scalar(0)); //红色通道置0
//
//            // 合并通道
//            Mat image_all = new Mat();
//            Core.merge(channels, image_all);
//            Mat image_red = new Mat();
//            Imgproc.cvtColor(image_all, image_red, Imgproc.COLOR_BGR2BGRA);//转换为BGRA格式
//
//            // 将单通道的Mat转换为Bitmap
//            Bitmap bitmap_red = Bitmap.createBitmap(image_red.cols(), image_red.rows(), Bitmap.Config.ARGB_8888);
//            Utils.matToBitmap(image_red, bitmap_red);
//
//            bitmap=bitmap_red;

            // 获取图片的宽度和高度
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            // 创建一个新的 Bitmap 来存储红色通道
            Bitmap bitmap_red = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int pixel = bitmap.getPixel(x, y);

                    int red = Color.red(pixel);

                    // 将红色通道的值设置到新的 Bitmap 中
                    int newPixel = Color.rgb(red, 0, 0);
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
