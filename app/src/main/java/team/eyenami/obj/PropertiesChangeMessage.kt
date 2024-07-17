package team.eyenami.obj

import androidx.annotation.IntDef
import team.eyenami.define.SettingProp
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * 속성(필드) 변경 업데이트 알림 메시지
 * @property src Any                업데이트 된 정보
 * @property propertiesName String  업데이트 된 속성명
 * @constructor
 */
class PropertiesChangeMessage(public val propertiesName : SettingProp, public val src: Any){
}
