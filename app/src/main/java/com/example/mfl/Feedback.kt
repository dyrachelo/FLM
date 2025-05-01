// Feedback.kt
package com.example.mfl

data class Feedback(
    val id: String,          // Firebase ID (String)
    val userEmail: String,
    val message: String,
    val timestamp: Long,
    val status: String       // "pending", "resolved" и т.д.
)