package org.hyperskill.stopwatch

import android.app.AlertDialog
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.isVisible
import org.hyperskill.stopwatch.databinding.ActivityMainBinding
import java.lang.NumberFormatException

const val CHANNEL_ID = "org.hyperskill"
const val NOTIFICATION_ID = 393939

class MainActivity : AppCompatActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private lateinit var binding: ActivityMainBinding
    private var counting = false
    private var startTime = System.currentTimeMillis()
    private val colors = arrayOf(Color.RED, Color.MAGENTA, Color.BLUE, Color.DKGRAY)
    private var index = 0
    private var limit = 0
    private lateinit var channel: NotificationChannel
    private var notificationSentFlag = false

    private val updateCount: Runnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
        override fun run() {
            val nowTime = System.currentTimeMillis()
            val diff = (nowTime - startTime)/1000
            val seconds = diff % 60
            val minutes = diff / 60
            handler.post {
                binding.textView.text = String.format("%02d:%02d", minutes, seconds)
                if (limit > 0 && diff > limit && !notificationSentFlag){
                    binding.textView.setTextColor(Color.RED)
                    createNotification(binding.root.context)
                    notificationSentFlag = true
                }
                binding.progressBar.indeterminateTintList = ColorStateList.valueOf(colors[index])
            }
            index = (index + 1) % colors.size
            handler.postDelayed(this, 1000)
        }
    }

    private fun createNotification(context: Context) {
        val newIntent = Intent(context, MainActivity::class.java)
        val pIntent = PendingIntent.getActivity(context, 0, newIntent, PendingIntent.FLAG_IMMUTABLE)
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.tomato)
            .setContentTitle("Pomodoro")
            //.setContentText(Book.phrases.random())
            .setContentText("It's time to take a break!")
            .setStyle(NotificationCompat.BigTextStyle())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pIntent)
        val mNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = notificationBuilder.build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT or Notification.FLAG_ONLY_ALERT_ONCE
        mNotificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Stopwatch"
            val descriptionText = "------alarm------"
            val importance = NotificationManager.IMPORTANCE_HIGH
            channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startButton.setOnClickListener {
            if (counting) return@setOnClickListener
            counting = true
            binding.progressBar.isVisible = true
            binding.settingsButton.isEnabled = false
            startTime = System.currentTimeMillis()
            handler.post(updateCount)
        }

        binding.resetButton.setOnClickListener {
            counting = false
            binding.progressBar.isVisible = false
            binding.settingsButton.isEnabled = true
            startTime = System.currentTimeMillis()
            handler.removeCallbacks(updateCount)
            handler.post {
                binding.textView.text = "00:00"
                binding.textView.setTextColor(Color.DKGRAY)
            }
            notificationSentFlag = false
        }

        binding.settingsButton.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            val inputEditTextField = EditText(this)
            inputEditTextField.id = R.id.upperLimitEditText
            builder.setView(inputEditTextField)
                .setTitle("Set upper limit in seconds")
                .setPositiveButton("Ok") { dialog, id ->
                    val string = inputEditTextField.text.toString()
                    try {
                        limit = string.toInt()
                    } catch (ex: NumberFormatException) {
                        limit = 0
                    }
                }
                .setNegativeButton("Cancel") { dialog, id ->
                    dialog.cancel()
                }
            val alertDialog = builder.create()
            alertDialog?.show()
        }

        createNotificationChannel()

    }
}