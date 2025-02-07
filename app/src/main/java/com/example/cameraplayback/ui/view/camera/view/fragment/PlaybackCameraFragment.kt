package com.example.cameraplayback.ui.view

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cameraplayback.databinding.FragmentPlaybackCameraBinding
import com.example.cameraplayback.di.scheduler.SchedulerProviderImpl
import com.example.cameraplayback.resource.ResourcesService
import com.example.cameraplayback.resource.ResourcesServiceImpl
import com.example.cameraplayback.ui.view.camera.view.fragment.adapter.DayPlaybackAdapter
import com.example.cameraplayback.ui.view.camera.viewmodel.MainViewModel
import com.example.cameraplayback.utils.Constant
import com.example.cameraplayback.utils.Constant.ConfigCamera.VNPT_CAMERA_RECORD_RESOLUTION_FHD
import com.example.cameraplayback.utils.Constant.ConfigCamera.VNPT_CAMERA_RECORD_RESOLUTION_SD
import com.example.cameraplayback.utils.extension.dayMonthYear
import com.example.cameraplayback.utils.extension.getCurrentDate
import com.example.cameraplayback.utils.extension.getEndOfDay
import com.example.cameraplayback.utils.extension.getStartOfDay
import com.example.cameraplayback.utils.view.ScalableScaleBarForCameraSJ
import com.example.cameraplayback.utils.view.VideoTimeBarForCameraSJ
import com.example.cameraplayback.utils.view.utils.ColorScaleForCameraSJ
import com.vnpttech.opengl.MGLSurfaceView
import vn.vnpt.ONEHome.di.component.scheduler.SchedulerProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// timeStamp 1720887363000; getStartOfDay(timeStamp) 1720803600000
class PlaybackCameraFragment : Fragment(),
    MGLSurfaceView.ISwipeTouchListener,
    ScalableScaleBarForCameraSJ.OnCursorListener{
    private var _binding: FragmentPlaybackCameraBinding? = null
    private val binding get() = _binding!!
    private val mainViewModel: MainViewModel by viewModels()
    private var timeStamp = 1721613779000
    lateinit var dayAdapter: DayPlaybackAdapter

    private var onTouchTimeBar: Boolean = false

    private lateinit var uid: String
    private lateinit var password: String




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaybackCameraBinding.inflate(inflater, container, false)
        binding.apply {
            timeLinePlayback.setOnCursorListener(this@PlaybackCameraFragment)
        }

        uid = arguments?.getString("uid").toString()
        password = arguments?.getString("password").toString()


        addObserver()
        onCommonViewLoaded()
        return binding.root
    }


    private fun addObserver() {

        // Observe state camera
        mainViewModel.apply {
            cameraState.observe(viewLifecycleOwner) { state ->
                setViewStateCamera(state)
            }
            compositeDisposable.add(
                dataVideoStream
                    .subscribeOn(schedulerProvider.newThread())
                    .observeOn(schedulerProvider.newThread())
                    .subscribe { data ->
                        binding.glSurfaceview.setYUVData(
                            data.widthData,
                            data.heightData,
                            data.byteArray_Data_fy,
                            data.byteArray_Data_fu,
                            data.byteArray_Data_fv
                        )
                    }
            )

            cursorTimebar.observe(viewLifecycleOwner) { cursorTime ->
                if (cursorTime.first) {
                    binding.timeLinePlayback.cursorValue = cursorTime.second
                }

            }

            colorDataTimeline.observe(viewLifecycleOwner) { data ->
                Log.d("ducpa", "nhay vao day $data")
                binding.timeLinePlayback.setEnableView(1F,true)
                binding.timeLinePlayback.setColorScale(
                    ColorScaleForCameraSJ(data)
                )
            }

            // Set video record quality
            videoRecordQuality.observe(viewLifecycleOwner) { quality ->
                setQualityValue(quality)
            }

            // Observe state play video playback
            playbackState.observe(viewLifecycleOwner) { state ->
                setViewStatePlayFilePlayback(state)
            }
        }
    }

//    /**
//     * Chọn ngày để play
//     */
//    private fun setSelectDayToPlay(midDaySelected: Long) {
//        dayAdapter.apply {
//            setCheckedItem(midDaySelected)
//            setTmpSelectDay(midDaySelected)
//        }
//        focusSelectedDay()
//        setUpPeriodTimeForTimeline(midDaySelected)
//        binding.timeLinePlayback.cursorValue = midDaySelected
//        viewModel.selectDayToPlay(midDaySelected, Constant.PlayPlaybackFileEvent.SELECT_DAY)
//    }



    private fun setUpPeriodTimeForTimeline(time: Long) {
        val endTime = getEndOfDay(time)
        val startTime = getStartOfDay(time)
        binding.apply {
            timeLinePlayback.apply {
                setRange(startTime, endTime)
                setMode(VideoTimeBarForCameraSJ.MODE_UNIT_1_MIN)
            }

            tvCurrentDay.text = getCurrentDate(
                currentTime = time,
                pattern = dayMonthYear
            )
        }
    }

    /**
     * Cập nhật view theo trạng thái play file playback: play, pause, seek,...
     */
    private fun setViewStatePlayFilePlayback(state: Constant.PlaybackSdCardStateUI) {
        Log.d("ducpa", "state: $state")
        when (state) {
            Constant.PlaybackSdCardStateUI.PLAYING -> {
                setViewPlayingPlayback()
                showLoading(false)
            }

            Constant.PlaybackSdCardStateUI.CONTINUE_PLAY -> {
               setViewPlayingPlayback()
            }

            Constant.PlaybackSdCardStateUI.PAUSING -> {
//                setViewPausingEvent()
            }

            Constant.PlaybackSdCardStateUI.PAUSED -> {
//                setViewPausedEvent()
            }

            Constant.PlaybackSdCardStateUI.SEEK -> {
               setViewSeekVideoPlayback()
                showLoading(true)
            }

            Constant.PlaybackSdCardStateUI.NEXT_FILE -> {
                setViewNextFilePlayback()
            }

            Constant.PlaybackSdCardStateUI.SELECT_DAY -> {
                // UI giống với state NEXT_FILE
//                setViewNextFilePlayback()
            }

            Constant.PlaybackSdCardStateUI.EMPTY_FILE,
            Constant.PlaybackSdCardStateUI.EMPTY_FILE_ALL_DAY -> {
//                setViewEmptyFilePlayback()
            }

            Constant.PlaybackSdCardStateUI.SCAN -> {
//                updateViewWhenScanEvent()
            }

            Constant.PlaybackSdCardStateUI.NEXT_DAY -> {
//                setViewNextFilePlayback()
//                updateViewWhenScanEvent()
            }

            Constant.PlaybackSdCardStateUI.COMPLETE -> {
//                setViewPlayComplete()
            }

            Constant.PlaybackSdCardStateUI.EMPTY_FILE_NOTIFICATION -> {
//                setViewEmptyFilePlaybackNotification()
            }
        }
    }

    /**
     * Set view khi playback ở trạng thái next file
     */
    private fun setViewNextFilePlayback() {
        showLoading(true)
        updateControlView(false)
        updateTimebarView(false)
    }

    /**
     * Set view khi playback ở trạng thái playing
     */
    private fun setViewPlayingPlayback() {
        binding.apply {
            glSurfaceview.show()
        }
    }

    private fun View.show() {
        this.visibility = View.VISIBLE
    }

    /**
     * Set view khi playback ở trạng thái seek video
     */
    private fun setViewSeekVideoPlayback() {
        Log.d("ducpa", "nhảy vào setViewSeekVideoPlayback")
        updateControlView(false)
        dayAdapter.setPickableDay(false)
    }

    /**
     * Cập nhật trạng thái các button trên thanh điều khiển:
     * Button play/pause, sound, screenshot, record, quality
     */
    private fun updateControlView(enable: Boolean) {
        val alpha: Float = if (enable) 1F else 0.3F
        Log.d("ducpa", "nhảy vào updateControlView")
        binding.apply {
//            ivPlay.setEnableView(alpha, enable)
//            ivLandPlay.setEnableView(alpha, enable)
//            ivReceiveAudio.setEnableView(alpha, enable)
//            ivLandReceiveAudio.setEnableView(alpha, enable)
//            ivScreenshot.setEnableView(alpha, enable)
//            ivLandScreenshot.setEnableView(alpha, enable)
//            ivRecordVideo.setEnableView(alpha, enable)
//            ivLandRecordVideo.setEnableView(alpha, enable)
//            tvChangeQualityVideo.setEnableView(alpha, enable)
//            tvLandQualityVideo.setEnableView(alpha, enable)
        }
    }



    /**
     * Set quality value
     */
    private fun setQualityValue(quality: Int) {
        when (quality) {
            // Độ phân giải SD
            VNPT_CAMERA_RECORD_RESOLUTION_SD -> {
                binding.apply {
//                    tvChangeQualityVideo.text = getString(R.string.sd_video_quality)
//                    tvLandQualityVideo.text = getString(R.string.sd_video_quality)
                }
            }

            // Độ phân giải FHD
            VNPT_CAMERA_RECORD_RESOLUTION_FHD -> {
                binding.apply {
//                    tvChangeQualityVideo.text = getString(R.string.fhd_video_quality)
//                    tvLandQualityVideo.text = getString(R.string.fhd_video_quality)
                }
            }
        }
    }

    /**
     * Set view tương ứng với từng state của camera
     */
    private fun setViewStateCamera(stateCamera: Constant.CameraState) {
        when (stateCamera) {
            Constant.CameraState.Init -> {
//                setViewInitializedCamera()
            }

            Constant.CameraState.CameraOnline -> {
//                binding.apply {
//                    if (ivReceiveAudio.isSelected || ivLandReceiveAudio.isSelected) {
//                        onOffAudioPlayback(true)
//                    }
//                }
            }

            Constant.CameraState.CameraConnectionTimeOut,
            Constant.CameraState.CameraOffline -> {
//                setViewCameraOffline()
            }

            Constant.CameraState.CameraLossConnection -> {
//                setViewInitializedCamera()
            }

            else -> {}
        }
    }

    private fun onCommonViewLoaded() {
        initGlSurfaceView()
        dayAdapter = DayPlaybackAdapter()
        binding.recyclerView.apply {
            adapter = dayAdapter
        }
        setUpPeriodTimeForTimeline(timeStamp)
        mainViewModel.apply {
            if (timeStamp != null && timeStamp != 0L) {
                Log.d("ducpa", "vào event NOTIFICATION")
                //Trỏ đến nơi phát hiện chuyển động
                setEventToPlay(Constant.PlayPlaybackFileEvent.NOTIFICATION)
                setTimeSeekValue(timeStamp)
            } else {
                Log.d("ducpa", "Vào playback lần đầu")
                //Vào playback lần đầu
                setEventToPlay(Constant.PlayPlaybackFileEvent.FIRST_TIME)
                setUpPeriodTimeForTimeline(firstTimeComeIn)
            }

            prepareAndInitializeCamera(getDataCamera(),uid)

            val a: SchedulerProvider = SchedulerProviderImpl()
            val b: ResourcesService  = ResourcesServiceImpl(requireContext())
            mainViewModel.init(
                a, b
            )
        }

    }

    /**
     * Initialize GLSurfaceview
     */
    private fun initGlSurfaceView() {
        binding.apply {
            glSurfaceview.apply {
                setMinScale(1f)
                setMaxScale(10f)
                setScrollView(scroolview)
                setSwipeListener(this@PlaybackCameraFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSwipeRight() {
    }

    override fun onSwipeLeft() {
    }

    override fun onSwipeTop() {
    }

    override fun onSwipeBottom() {
    }

    override fun onSingleTapUp(p0: MotionEvent?) {
    }

    override fun onStart(cursorValue: Long) {
        Log.d("ducpa", "onStart: $onTouchTimeBar")
        Log.d("ducpa", "nhảy vào onStart")
        if (!onTouchTimeBar) {
            onTouchTimeBar = true
            mainViewModel.seekTimePlaybackFile()
        }

        startSeekVideo.cancel()
    }

    override fun onMoving(cursorValue: Long) {
    }

    override fun onEnd(cursorValue: Long) {
        Log.d("ducpa", "onEnd cursorValue: $cursorValue")
        Log.d("ducpa", "nhảy vào onEnd")
        if (onTouchTimeBar) {
            mainViewModel.setTimeSeekValue(cursorValue)
            startCountdownSeekVideo()
        }
    }

    private fun startCountdownSeekVideo() {
        startSeekVideo.cancel()
        startSeekVideo.start()
    }

    // Countdowntimer để bắt đầu tua video
    // Khi kéo thanh timeline, sau khi nhả tay ra, delay 1s sau đó mới bắt đầu gọi lệnh seek xuống cam
    private val startSeekVideo = object : CountDownTimer(1000, 1000) {
        override fun onTick(p0: Long) {}

        override fun onFinish() {
            onTouchTimeBar = false
            updateViewStartSeekVideoPlayback()
            mainViewModel.startSeekVideo()
            showLoading(false)
        }
    }

    /**
     * Cập nhật view khi bắt đầu quá trình seek video playback
     */
    private fun updateViewStartSeekVideoPlayback() {
        showLoading(true)
        updateTimebarView(false)
    }

    /**
     * Cập nhật view của thanh giời gian (timeline), thanh chọn ngày, textview chọn ngày xem playback
     */
    private fun updateTimebarView(enable: Boolean) {

        binding.apply {
            timeLinePlayback.apply {
                setEnableView(1F, true)
                setTouchable(true)
            }
            recyclerView.setEnableView(1F, true)

          dayAdapter.setPickableDay(enable)
        }
    }

    private fun View.setEnableView(alphaValue: Float, isEnable: Boolean) {
        isEnabled = isEnable
        alpha = if (!isEnable) alphaValue else 1.0F
        isClickable = isEnable
    }

    private fun showLoading(enable: Boolean) {
        binding.ivLoading.visibility = if (enable) View.VISIBLE else View.GONE
    }
}

