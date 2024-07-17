package team.eyenami.helper

import timber.log.Timber



/**
 * 로그를 편리하게 찍기위해 기존 Timber을 변경한 함수
 * 자동으로 로그가 발생할 클래스의 위치를 테크로 설정한다.
 * @property filterName String              Log를 필터링 해서 볼때 TIMBER로 등록된 로그만 필터링 할수 있게 추가된 문구
 * @property logHelper LogTimberHelper      Log write 시 파일로 저장하기 위한 클래스
 */
class TimberDebugTree : Timber.DebugTree() {
    private val filterName = "TIMBER"
    override fun createStackElementTag(element: StackTraceElement): String {
        return "(${element.fileName}:${element.lineNumber})"
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, "${filterName}$message", t)
    }
}