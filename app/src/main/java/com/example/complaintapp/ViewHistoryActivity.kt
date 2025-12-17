package com.example.complaintapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.complaintapp.data.Complaint
import com.example.complaintapp.repository.AuthRepository
import com.example.complaintapp.repository.ComplaintRepository
import com.example.complaintapp.util.Constants
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ViewHistoryActivity : AppCompatActivity() {

    private lateinit var rvComplaints: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var chipGroup: ChipGroup
    private lateinit var progressBar: ProgressBar
    
    private val authRepository = AuthRepository()
    private val complaintRepository = ComplaintRepository()
    private var allComplaints = listOf<Complaint>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_view_history)

        initViews()
        setupClickListeners()
        setupRecyclerView()
        loadComplaints()
    }

    private fun initViews() {
        rvComplaints = findViewById(R.id.rvComplaints)
        emptyState = findViewById(R.id.emptyState)
        chipGroup = findViewById(R.id.chipGroup)
        progressBar = ProgressBar(this).apply {
            visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        findViewById<FrameLayout>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Filter chips
        chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chipId = checkedIds[0]
                filterComplaints(chipId)
            }
        }
    }

    private fun setupRecyclerView() {
        rvComplaints.layoutManager = LinearLayoutManager(this)
        rvComplaints.adapter = ComplaintAdapter(emptyList()) { complaint ->
            openComplaintDetail(complaint)
        }
    }

    private fun loadComplaints() {
        val currentUser = authRepository.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "Please login again", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = complaintRepository.getUserComplaints(currentUser.uid)
            
            progressBar.visibility = View.GONE

            result.onSuccess { complaints ->
                allComplaints = complaints
                (rvComplaints.adapter as ComplaintAdapter).updateList(complaints)
                updateStats()
                
                if (complaints.isEmpty()) {
                    rvComplaints.visibility = View.GONE
                    emptyState.visibility = View.VISIBLE
                } else {
                    rvComplaints.visibility = View.VISIBLE
                    emptyState.visibility = View.GONE
                }
            }.onFailure { exception ->
                Toast.makeText(this@ViewHistoryActivity, "Failed to load complaints: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun filterComplaints(chipId: Int) {
        val filteredList = when (chipId) {
            R.id.chipAll -> allComplaints
            R.id.chipPending -> allComplaints.filter { it.status == Constants.STATUS_PENDING }
            R.id.chipInProgress -> allComplaints.filter { it.status == Constants.STATUS_IN_PROGRESS }
            R.id.chipResolved -> allComplaints.filter { it.status == Constants.STATUS_RESOLVED }
            else -> allComplaints
        }

        (rvComplaints.adapter as ComplaintAdapter).updateList(filteredList)
        
        if (filteredList.isEmpty()) {
            rvComplaints.visibility = View.GONE
            emptyState.visibility = View.VISIBLE
        } else {
            rvComplaints.visibility = View.VISIBLE
            emptyState.visibility = View.GONE
        }
    }

    private fun updateStats() {
        findViewById<TextView>(R.id.tvTotalCount).text = allComplaints.size.toString()
        findViewById<TextView>(R.id.tvPendingCount).text = allComplaints.count { it.status == Constants.STATUS_PENDING }.toString()
        findViewById<TextView>(R.id.tvResolvedCount).text = allComplaints.count { it.status == Constants.STATUS_RESOLVED }.toString()
    }

    private fun openComplaintDetail(complaint: Complaint) {
        val intent = Intent(this, ComplaintDetailActivity::class.java).apply {
            putExtra(ComplaintDetailActivity.EXTRA_COMPLAINT_ID, complaint.id)
            putExtra(ComplaintDetailActivity.EXTRA_CATEGORY, complaint.category)
            putExtra(ComplaintDetailActivity.EXTRA_TITLE, complaint.title)
            putExtra(ComplaintDetailActivity.EXTRA_DESCRIPTION, complaint.description)
            putExtra(ComplaintDetailActivity.EXTRA_STATUS, complaint.status)
            putExtra(ComplaintDetailActivity.EXTRA_URGENCY, complaint.urgency)
            putExtra(ComplaintDetailActivity.EXTRA_LOCATION, complaint.location)
            putExtra(ComplaintDetailActivity.EXTRA_DATE, dateFormat.format(java.util.Date(complaint.createdAt)))
        }
        startActivity(intent)
    }

    // Adapter
    inner class ComplaintAdapter(
        private var complaints: List<Complaint>,
        private val onClick: (Complaint) -> Unit
    ) : RecyclerView.Adapter<ComplaintAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val categoryIconFrame: FrameLayout = view.findViewById(R.id.categoryIconFrame)
            val ivCategoryIcon: ImageView = view.findViewById(R.id.ivCategoryIcon)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvCategory: TextView = view.findViewById(R.id.tvCategory)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvComplaintId: TextView = view.findViewById(R.id.tvComplaintId)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_complaint, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val complaint = complaints[position]

            holder.tvTitle.text = complaint.title
            holder.tvDate.text = dateFormat.format(java.util.Date(complaint.createdAt))
            holder.tvComplaintId.text = complaint.id

            // Set category
            val (iconRes, categoryName, bgRes) = getCategoryResources(complaint.category)
            holder.ivCategoryIcon.setImageResource(iconRes)
            holder.categoryIconFrame.setBackgroundResource(bgRes)
            holder.tvCategory.text = getString(categoryName)

            // Set status
            val (statusText, statusColor, statusBg) = getStatusResources(complaint.status)
            holder.tvStatus.text = getString(statusText)
            holder.tvStatus.setTextColor(ContextCompat.getColor(this@ViewHistoryActivity, statusColor))
            holder.tvStatus.setBackgroundResource(statusBg)

            holder.itemView.setOnClickListener { onClick(complaint) }
        }

        override fun getItemCount() = complaints.size

        fun updateList(newList: List<Complaint>) {
            complaints = newList
            notifyDataSetChanged()
        }

        private fun getCategoryResources(category: String): Triple<Int, Int, Int> {
            return when (category) {
                "electricity" -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
                "water" -> Triple(R.drawable.ic_water, R.string.water, R.drawable.bg_icon_water)
                "maintenance" -> Triple(R.drawable.ic_maintenance, R.string.maintenance, R.drawable.bg_icon_maintenance)
                "cleanliness" -> Triple(R.drawable.ic_cleanliness, R.string.cleanliness, R.drawable.bg_icon_cleanliness)
                "staff" -> Triple(R.drawable.ic_staff, R.string.staff, R.drawable.bg_icon_staff)
                else -> Triple(R.drawable.ic_electricity, R.string.electricity, R.drawable.bg_icon_electricity)
            }
        }

        private fun getStatusResources(status: String): Triple<Int, Int, Int> {
            return when (status) {
                Constants.STATUS_PENDING -> Triple(R.string.pending, R.color.urgency_medium, R.drawable.bg_urgency_medium)
                Constants.STATUS_IN_PROGRESS -> Triple(R.string.in_progress, R.color.category_water, R.drawable.bg_status_in_progress)
                Constants.STATUS_RESOLVED -> Triple(R.string.resolved, R.color.urgency_low, R.drawable.bg_urgency_low)
                else -> Triple(R.string.pending, R.color.urgency_medium, R.drawable.bg_urgency_medium)
            }
        }
    }
}

