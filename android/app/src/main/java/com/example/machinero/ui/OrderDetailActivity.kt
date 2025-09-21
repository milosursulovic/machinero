package com.example.machinero.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.machinero.R
import com.google.android.material.textview.MaterialTextView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

class OrderDetailActivity : AppCompatActivity() {

    private val isoFmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val isoFmtNoMillis = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    private val srFmt = SimpleDateFormat("dd.MM.yyyy.", Locale("sr", "RS"))

    private fun parseIso(iso: String?): Date? {
        if (iso.isNullOrBlank()) return null
        return try {
            isoFmt.parse(iso)
        } catch (_: Throwable) {
            try {
                isoFmtNoMillis.parse(iso)
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun daysDiffFromToday(date: Date?): Int? {
        if (date == null) return null
        val calTarget = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val calToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val ms = calTarget.timeInMillis - calToday.timeInMillis
        return Math.round(ms / 86_400_000.0).toInt()
    }

    private fun setBadge(view: MaterialTextView, days: Int?) {
        val (text, colorRes) = when {
            days == null -> "Rok: —" to R.color.badge_neutral
            days < 0 -> "⏰ Kasni (${abs(days)} d)" to R.color.badge_red
            days == 0 -> "🔔 Danas" to R.color.badge_orange
            days == 1 -> "📦 Sutra" to R.color.badge_green
            else -> "📦 Za $days dana" to R.color.badge_green
        }
        view.text = text
        val bg = view.background as? GradientDrawable
        bg?.setColor(ContextCompat.getColor(this, colorRes))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_order_detail)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tvNotifTitle = findViewById<MaterialTextView>(R.id.tvNotifTitle)
        val tvNotifBody = findViewById<MaterialTextView>(R.id.tvNotifBody)
        val tvDueBadge = findViewById<MaterialTextView>(R.id.tvDueBadge)
        val tvOrderId = findViewById<MaterialTextView>(R.id.tvOrderId)
        val tvStatus = findViewById<MaterialTextView>(R.id.tvStatus)
        val tvCustomer = findViewById<MaterialTextView>(R.id.tvCustomer)
        val tvDeliveryDate = findViewById<MaterialTextView>(R.id.tvDeliveryDate)
        val tvDaysLeft = findViewById<MaterialTextView>(R.id.tvDaysLeft)

        val orderId = intent.getStringExtra("orderId") ?: "-"
        val status = intent.getStringExtra("status") ?: "-"
        val customer = intent.getStringExtra("customer") ?: "-"
        val deliveryIso = intent.getStringExtra("deliveryDate")
        val notifTitle = intent.getStringExtra("_title") ?: "Obaveštenje"
        val notifBody = intent.getStringExtra("_body") ?: ""

        tvNotifTitle.text = notifTitle
        tvNotifBody.text = notifBody

        tvOrderId.text = "Order ID: $orderId"
        tvStatus.text = "Status: $status"
        tvCustomer.text = "Kupac: $customer"

        val deliveryDate = parseIso(deliveryIso)
        tvDeliveryDate.text = "Rok isporuke: ${
            if (deliveryDate != null) srFmt.format(deliveryDate) else "—"
        }"

        val days = daysDiffFromToday(deliveryDate)
        tvDaysLeft.text = "Preostalo dana: ${days?.toString() ?: "—"}"
        setBadge(tvDueBadge, days)
    }
}
