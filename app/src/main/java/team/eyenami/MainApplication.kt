package team.eyenami

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import team.eyenami.helper.TimberDebugTree
import team.eyenami.obj.PropertiesChangeListener
import team.eyenami.obj.PropertiesChangeMessage
import team.eyenami.obj.SettingManager
import team.eyenami.utills.Util
import timber.log.Timber


class MainApplication : Application(), PropertiesChangeListener {
    companion object {
        private var _appContext: Context? = null
        @JvmStatic
        val appContext: Context get() = _appContext!!
    }


    override fun onCreate() {
        super.onCreate()
        _appContext = this
        Timber.plant(TimberDebugTree())

        initialize()

        addListener()
        Timber.e("앱 시작")
//        CoroutineScope(Dispatchers.IO).launch {
//            Util.getAbleModelList()
//        }


    }

    private fun initialize() {
        SettingManager.initialize()
    }

    private fun addListener() {

    }



    override fun update(message: PropertiesChangeMessage) {

    }









}
