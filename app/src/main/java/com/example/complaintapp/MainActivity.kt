package com.example.complaintapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.complaintapp.repository.AuthRepository
import com.example.complaintapp.repository.ComplaintRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()
    private val complaintRepository = ComplaintRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Check authentication
        if (authRepository.getCurrentUser() == null) {
            redirectToLogin()
            return
        }
        
        setContentView(R.layout.activity_main)
        
        setupClickListeners()
        loadUserData()
        loadRecentComplaints()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            val result = authRepository.getCurrentUserData()
            
            result.onSuccess { user ->
                findViewById<TextView>(R.id.tvUserName).text = user.name
            }.onFailure {
                // Keep default text if loading fails
            }
        }
    }

    private fun loadRecentComplaints() {
        val currentUser = authRepository.getCurrentUser() ?: return

        lifecycleScope.launch {
            val result = complaintRepository.getUserComplaints(currentUser.uid)
            
            result.onSuccess { complaints ->
                // Show recent complaints (last 3)
                val recentComplaints = complaints.take(3)
                displayRecentComplaints(recentComplaints)
            }.onFailure {
                // Silently fail - don't show error for empty list
            }
        }
    }

    private fun displayRecentComplaints(complaints: List<com.example.complaintapp.data.Complaint>) {
        // Recent complaints are displayed in the layout
        // This method can be extended later to populate a RecyclerView if needed
    }

    private fun setupClickListeners() {
        // Submit Complaint Button - opens submit screen without pre-selected category
        findViewById<MaterialButton>(R.id.btnSubmitComplaint).setOnClickListener {
            openSubmitComplaint()
        }

        // Quick Action Cards
        findViewById<MaterialCardView>(R.id.cardViewHistory).setOnClickListener {
            startActivity(Intent(this, ViewHistoryActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardTrackStatus).setOnClickListener {
            startActivity(Intent(this, TrackStatusActivity::class.java))
        }

        // Category Cards - open submit screen with pre-selected category
        findViewById<MaterialCardView>(R.id.cardElectricity).setOnClickListener {
            openSubmitComplaint("electricity")
        }

        findViewById<MaterialCardView>(R.id.cardWater).setOnClickListener {
            openSubmitComplaint("water")
        }

        findViewById<MaterialCardView>(R.id.cardMaintenance).setOnClickListener {
            openSubmitComplaint("maintenance")
        }

        findViewById<MaterialCardView>(R.id.cardCleanliness).setOnClickListener {
            openSubmitComplaint("cleanliness")
        }

        findViewById<MaterialCardView>(R.id.cardStaff).setOnClickListener {
            openSubmitComplaint("staff")
        }

        // View All Recent Complaints
        findViewById<View>(R.id.tvViewAll).setOnClickListener {
            startActivity(Intent(this, ViewHistoryActivity::class.java))
        }
    }

    private fun openSubmitComplaint(category: String? = null) {
        val intent = Intent(this, SubmitComplaintActivity::class.java)
        if (category != null) {
            intent.putExtra(SubmitComplaintActivity.EXTRA_CATEGORY, category)
        }
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
