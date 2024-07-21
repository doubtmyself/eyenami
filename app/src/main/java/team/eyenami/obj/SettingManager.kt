package team.eyenami.obj

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import team.eyenami.utills.SettingProp
import team.eyenami.obj.localDB.AppDatabase
import team.eyenami.obj.localDB.SettingDBM
import java.util.concurrent.CopyOnWriteArrayList

object SettingManager {
    private val db: AppDatabase get() = AppDatabase.getInstance()
    private lateinit var settingDBM: SettingDBM
    private val propertiesChangeListener = CopyOnWriteArrayList<PropertiesChangeListener>()

    fun initialize() {
        runBlocking(Dispatchers.IO) {
            val loadData = db.settingDao().getAll() // 하나만 데이터가 들어갈 예정
            if (db.settingDao().getRowCount() == 0) {     // 최초 실행
                settingDBM = SettingDBM(1)
                db.settingDao().insert(settingDBM)
            }
            for (data in loadData) {
                settingDBM = data
                break
            }
        }
    }

    fun addPropertiesChangeListener(argValue: PropertiesChangeListener) {
        propertiesChangeListener.add(argValue)
    }

    fun removePropertiesChangeListener(argValue: PropertiesChangeListener) {
        propertiesChangeListener.remove(argValue)
    }

    fun setVibrateRun(flag: Boolean) = CoroutineScope(Dispatchers.IO).launch {
        settingDBM.vibrateRun = flag
        db.settingDao().update(settingDBM)
        notifyUpdate(PropertiesChangeMessage(SettingProp.VIBRATERUN, this))
    }

    fun getVibrateRun(): Boolean {
        return settingDBM.vibrateRun
    }


    fun getDetectionCount(): Int {
        return settingDBM.deletionCount
    }

    fun setDetectionCount(count : Int)  {
        settingDBM.deletionCount = count
    }

    fun getDetectionCountMS(): Long {
        return settingDBM.deletionCount.toLong() * 1000L
    }


    private fun notifyUpdate(message: PropertiesChangeMessage) {
        for (item in propertiesChangeListener) {
            CoroutineScope(Dispatchers.Main).launch {
                item.update(message)
            }
        }
    }
}