package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CodeReviewDao {
    @Query("SELECT * FROM code_reviews ORDER BY timestamp DESC")
    fun getAllReviews(): Flow<List<CodeReview>>

    @Query("SELECT * FROM code_reviews WHERE id = :id LIMIT 1")
    suspend fun getReviewById(id: Int): CodeReview?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: CodeReview): Long

    @Query("DELETE FROM code_reviews WHERE id = :id")
    suspend fun deleteReviewById(id: Int)

    @Query("DELETE FROM code_reviews")
    suspend fun clearAllReviews()
}
