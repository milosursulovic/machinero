package com.example.machinero

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.machinero.data.AppDatabase
import com.example.machinero.data.NotificationEntity
import com.example.machinero.push.NotificationUtil
import com.example.machinero.ui.NotificationsAdapter
import com.example.machinero.ui.OrderDetailActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private val topic = "orders_all"

    private val requestNotifPerm = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Toast.makeText(
            this,
            if (granted) "Notifikacije dozvoljene ✅" else "Notifikacije blokirane ❌",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ensureNotifPermission()
        NotificationUtil.ensureChannel(this)

        val btnSub = findViewById<MaterialButton>(R.id.btnSubscribe)
        val btnUnsub = findViewById<MaterialButton>(R.id.btnUnsubscribe)
        val tvToken = findViewById<MaterialTextView>(R.id.tvToken)
        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvNotifications)
        val tvEmpty = findViewById<MaterialTextView>(R.id.tvEmpty)

        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            tvToken.text = if (it.isSuccessful) {
                "FCM token:\n${it.result}"
            } else {
                "Ne mogu da dobijem token: ${it.exception?.localizedMessage}"
            }
        }

        btnSub.setOnClickListener {
            FirebaseMessaging.getInstance().subscribeToTopic(topic).addOnCompleteListener { task ->
                Toast.makeText(
                    this,
                    if (task.isSuccessful) "Pretplaćen na $topic" else "Greška: ${task.exception?.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        btnUnsub.setOnClickListener {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    Toast.makeText(
                        this,
                        if (task.isSuccessful) "Otkačen sa $topic" else "Greška: ${task.exception?.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }

        val adapter = NotificationsAdapter { n -> openDetails(n) }
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        val dao = AppDatabase.getInstance(this).notificationDao()
        dao.getAllLive().observe(this, Observer { list ->
            adapter.submitList(list)
            tvEmpty.visibility = if (list.isNullOrEmpty()) View.VISIBLE else View.GONE
        })

        intent?.extras?.let { ex ->
            val maybeOrderId = ex.getString("orderId")
            if (!maybeOrderId.isNullOrBlank()) {
                openDetailsFromExtras(ex)
            }
        }
    }

    private fun openDetails(n: NotificationEntity) {
        val i = Intent(this, OrderDetailActivity::class.java).apply {
            putExtra("_title", n.title)
            putExtra("_body", n.body)
            n.orderId?.let { putExtra("orderId", it) }
            n.status?.let { putExtra("status", it) }
            n.customer?.let { putExtra("customer", it) }
            n.deliveryDateIso?.let { putExtra("deliveryDate", it) }
            n.type?.let { putExtra("type", it) }
        }
        startActivity(i)
    }

    private fun openDetailsFromExtras(ex: Bundle) {
        val i = Intent(this, OrderDetailActivity::class.java).apply {
            putExtras(ex)
        }
        startActivity(i)
    }

    private fun ensureNotifPermission() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
