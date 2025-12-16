package com.example.complaintapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        setContentView(R.layout.activity_main)
        
        setupClickListeners()
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
