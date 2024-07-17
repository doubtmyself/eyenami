package team.eyenami.obj

import androidx.annotation.IntDef
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * 속성(필드) 변경 업데이트 알림 메시지
 * @property src Any                업데이트 된 정보
 * @property propertiesName String  업데이트 된 속성명
 * @constructor
 */
class PropertiesChangeMessage{

    constructor(propertiesName : Int , src: Any){
        this.propertiesName = propertiesName
        this.src = src
    }

    constructor(propertiesName : Int , src : Any , oldValue : Any){
        this.propertiesName = propertiesName
        this.src = src
        this.oldValue = oldValue
    }

    @PROP var propertiesName: Int = 0
    var src: Any? = null
    var oldValue : Any? = null
//    @IntDef(
//        Define.PROP_CONNECTION_STATE, Define.PROP_BATTERY, Define.PROP_ELECTRODE_STATUS, Define.PROP_FALLING_STATUS, Define.PROP_MAC_ADDRESS,
//        Define.PROP_PICTURE_URI, Define.PROP_ULCER_STATUS, Define.PROP_MAIN_NOTY_FLAG, Define.PROP_FW_VERSION, Define.PROP_FW_PID,
//        Define.PROP_DIAPER_TYPE_CHANGE, Define.PROP_NAME, Define.PROP_BIRTH, Define.PROP_HEIGHT, Define.PROP_WEIGHT,
//        Define.PROP_GENDER, Define.PROP_ALERT_URINE_VOLUME, Define.PROP_WAIT_ACK_HRM, Define.PROP_WAIT_ACK_TEMP, Define.PROP_DATA_STRESS,
//        Define.PROP_SPO2, Define.PROP_DATA_URINE_LIST, Define.PROP_DATA_STOOL_LIST, Define.PROP_DATA_HRM_LIST, Define.PROP_DATA_TEMP_LIST,
//        Define.PROP_DATA_STEP_LIST, Define.PROP_DATA_DIAPER_LIST, Define.PROP_DATA_POSITION_LIST, Define.PROP_CREATE, Define.PROP_REMOVE,
//        Define.PROP_CURRENT_PROFILE_ID, Define.PROP_DATA_SPO2_LIST , Define.PROP_DATA_POSITION_NOW_LIST, Define.PROP_STOOL_WARMUP_REMAIN_TIME
//           )
    @Retention(RetentionPolicy.SOURCE)
    annotation class PROP {}
}
