package com.king.wechat.qrcode.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Path
import android.util.Log
import android.view.View
import android.widget.ImageView
import com.king.app.dialog.AppDialog
import com.king.app.dialog.AppDialogConfig
import com.king.mlkit.vision.camera.AnalyzeResult
import com.king.mlkit.vision.camera.CameraScan
import com.king.mlkit.vision.camera.analyze.Analyzer
import com.king.opencv.qrcode.scanning.OpenCVCameraScanActivity
import com.king.opencv.qrcode.scanning.analyze.OpenCVScanningAnalyzer

import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core

/**
 * OpenCV二维码扫描实现示例
 *
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 */
class ColorQRCodeActivity : OpenCVCameraScanActivity() {//继承opencv中camera扫描的activity
    override fun onScanResultCallback(result: AnalyzeResult<MutableList<String>>) {//接收AnalyzeResult。AnalyzeResult<MutableList<String>>是qr code的位置信息
        // 停止分析
        cameraScan.setAnalyzeImage(false)
        Log.d(TAG, result.result.toString())

        //如果是OpenCVScanningAnalyzer.QRCodeAnalyzeResult ，那么就对二维码的位置信息进行处理
        // 当初始化 OpenCVScanningAnalyzer 时，如果是需要二维码的位置信息，则会返回 OpenCVAnalyzeResult
        if (result is OpenCVScanningAnalyzer.QRCodeAnalyzeResult) { // 如果需要处理结果二维码的位置信息

            val buffer = StringBuilder() //val为不可变变量
            val bitmap = result.bitmap!!.drawRect { canvas, paint ->
                // 扫码结果（result.bitmap就是“Bitmap”对象扫到的包含二维码的图像）
                result.result.forEachIndexed{ index, data ->
                    buffer.append("[$index] ").append(data).append("\n")
                }
                Log.w(TAG, "123456cols: ${result.points.cols()}, rows: ${result.points.rows()}")

                for (i in 0 until result.points.rows()) {//result.points是一个cv::Mat里面存有QR code的位置信息
                    result.points.row(i).let { mat ->
                        // 扫码结果二维码的四个点（一个四边形）；需要注意的是：OpenCVQRCode识别的二维码和WeChatQRCode的识别的二维码记录在Mat中的点位方式是不一样的
                        Log.d(TAG, "point0: ${mat[0, 0][0]}, ${mat[0, 0][1]}")
                        Log.d(TAG, "point1: ${mat[0, 1][0]}, ${mat[0, 1][1]}")
                        Log.d(TAG, "point2: ${mat[0, 2][0]}, ${mat[0, 2][1]}")
                        Log.d(TAG, "point3: ${mat[0, 3][0]}, ${mat[0, 3][1]}")
                        val path = Path()
                        path.moveTo(mat[0, 0][0].toFloat(), mat[0, 0][1].toFloat())
                        path.lineTo(mat[0, 1][0].toFloat(), mat[0, 1][1].toFloat())
                        path.lineTo(mat[0, 2][0].toFloat(), mat[0, 2][1].toFloat())
                        path.lineTo(mat[0, 3][0].toFloat(), mat[0, 3][1].toFloat())
                        path.lineTo(mat[0, 0][0].toFloat(), mat[0, 0][1].toFloat())
                        // 将二维码位置在图片上框出来
                        canvas.drawPath(path, paint)
                    }
                }
            }

            val config = AppDialogConfig(this, R.layout.qrcode_result_dialog).apply {
                content = buffer
                onClickConfirm = View.OnClickListener {
                    AppDialog.INSTANCE.dismissDialog()
                    // 继续扫码分析
                    cameraScan.setAnalyzeImage(true)
                }
                onClickCancel = View.OnClickListener {
                    AppDialog.INSTANCE.dismissDialog()
                    finish()
                }
                val imageView = getView<ImageView>(R.id.ivDialogContent)//在需要显示的位置添加一个ImageView(其ID为ivDialogContent)
                imageView.setImageBitmap(bitmap)//将bitmap显示在这个界面上
//                imageView.setImageBitmap(redBitmap);
            }
            AppDialog.INSTANCE.showDialog(config, false)

        } else {
            // 一般需求都是识别一个码，所以这里取第0个就可以；有识别多个码的需求，可以取全部
            val text = result.result[0]
            val intent = Intent()
            intent.putExtra(CameraScan.SCAN_RESULT, text)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun createAnalyzer(): Analyzer<MutableList<String>>? {
        // 如果需要返回结果二维码位置信息，则初始化分析器时，参数传 true 即可
        return OpenCVScanningAnalyzer(true)
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_opencv_qrcode
    }

    companion object {
//        const val TAG = "OpenCVQRCodeActivity"
    const val TAG = "ColorQRCodeActivity"

    }
}