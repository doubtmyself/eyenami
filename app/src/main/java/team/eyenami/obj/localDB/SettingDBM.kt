package team.eyenami.obj.localDB

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 *
 */
@Entity(
    tableName = AppDatabase.SETTING_TABLE,
       )
data class SettingDBM(
        @PrimaryKey(autoGenerate = true)
        @ColumnInfo(name = "ID")
        val ID: Int) {
    var vibrateRun :Boolean = false
}