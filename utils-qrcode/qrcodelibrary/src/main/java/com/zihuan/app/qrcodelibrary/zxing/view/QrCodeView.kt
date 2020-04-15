package com.zihuan.app.qrcodelibrary.zxing.view

import android.app.Activity
import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.FrameLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.Result
import com.zihuan.app.qrcodelibrary.R
import com.zihuan.app.qrcodelibrary.zxing.QrCodeScanResultListener
import com.zihuan.app.qrcodelibrary.zxing.camera.CameraManager
import com.zihuan.app.qrcodelibrary.zxing.decoding.CaptureActivityHandler
import com.zihuan.app.qrcodelibrary.zxing.decoding.InactivityTimer
import java.io.IOException
import java.util.*

class QrCodeView : FrameLayout, SurfaceHolder.Callback {
//    constructor(context: Context) : super(context) {
//        initView()
//    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        initView(attrs)
    }

    private var voiceId = 0
    private var handler: CaptureActivityHandler? = null
    private lateinit var viewfinderView: ViewfinderView
    private var hasSurface = false
    private var decodeFormats: Vector<BarcodeFormat>? = null
    private var characterSet: String? = null
    private var inactivityTimer: InactivityTimer? = null
    private var playBeep = false
    private var vibrate = false
    private lateinit var mActivity: Activity
    private var mQrCodeScanResultListener: QrCodeScanResultListener? = null
    fun initView(attrs: AttributeSet?) {
        mActivity = context as Activity
        if (mActivity is QrCodeScanResultListener) {
            mQrCodeScanResultListener = mActivity as QrCodeScanResultListener
        }
        viewfinderView = ViewfinderView(mActivity)
        val att = context.obtainStyledAttributes(attrs, R.styleable.QrCodeView)
        val viewfinder_mask = att.getColor(R.styleable.QrCodeView_backgroundColor, resources.getColor(R.color.viewfinder_mask))
        val result_view = att.getColor(R.styleable.QrCodeView_requestColor, resources.getColor(R.color.result_view))
        val lineColor = att.getColor(R.styleable.QrCodeView_lineColor, resources.getColor(R.color.qrcode_color_def))
        val angleColor = att.getColor(R.styleable.QrCodeView_angleColor, resources.getColor(R.color.qrcode_color_def))
        val textColor = att.getColor(R.styleable.QrCodeView_textColor, resources.getColor(R.color.qrcode_color_def))
        val text = att.getString(R.styleable.QrCodeView_text)
        viewfinderView.setColor(viewfinder_mask, result_view, lineColor, angleColor, textColor)
        viewfinderView.setText(text)
        CameraManager.init(mActivity)
        hasSurface = false
        inactivityTimer = InactivityTimer(mActivity)
        val surfaceView = SurfaceView(mActivity)
        addView(surfaceView)
        addView(viewfinderView)
        val surfaceHolder = surfaceView.holder
        surfaceHolder.addCallback(this)
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        decodeFormats = null
        characterSet = null
        val audioService = mActivity.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioService.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            playBeep = false
        }
        initBeepSound()
        vibrate = true
    }

    override fun surfaceChanged(p0: SurfaceHolder?, p1: Int, p2: Int, p3: Int) {}

    override fun surfaceDestroyed(p0: SurfaceHolder?) {
        hasSurface = false

    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!hasSurface) {
            hasSurface = true
            initCamera(holder)
        }
    }

    private lateinit var soundPool: SoundPool

    // 播放声音
    private fun initBeepSound() {
        mActivity.volumeControlStream = AudioManager.STREAM_MUSIC
        soundPool = SoundPool.Builder().build()
//        SoundPool(1, AudioManager.STREAM_MUSIC, 0)
        //声音ID 加载音频资源,这里用的是第二种，第三个参数为priority，声音的优先级*API中指出，priority参数目前没有效果，建议设置为1。
        try {
            val fileDescriptor = resources.openRawResourceFd(R.raw.beep)
            voiceId = soundPool.load(fileDescriptor, 1)
            //异步需要等待加载完成，音频才能播放成功
//            soundPool.setOnLoadCompleteListener { soundPool1: SoundPool, sampleId: Int, status: Int ->
//                if (status == 0) {
//                    //第一个参数soundID
//                    //第二个参数leftVolume为左侧音量值（范围= 0.0到1.0）
//                    //第三个参数rightVolume为右的音量值（范围= 0.0到1.0）
//                    //第四个参数priority 为流的优先级，值越大优先级高，影响当同时播放数量超出了最大支持数时SoundPool对该流的处理
//                    //第五个参数loop 为音频重复播放次数，0为值播放一次，-1为无限循环，其他值为播放loop+1次
//                    //第六个参数 rate为播放的速率，范围0.5-2.0(0.5为一半速率，1.0为正常速率，2.0为两倍速率)
//                    soundPool.play(voiceId, 1f, 1f, 1, 0, 1f)
//                }
//            }
        } catch (e: IOException) {
        }
    }

    private val VIBRATE_DURATION = 200L

    private fun playBeepSoundAndVibrate() {
        soundPool.play(voiceId, 1f, 1f, 1, 0, 1f)
//        震动
        if (vibrate) {
            val vibrator = mActivity.getSystemService(VIBRATOR_SERVICE) as Vibrator;
            vibrator.vibrate(VIBRATE_DURATION)
        }
    }

    private fun initCamera(surfaceHolder: SurfaceHolder) {
        try {
            CameraManager.get().openDriver(surfaceHolder)
        } catch (ioe: IOException) {
            return
        } catch (e: RuntimeException) {
            return
        }
        if (handler == null) {
            handler = CaptureActivityHandler(mActivity, decodeFormats, characterSet, viewfinderView, this)
        }
    }

    /**
     * 扫描结果
     *
     * @param result
     * @param barcode
     */
    fun handleDecode(result: Result, barcode: Bitmap?) {
        inactivityTimer!!.onActivity()
        val resultString = result.text
        if (resultString.isNullOrBlank()) {
            mQrCodeScanResultListener?.qrFailure(result)
        } else {
            val resultIntent = Intent()
            val bundle = Bundle()
            bundle.putString("SCAN_RESULT", resultString)
            resultIntent.putExtras(bundle)
            Log.e("扫描结果", "扫描结果 $resultString")
            playBeepSoundAndVibrate()
            mQrCodeScanResultListener?.qrSuccessful(Activity.RESULT_OK, resultIntent)
        }
    }

    fun drawViewfinder() {
        viewfinderView.drawViewfinder()
    }

    private fun onPause() {
        if (handler != null) {
            handler!!.quitSynchronously()
            handler = null
        }
        CameraManager.get().closeDriver()
    }

    fun getViewHandler(): Handler {
        return handler!!
    }

    fun onDestroy() {
        onPause()
        soundPool.release()
        inactivityTimer!!.shutdown()
    }
}