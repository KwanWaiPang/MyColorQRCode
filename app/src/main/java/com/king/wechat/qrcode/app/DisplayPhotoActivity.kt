package com.king.wechat.qrcode.app

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

class DisplayPhotoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_display_image)

        val imageView = findViewById<ImageView>(R.id.ColorQRCodeImageView)
//        val imageUri = intent.getParcelableExtra<Uri>(EXTRA_IMAGE_URI)
        val imageUris = intent.getParcelableArrayListExtra<Uri>(EXTRA_IMAGE_URI)//获取一组数据
//        imageView.setImageURI(imageUri)
        val imageUri_R = imageUris?.get(0)
        val imageUri_G = imageUris?.get(1)
        val imageUri_B = imageUris?.get(2)

//        val bitmap = getBitmapFromImageUri(imageUri)//将uri变成位图
//        val processMat = ProcessBitmapinCVmat(bitmap)//将位图变成opencv中的cv::Mat
//        val grayscaleBitmap = bitmapFromMat(processMat)//再变回位图

//        将uri变成位图
        val bitmap_R = getBitmapFromImageUri(imageUri_R)
        val bitmap_G = getBitmapFromImageUri(imageUri_G)
        val bitmap_B = getBitmapFromImageUri(imageUri_B)
        val processMat = GenerateColorCode(bitmap_R,bitmap_G,bitmap_B)
        val grayscaleBitmap = bitmapFromMat(processMat)//再变回位图

        imageView.setImageBitmap(grayscaleBitmap)
    }

    private fun getBitmapFromImageUri(imageUri: Uri?): Bitmap {
        return MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
    }

    private fun ProcessBitmapinCVmat(bitmap: Bitmap): Mat {
        val rgbaMat = Mat()
        Utils.bitmapToMat(bitmap, rgbaMat)
        val grayMat = Mat(rgbaMat.rows(), rgbaMat.cols(), CvType.CV_8UC1)
        Imgproc.cvtColor(rgbaMat, grayMat, Imgproc.COLOR_RGB2GRAY)
        return grayMat
    }

    private fun GenerateColorCode(bitmap_r: Bitmap,bitmap_g: Bitmap,bitmap_b: Bitmap): Mat {
        val r_Mat = Mat()
        Utils.bitmapToMat(bitmap_r, r_Mat)
        val g_Mat = Mat()
        Utils.bitmapToMat(bitmap_g, g_Mat)
        val b_Mat = Mat()
        Utils.bitmapToMat(bitmap_b, b_Mat)

        val r_Mat_gray = Mat(r_Mat.rows(), r_Mat.cols(), CvType.CV_8UC1)
        Imgproc.cvtColor(r_Mat, r_Mat_gray, Imgproc.COLOR_RGB2GRAY)

        val g_Mat_gray = Mat(g_Mat.rows(), g_Mat.cols(), CvType.CV_8UC1)
        Imgproc.cvtColor(g_Mat, g_Mat_gray, Imgproc.COLOR_RGB2GRAY)

        val b_Mat_gray = Mat(b_Mat.rows(), b_Mat.cols(), CvType.CV_8UC1)
        Imgproc.cvtColor(b_Mat, b_Mat_gray, Imgproc.COLOR_RGB2GRAY)

        val OutputMat = Mat(r_Mat.rows(), r_Mat.cols(), CvType.CV_8UC3)
        val r_OutputMat = Mat(r_Mat.rows(), r_Mat.cols(), CvType.CV_8UC3)
        val g_OutputMat = Mat(r_Mat.rows(), r_Mat.cols(), CvType.CV_8UC3)
        val b_OutputMat = Mat(r_Mat.rows(), r_Mat.cols(), CvType.CV_8UC3)

        for (i in 0 until r_Mat.rows()) {
            for (j in 0 until r_Mat.cols()) {
                val rValue = r_Mat_gray.get(i, j)[0]
                val gValue = g_Mat_gray.get(i, j)[0]
                val bValue = b_Mat_gray.get(i, j)[0]

//                if(rValue == 0.0 && gValue==0.0 && bValue==0.0)
//                {
//                    OutputMat.put(i, j, byteArrayOf(255.0.toInt().toByte(),
//                        255.0.toInt().toByte(), 255.0.toInt().toByte()))
//                }else{
                    OutputMat.put(i, j, byteArrayOf(rValue.toInt().toByte(),
                        gValue.toInt().toByte(), bValue.toInt().toByte()
                    ))
//                }

                if(rValue == 0.0)
                {
                    r_OutputMat.put(i, j, byteArrayOf(255.0.toInt().toByte(),
                        255.0.toInt().toByte(), 255.0.toInt().toByte()))
                }else{
                    r_OutputMat.put(i, j, byteArrayOf(rValue.toInt().toByte(),
                        0.0.toInt().toByte(), 0.0.toInt().toByte()))
                }

                if(gValue == 0.0)
                {
                    g_OutputMat.put(i, j, byteArrayOf(255.0.toInt().toByte(),
                        255.0.toInt().toByte(), 255.0.toInt().toByte()))
                }else{
                    g_OutputMat.put(i, j, byteArrayOf(0.0.toInt().toByte(),
                        gValue.toInt().toByte(), 0.0.toInt().toByte()))
                }

                if(bValue == 0.0)
                {
                    b_OutputMat.put(i, j, byteArrayOf(255.0.toInt().toByte(),
                        255.0.toInt().toByte(), 255.0.toInt().toByte()))
                }else{
                    b_OutputMat.put(i, j, byteArrayOf(0.0.toInt().toByte(),
                        0.0.toInt().toByte(), bValue.toInt().toByte()))
                }
            }
        }

        return OutputMat
    }

    private fun bitmapFromMat(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mat, bitmap)
        return bitmap
    }

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
}
