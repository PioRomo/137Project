package com.example.pixelpedia

data class UserProfile(
    val userId: String,
    val username: String,
    val profilePicUrl: String,
    val likes: Int
){}