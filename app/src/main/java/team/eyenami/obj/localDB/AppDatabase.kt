package team.eyenami.obj.localDB

import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import team.eyenami.MainApplication

/**
 * SQLiteDatabase 에 접근하기 쉽게 만든 RoomDB 라이브러리 추상 클래스 구현
 */
@Database(
    entities = [SettingDBM::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
//    abstract fun rxProfileDao(): ProfileDao
    abstract fun settingDao(): SettingDao
//    abstract fun dataUrineDao(): DataUrineDao
//    abstract fun dataStoolDao(): DataStoolDao
//    abstract fun dataDiaperDao(): DataDiaperDao
//    abstract fun dataHRMDao(): DataHRMDao
//    abstract fun dataPostureDao(): DataPostureDao
//    abstract fun dataPostureNowDao(): DataPostureNowDao
//    abstract fun dataSPO2Dao(): DataSpo2Dao
//    abstract fun dataStepDao(): DataStepDao
//    abstract fun dataTempDao(): DataTempDao


    companion object {
        /**
         * DB 설정 테이블
         */
//        const val PROFILE_TABLE = "ProfileTable"
        const val SETTING_TABLE = "SettingTable"
//        const val URINE_TABLE = "UrineTable"
//        const val TEMP_TABLE = "TempTable"
//        const val STOOL_TABLE = "StoolTable"
//        const val STEP_TABLE = "StepTable"
//        const val SPO2_TABLE = "SPO2Table"
//        const val HRM_TABLE = "HrmTable"
//        const val DIAPER_TABLE = "DiaperTable"
//        const val POSTURE_TABLE="postureTable"
//        const val POSTURE_NOW_TABLE="postureTableNow"

        @Volatile   //메인 메모리에만 적제됨
        private var instance: AppDatabase? = null

        @JvmStatic
        fun getInstance(): AppDatabase {
            instance ?: synchronized(this) { // 존재하지 않는다면 sychronized
//                val passphrase = BuildConfig.ROOM_KEY.toCharArray()
//                val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase))
                instance = databaseBuilder(MainApplication.appContext, AppDatabase::class.java, "database")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build()
            }

            //fallbackToDestructiveMigration() 버전 갱신시 이전 버전 데이터를 마이그레이션시키지 않고 삭제후 디비 생성
            return instance!!
        }
    }
}