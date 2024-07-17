package team.eyenami

import android.app.Application
import android.content.Context
import team.eyenami.helper.TimberDebugTree
import team.eyenami.obj.PropertiesChangeListener
import team.eyenami.obj.PropertiesChangeMessage
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


//        if (BuildConfig.DEBUG_MODE) {
//            Toast.makeText(applicationContext, "DEBUG_MODE", Toast.LENGTH_SHORT).show()
        Timber.plant(TimberDebugTree())
//        } else {
//            Toast.makeText(applicationContext, "Relese_MODE", Toast.LENGTH_SHORT).show()
//        }


        addListener()
        Timber.e("앱 시작")
    }

    private fun addListener() {

    }



    override fun update(message: PropertiesChangeMessage) {

    }









}
