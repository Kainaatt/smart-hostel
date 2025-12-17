package com.example.complaintapp.repository

import com.example.complaintapp.data.User
import com.example.complaintapp.util.Constants
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            Result.success(result.user ?: throw Exception("Login failed"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        name: String,
        studentId: String,
        room: String
    ): Result<FirebaseUser> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Sign up failed")
            
            // Save user profile to Firestore
            val userData = User(
                id = user.uid,
                name = name,
                email = email,
                studentId = studentId,
                room = room,
                isAdmin = false
            )
            
            db.collection(Constants.COLLECTION_USERS)
                .document(user.uid)
                .set(userData.toMap())
                .await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCurrentUserData(): Result<User> {
        return try {
            val currentUser = auth.currentUser
                ?: return Result.failure(Exception("No user logged in"))
            
            val document = db.collection(Constants.COLLECTION_USERS)
                .document(currentUser.uid)
                .get()
                .await()
            
            if (document.exists()) {
                Result.success(User.fromDocument(document))
            } else {
                Result.failure(Exception("User data not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isAdmin(): Boolean {
        return try {
            val userData = getCurrentUserData()
            userData.getOrNull()?.isAdmin ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun logout() {
        auth.signOut()
    }
}

