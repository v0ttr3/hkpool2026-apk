package com.hkpool.app.data
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface ResultDao {
    @Query("SELECT * FROM hk_results ORDER BY date DESC")
    fun getAllFlow(): Flow<List<ResultEntity>>
    @Query("SELECT * FROM hk_results ORDER BY date DESC")
    suspend fun getAll(): List<ResultEntity>
    @Query("SELECT * FROM hk_results WHERE date BETWEEN :from AND :to ORDER BY date ASC")
    suspend fun getBetween(from: String, to: String): List<ResultEntity>
    @Query("SELECT * FROM hk_results ORDER BY date DESC LIMIT 1")
    suspend fun getLast(): ResultEntity?
    @Query("SELECT * FROM hk_results WHERE substr(date,1,7)=:month ORDER BY date ASC")
    suspend fun getByMonth(month: String): List<ResultEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ResultEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ResultEntity)
    @Query("DELETE FROM hk_results WHERE date = :date")
    suspend fun deleteByDate(date: String)
    @Query("DELETE FROM hk_results")
    suspend fun clearAll()
}
