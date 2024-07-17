package team.eyenami.obj.localDB

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Update



/**
 * 공통된 사용 쿼리 관리 클래스
 * 제너릭으로 해당 DBM에 대한 쿼리를 정의한다
 * @param T         DB 의 Entity(DBM:database model)가 제너릭으로 정의되어 해당 쿼리를 실행함
 */
@Dao
interface BaseDao<T> {
    /**
     * 테이블의 컬럼 insert
     * @param data T
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(data: T)

    /**
     * 테이블의 여려 컬럼 insert
     * @param data T
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(vararg obj: T)
    /**
     * 테이블 컬럼 업데이트
     * @param date T            업데이트 데이터
     */
    @Update
    suspend fun update(date: T)

    /**
     * 테이블 컬럼 업데이트
     * @param date T            업데이트 데이터
     */
    @Update
    fun updateWait(date: T)

    /**
     * 테이블 컬럼 삭제
     * @param date List<T>      삭제할 데이터 리스트
     */
    @Delete
    suspend fun delete(date: T)
}