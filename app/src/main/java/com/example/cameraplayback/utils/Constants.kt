package com.example.cameraplayback.utils

object Constant {
    const val EMPTY_STRING = ""
    const val PHP_SERVER: String =
        "GlzOOoa2PnMOPRXL5y83+Oa1QmuG1Znr0M9DII6iwy4+pxG5bmmyBhh7EzeJwIhK"
    const val BLANSEARCH = (0x7A).toChar()

    /**
     * this is define of Camera error code constants
     */
    enum class VNPTCameraStatusCode(val value: Int) {
        NotInitialized(-1), // Camera chưa được khởi tạo
        ConnectSuccessful(0), // Connect camera thành công
        AlreadyInitialized(-2), // Đã tồn tại một phiên khởi tạo camera
        TimeoutConnection(-3), // Connect camera hết timeout
        InvalidID(-4), // Id Camera không chính xác
        InvalidParams(-5), // Truyển params sai
        Offline(-6), // Camera hiện đang offline
        InvalidIDPrefix(-8), // UID Camera không phù hợp với máy chủ P2P
        SessionClosedRemote(-12), // Phiên làm việc của camera hết hạn
        SessionClosedTimeout(-13), // Phiên làm việc bị timeout
        SessionClosedCalled(-14), // Phiên làm việc bị đóng
        ConnectUnsuccessful(1), // Camera kết nối không thành công
        NoDeviceFoundOrDeadServer(-90),
        ClientPasswordInvalid(-145),    // camera's password is invalid
        UidNotLicenced(-10),
        SessionAlreadyConnected(-128),    // Hiện tại đang trong phiên connect
        DeviceRequestExpired(-130),
        LossConnection(-99), // Define riêng cho case loss connection nhưng để phân biệt với mode offline khác
        ConnectTCPReplayFailed(-24)
    }

    object ConfigCamera {
        const val MOTION_DETECTION = "motion_detection"
        const val ALARM_TO_PHONE = "alarm_to_phone"
        const val ALARM_VOLUME = "alarm_volume"
        const val VOLUMES = "volumes"
        const val TIMEZONE = "timezone"
        const val TIMEZONE_ID = "timezone_id"
        const val TIMEZONE_VALUE = "timezone_value"
        const val RECORD_MODE = "record_mode"
        const val SLEEP_MODE = "sleep_mode"
        const val WIFI_SSID = "wifi_ssid"
        const val WIFI_MAC = "wifi_mac"
        const val WIFI_IP = "wifi_ip"
        const val STORAGE = "storage"
        const val SD_CARD_RESOLUTION = "sdcard_resolution"
        const val SDCARD_TOTAL = "sdcard_total"
        const val SDCARD_FREE = "sdcard_free"
        const val SCHEDULER_TIME = "schedule_time"
        const val SLEEP_MODE_ON = "0"
        const val SLEEP_MODE_OFF = "1"
        const val MOTION_DETECTION_AREA = "motion_detection_area"
        const val SCHEDULE_NOTIFICATION = "schedule_notification"
        const val NOTIFICATION_FREQUENCY = "notification_frequency"

        const val MAC = "mac"
        const val IP = "ip"
        const val CAMERA_TIME_ZONE_DEFAULT = 420
        const val CAMERA_VOLUME_CAMERA_DEFAULT = 100
        const val CAMERA_TYPE_DEFAULT = 1
        const val CAMERA_TIMEZONE_ID_DEFAULT = 99
        const val CAMERA_TIME_ZONE_VALUE_DEFAULT = "UTC+07"
        const val CAMERA_SLEEP_MODE_DEFAULT = 1
        const val CAMERA_RESOLUTION_FULL_HD = 3
        const val CAMERA_RESOLUTION_FULL_SD = 1
        const val CAMERA_PASSWORD = "12345678"
        const val CAMERA_DEFAULT_PASSWORD = "888888"
        const val CAMERA_ACCOUNT = "admin"
        const val CAMERA_CONFIRM_OKE = "ok"
        const val CAMERA_SCREENSHOT_EXTENSION = ".jpg"
        const val CAMERA_RECORDING_VIDEO_EXTENSION = ".mp4"
        const val CAMERA_WATERMARK_MODE = "watermark_mode"
        const val CAMERA_WATERMARK_TEXT = "watermark_text"
        const val ON = "on"
        const val OFF = "off"
        const val VNPT_CAMERA_DEFAULT_PASSWORD = "vnpt@123"
        const val VNPT_CAMERA_RECORD_RESOLUTION_SD = 1
        const val VNPT_CAMERA_RECORD_RESOLUTION_FHD = 0
        const val VNPT_RECORD_TYPE_OFF = -1
        const val VNPT_RECORD_TYPE_ALWAYS = 0
        const val VNPT_RECORD_TYPE_MOTION_DETECT = 2
        const val VNPT_RECORD_TYPE_SCHEDULE = 1
        const val VNPT_CUSTOM_SUBSCRIPTION_ACTIVE_TO_PAUSE = 789
        const val VNPT_CUSTOM_SUBSCRIPTION_PAUSE_TO_ACTIVE = 987
        const val VNPT_CUSTOM_CAMERA_STAY_IN_SUBSCRIPTION_BUT_PAIRED = 654
    }

    /**
     * this is define of Camera state constants
     */
    enum class CameraState(val value: String) {
        Null("NULL"),
        Init("INIT"),
        CameraOnline("ONLINE"),
        CameraOffline("OFFLINE"),
        CameraSleep("SLEEP"),
        CameraReconnect("RECONNECT"),
        CameraNotInitialized("CameraNotInitialized"),
        CameraInvalidUid("INVALID_UID"),
        CameraConnectionTimeOut("CameraConnectionTimeOut"),
        CameraSessionClosed("CameraSessionClosed"),
        InvalidIDPrefix("InvalidIDPrefix"),
        CameraScreenShot("SCREEN_SHOT"),
        CameraRecordVideo("RECORD_VIDEO"),
        ScreenShotVideoManager("SCREEN_SHOT_VIDEO_MANAGER"),
        PlaybackSdCardStart("PLAYBACK_SDCARD_START"),
        PlaybackLoadmore("PLAYBACK_LOAD_MORE"),
        CameraIsUpdatingFirmware("IS_UPDATING_FIRM_WARE"),
        PlaybackVideoCloud("PLAYBACK_VIDEO_CLOUD"),
        PlaybackEmptySdcard("PLAYBACK_EMPTY_SDCARD"),
        CameraLossConnection("CAMERA_LOSS_CONNECTION"),
        PlaybackDebounceLoading("PLAYBACK_DEBOUNCE_LOADING"),
        PlaybackSdCardNoData("PLAYBACK_SDCARD_NOPLAYBACK_DATA"),
        CameraRecordVideoH265("H265_VIDEO"),
    }

    /**
     * Các state tương ứng của playback sd để update UI
     */
    enum class PlaybackSdCardStateUI {
        PLAYING,
        PAUSED,          // Hoàn tất pause
        CONTINUE_PLAY,  // Trạng thái play từ pause -> play lại
        PAUSING,        // Đang trong quá trình pause
        SEEK,           // Trạng thái tua
        NEXT_FILE,      // Trạng thái next sang file mới trong cùng 1 ngày
        SELECT_DAY,     // Chọn ngày xem playback
        EMPTY_FILE,      // Ngày phát lại không có dữ liệu
        SCAN,
        EMPTY_FILE_ALL_DAY,
        NEXT_DAY,
        COMPLETE,
        EMPTY_FILE_NOTIFICATION
    }

    enum class PlayPlaybackFileEvent {
        FIRST_TIME,         // Thời điểm đầu tiên vào playback
        SEEK,               // Tua file
        NEXT_FILE,          // Next sang file mới trong cùng 1 ngày
        SELECT_DAY,         // Chọn ngày xem playback
        SCAN_PREVIOUS,      // Quét về quá khứ
        SCAN_NEXT,          // Quét về tương lai
        NEXT_DAY,            // Next sang ngày mới khi đã play hết ngày hiện tại
        PAUSE,
        NOTIFICATION //Xem thông báo phát hiện chuyển động
    }
}