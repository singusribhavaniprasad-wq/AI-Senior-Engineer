package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "code_reviews")
data class CodeReview(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val language: String,
    val projectType: String,
    val reviewMode: String,
    val codeSnippet: String,
    val reportContent: String,
    val score: Float, // Overall score calculated or extracted
    val timestamp: Long = System.currentTimeMillis()
)
