package com.example.complaintapp.repository

import com.example.complaintapp.data.Complaint
import com.example.complaintapp.util.Constants
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ComplaintRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun saveComplaint(complaint: Complaint): Result<String> {
        return try {
            val docRef = db.collection(Constants.COLLECTION_COMPLAINTS)
                .add(complaint.toMap())
                .await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComplaintById(complaintId: String): Result<Complaint> {
        return try {
            val document = db.collection(Constants.COLLECTION_COMPLAINTS)
                .document(complaintId)
                .get()
                .await()
            
            if (document.exists()) {
                Result.success(Complaint.fromDocument(document))
            } else {
                Result.failure(Exception("Complaint not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserComplaints(userId: String): Result<List<Complaint>> {
        return try {
            val snapshot = db.collection(Constants.COLLECTION_COMPLAINTS)
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val complaints = snapshot.documents.map { Complaint.fromDocument(it) }
            Result.success(complaints)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getUserComplaintsFlow(userId: String): Flow<List<Complaint>> = callbackFlow {
        val listenerRegistration = db.collection(Constants.COLLECTION_COMPLAINTS)
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val complaints = snapshot?.documents?.map { Complaint.fromDocument(it) } ?: emptyList()
                trySend(complaints)
            }
        
        awaitClose { listenerRegistration.remove() }
    }

    suspend fun getAllComplaints(): Result<List<Complaint>> {
        return try {
            val snapshot = db.collection(Constants.COLLECTION_COMPLAINTS)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val complaints = snapshot.documents.map { Complaint.fromDocument(it) }
            Result.success(complaints)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComplaintsByStatus(status: String): Result<List<Complaint>> {
        return try {
            val snapshot = db.collection(Constants.COLLECTION_COMPLAINTS)
                .whereEqualTo("status", status)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val complaints = snapshot.documents.map { Complaint.fromDocument(it) }
            Result.success(complaints)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComplaintsByUrgency(urgency: String): Result<List<Complaint>> {
        return try {
            val snapshot = db.collection(Constants.COLLECTION_COMPLAINTS)
                .whereEqualTo("urgency", urgency)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val complaints = snapshot.documents.map { Complaint.fromDocument(it) }
            Result.success(complaints)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateComplaintStatus(
        complaintId: String,
        status: String,
        adminNotes: String = ""
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to System.currentTimeMillis()
            )
            
            if (adminNotes.isNotEmpty()) {
                updates["adminNotes"] = adminNotes
            }
            
            db.collection(Constants.COLLECTION_COMPLAINTS)
                .document(complaintId)
                .update(updates)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getActiveComplaints(userId: String): Result<List<Complaint>> {
        return try {
            val snapshot = db.collection(Constants.COLLECTION_COMPLAINTS)
                .whereEqualTo("userId", userId)
                .whereIn("status", listOf(Constants.STATUS_PENDING, Constants.STATUS_IN_PROGRESS))
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val complaints = snapshot.documents.map { Complaint.fromDocument(it) }
            Result.success(complaints)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

