package com.example.data

import kotlinx.coroutines.flow.Flow

class CodeReviewRepository(private val codeReviewDao: CodeReviewDao) {
    val allReviews: Flow<List<CodeReview>> = codeReviewDao.getAllReviews()

    suspend fun getReviewById(id: Int): CodeReview? {
        return codeReviewDao.getReviewById(id)
    }

    suspend fun insertReview(review: CodeReview): Long {
        return codeReviewDao.insertReview(review)
    }

    suspend fun deleteReviewById(id: Int) {
        codeReviewDao.deleteReviewById(id)
    }

    suspend fun clearAllReviews() {
        codeReviewDao.clearAllReviews()
    }
}
