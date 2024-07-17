package team.eyenami.obj

/**
 * 속성 변경 알람 리스너
 */
interface PropertiesChangeListener {


    /**
     * 속성 업데이트
     * @param message PropertiesChangeMessage       업데이트 메시지
     */
    fun update(message : PropertiesChangeMessage)

}