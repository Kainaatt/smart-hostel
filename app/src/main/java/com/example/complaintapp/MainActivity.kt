package com.example.complaintapp

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
        // Submit Complaint Button
        findViewById<MaterialButton>(R.id.btnSubmitComplaint).setOnClickListener {
            showToast("Submit complaint clicked - Coming soon!")
        }

        // Quick Action Cards
        findViewById<MaterialCardView>(R.id.cardViewHistory).setOnClickListener {
            showToast("View history - Coming soon!")
        }

        findViewById<MaterialCardView>(R.id.cardTrackStatus).setOnClickListener {
            showToast("Track status - Coming soon!")
        }

        // Category Cards
        findViewById<MaterialCardView>(R.id.cardElectricity).setOnClickListener {
            showToast("Electricity complaint - Coming soon!")
        }

        findViewById<MaterialCardView>(R.id.cardWater).setOnClickListener {
            showToast("Water & Sanitation complaint - Coming soon!")
        }

        findViewById<MaterialCardView>(R.id.cardMaintenance).setOnClickListener {
            showToast("Maintenance complaint - Coming soon!")
        }

        findViewById<MaterialCardView>(R.id.cardCleanliness).setOnClickListener {
            showToast("Cleanliness complaint - Coming soon!")
        }

        findViewById<MaterialCardView>(R.id.cardStaff).setOnClickListener {
            showToast("Staff behavior complaint - Coming soon!")
        }

        // View All Recent Complaints
        findViewById<View>(R.id.tvViewAll).setOnClickListener {
            showToast("View all complaints - Coming soon!")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

