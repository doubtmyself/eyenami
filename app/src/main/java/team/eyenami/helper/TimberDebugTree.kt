package team.eyenami.helper

import timber.log.Timber



class TimberDebugTree : Timber.DebugTree() {
    private val filterName = "TIMBER"
    override fun createStackElementTag(element: StackTraceElement): String {
        return "(${element.fileName}:${element.lineNumber})"
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, tag, "${filterName}$message", t)
    }
}