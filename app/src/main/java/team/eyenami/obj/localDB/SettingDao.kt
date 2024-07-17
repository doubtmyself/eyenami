package team.eyenami.obj.localDB

import androidx.room.*


@Dao
interface SettingDao : BaseDao<SettingDBM>{
    @Query("SELECT * FROM ${AppDatabase.SETTING_TABLE}")
    suspend fun getAll(): List<SettingDBM>

    @Query("SELECT COUNT(ID) FROM ${AppDatabase.SETTING_TABLE}")
    fun getRowCount(): Int

    @Query("DELETE FROM ${AppDatabase.SETTING_TABLE}")
    fun delete()
}