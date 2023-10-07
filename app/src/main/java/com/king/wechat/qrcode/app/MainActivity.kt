package com.king.wechat.qrcode.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import com.king.mlkit.vision.camera.CameraScan
import com.king.mlkit.vision.camera.util.LogUtils
import com.king.mlkit.vision.camera.util.PermissionUtils
import com.king.opencv.qrcode.OpenCVQRCodeDetector
import com.king.wechat.qrcode.WeChatQRCodeDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.opencv.OpenCV

/**
 * @author <a href="mailto:jenly1314@gmail.com">Jenly</a>
 */
class MainActivity : AppCompatActivity() {

    /**
     * OpenCVQRCodeDetector
     */
    private val openCVQRCodeDetector by lazy {
        OpenCVQRCodeDetector()
    }

    /**
     * 是否使用 WeChatQRCodeDetector 进行检测二维码
     */
    private var useWeChatDetect = false

    private val selectedImageUris = mutableListOf<Uri>()//用于获取多张图片

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)//设置界面（加载布局）
        // 初始化OpenCV
        OpenCV.initAsync(this)
        // 初始化WeChatQRCodeDetector
        WeChatQRCodeDetector.init(this)
    }

    private fun getContext() = this

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_REQUEST_EXTERNAL_STORAGE && PermissionUtils.requestPermissionsResult(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                permissions,
                grantResults
            )
        ) {
            startPickPhoto()
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_QRCODE -> processQRCodeResult(data)
                REQUEST_CODE_PICK_PHOTO -> processPickPhotoResult(data)
            }
        }

        if (requestCode == REQUEST_PICK_IMAGE) {
            data?.clipData?.let { clipData ->
                selectedImageUris.clear()//一开始先清空一下
                for (i in 0 until minOf(clipData.itemCount, 3)) {//最多选3张
                    val imageUri = clipData.getItemAt(i).uri
                    selectedImageUris.add(imageUri)
                }
            }

            val intent = Intent(this, DisplayPhotoActivity::class.java)
            intent.putParcelableArrayListExtra(DisplayPhotoActivity.EXTRA_IMAGE_URI, ArrayList(selectedImageUris))
            startActivity(intent)
        }

        //下面是单张图片
//        if (requestCode == REQUEST_PICK_IMAGE) {//// 在onActivityResult中处理选择的图片
//            val imageUri = data?.data
//            if (imageUri != null) {
//                val intent = Intent(this, DisplayPhotoActivity::class.java)
//                intent.putExtra(DisplayPhotoActivity.EXTRA_IMAGE_URI, imageUri)
//                startActivity(intent)
//            }
//        }

    }

    /**
     * 处理选择图片后，从图片中检测二维码结果
     */
    private fun processPickPhotoResult(data: Intent?) {
        data?.let {
            try {
                lifecycleScope.launch {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, it.data)
                    if (useWeChatDetect) {
                        val result = withContext(Dispatchers.IO) {
                            // 通过WeChatQRCodeDetector识别图片中的二维码
                            WeChatQRCodeDetector.detectAndDecode(bitmap)
                        }
                        if (result.isNotEmpty()) {// 不为空，则表示识别成功
                            // 打印所有结果
                            for ((index, text) in result.withIndex()) {
                                LogUtils.d("result$index:$text")
                            }
                            // 一般需求都是识别一个码，所以这里取第0个就可以；有识别多个码的需求，可以取全部
                            Toast.makeText(getContext(), result[0], Toast.LENGTH_SHORT).show()
                        } else {
                            // 为空表示识别失败
                            LogUtils.d("result = null")
                        }
                    } else {
                        val result = withContext(Dispatchers.IO) {
                            // 通过OpenCVQRCodeDetector识别图片中的二维码
                            openCVQRCodeDetector.detectAndDecode(bitmap)
                        }

                        if (!result.isNullOrEmpty()) {// 不为空，则表示识别成功
                            LogUtils.d("result$result")
                            Toast.makeText(getContext(), result, Toast.LENGTH_SHORT).show()
                        } else {
                            // 为空表示识别失败
                            LogUtils.d("result = null")
                        }
                    }

                }

            } catch (e: Exception) {
                LogUtils.w(e)
            }

        }
    }

    private fun processQRCodeResult(intent: Intent?) {
        // 扫码结果
        CameraScan.parseScanResult(intent)?.let {
            Log.d(CameraScan.SCAN_RESULT, it)
            Toast.makeText(getContext(), it, Toast.LENGTH_SHORT).show()
        }
    }

    private fun pickPhotoClicked(useWeChatDetect: Boolean) {
        this.useWeChatDetect = useWeChatDetect
        if (PermissionUtils.checkPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            startPickPhoto()
        } else {
            PermissionUtils.requestPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                REQUEST_CODE_REQUEST_EXTERNAL_STORAGE
            )
        }
    }

    private fun startPickPhoto() {
        val pickIntent = Intent(
            Intent.ACTION_PICK,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
        startActivityForResult(pickIntent, REQUEST_CODE_PICK_PHOTO)
    }

    private fun startActivityForResult(clazz: Class<*>) {
        val options = ActivityOptionsCompat.makeCustomAnimation(this, R.anim.alpha_in, R.anim.alpha_out)
        startActivityForResult(Intent(this, clazz), REQUEST_CODE_QRCODE, options.toBundle())
    }

    fun onClick(view: View) {//点击按钮，不同的按钮id对应不同的功能
        when (view.id) {
            R.id.btnWeChatQRCodeScan -> startActivityForResult(WeChatQRCodeActivity::class.java)
            R.id.btnWeChatMultiQRCodeScan -> startActivityForResult(WeChatMultiQRCodeActivity::class.java)
            R.id.btnWeChatQRCodeDecode -> pickPhotoClicked(true)
            R.id.btnOpenCVQRCodeScan -> startActivityForResult(OpenCVQRCodeActivity::class.java)
            R.id.btnOpenCVQRCodeDecode -> pickPhotoClicked(false)

            //新定义设计的彩色二维码(通过opencv实现彩色二维码)
            R.id.ColorOpencvQRCodeScan -> startActivityForResult(ColorQRCodeActivity::class.java)

            //定义按键点击创建彩色二维码
            R.id.ColorQRCodeGeneration ->pickImage()
        }
    }

    private fun pickImage() {//拿到一张图片（从相册中）
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    companion object {

        const val REQUEST_CODE_QRCODE = 0x10
        const val REQUEST_CODE_REQUEST_EXTERNAL_STORAGE = 0x11
        const val REQUEST_CODE_PICK_PHOTO = 0x12

        private const val REQUEST_PICK_IMAGE = 100 // 请求选择图片的请求码
    }
}