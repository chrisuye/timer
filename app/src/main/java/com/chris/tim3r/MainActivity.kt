package com.chris.tim3r

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.*
import android.util.Log.d
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.shitij.goyal.slidebutton.SwipeButton
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bday.view.*
import kotlinx.android.synthetic.main.count_down.*
import kotlinx.android.synthetic.main.count_down.view.*
import kotlinx.android.synthetic.main.texts.view.*
import kotlinx.android.synthetic.main.time_picker.view.*
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates


class MainActivity : AppCompatActivity() {
    private val quote: ListOfQuotesInterface = ListOfQuotes()
    private var total by Delegates.notNull<Long>()
    private var progr: Double = 0.0
    private var isPaused = false
    private var isCanceled = false
    private var timeLeft by Delegates.notNull<Long>()
    private var interval: Long = 250
    private lateinit var handler: Handler

    lateinit var notificationChannel: NotificationChannel
    lateinit var notificationManager: NotificationManager
    lateinit var builder: Notification.Builder
    private val channelId = "com.chris.tim3r"
    private val description = "Time's up!"

    private var timeTaken: Long = 1000

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPref = getSharedPreferences("Settings", MODE_MULTI_PROCESS)
        val checkStart = sharedPref.getBoolean("start", false)

        if (checkStart) {
            greet.visibility = View.GONE
        }
        greet.swipe_btn.setOnClickListener{
            greet.visibility = View.GONE

            with (sharedPref.edit()) {
                putBoolean("start", true)
                commit()
            }

        }

        setTime()
        handler = Handler()

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        textAnim.quote.text = quote.getQuote().quote
        textAnim.author.text = quote.getQuote().author

        AsyncTask.execute {
            while (true){
                Thread.sleep(5000)
                runOnUiThread {
                    textAnim.viewFlipper.startFlipping()
                    val quotes = quote.getQuote()
                    textAnim.quote.text = quotes.quote
                    textAnim.author.text = quotes.author
                    textAnim.viewFlipper.stopFlipping()
                }
            }

        }

        hour.up_btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    stopRepeatingTaskHourUp()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_DOWN -> {
                    startRepeatingTaskHourUp()
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        hour.down_btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    stopRepeatingTaskHourDown()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_DOWN -> {
                    startRepeatingTaskHourDown()
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        min.up_btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    stopRepeatingTaskMinUp()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_DOWN -> {
                    startRepeatingTaskMinUp()
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        min.down_btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    stopRepeatingTaskMinDown()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_DOWN -> {
                    startRepeatingTaskMinDown()
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        second.up_btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    stopRepeatingTaskSecUp()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_DOWN -> {
                    startRepeatingTaskSecUp()
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        second.down_btn.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_UP -> {
                    stopRepeatingTaskSecDown()
                    return@setOnTouchListener true
                }
                MotionEvent.ACTION_DOWN -> {
                    startRepeatingTaskSecDown()
                    return@setOnTouchListener true
                }
                else -> return@setOnTouchListener false
            }
        }

        start_btn.setOnClickListener {
            pause_btn.visibility = View.VISIBLE
            cancel_btn.visibility = View.VISIBLE
            progr = 0.0
            updateProgressBar()
            val hr = hour.inText.text.toString().toLong() * 3600 * 1000
            val mins = min.inText.text.toString().toLong() * 60 * 1000
            val sec = second.inText.text.toString().toLong() * 1000
            total = hr + mins + sec
            hour.visibility = View.GONE
            min.visibility = View.GONE
            second.visibility = View.GONE
            timeMain.visibility = View.VISIBLE
            it.visibility = View.INVISIBLE

            isCanceled = false
            isPaused = false
            timeTaken = total
            count(total)
        }

        pause_btn.setOnClickListener {
            isPaused = !isPaused
            if (!isPaused) {
                count(timeLeft)
                pause_btn.setImageResource(R.drawable.ic_baseline_pause_24)
            } else {
                pause_btn.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            }
        }

        cancel_btn.setOnClickListener {
            isCanceled = true
            reset()
            hour.visibility = View.VISIBLE
            min.visibility = View.VISIBLE
            second.visibility = View.VISIBLE
            timeMain.visibility = View.GONE
            start_btn.visibility = View.VISIBLE
        }

    }

    private fun count(end: Long) {
        object : CountDownTimer(end, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                progr = timeTaken.toDouble()/total.toDouble() * 100
                updateProgressBar()

                if (isCanceled || isPaused) {
                    cancel()
                    timeLeft = millisUntilFinished
                } else {
                    timeTaken -= 1000
                    if (TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 60 < 10) {
                        timeMain.hourView.text = "0${TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 60}"
                    } else {
                        timeMain.hourView.text =
                            "${TimeUnit.MILLISECONDS.toHours(millisUntilFinished) % 60}"
                    }
                    if (TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60 < 10) {
                        timeMain.minView.text =  "0${TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60}"
                    } else {
                        timeMain.minView.text =
                            "${TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60}"
                    }
                    if (TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60 < 10) {
                        timeMain.secView.text = "0${TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60}"
                    } else {
                        timeMain.secView.text =
                            "${TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60}"
                    }
                }
            }

            override fun onFinish() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    notificationChannel =
                        NotificationChannel(
                            channelId,
                            description,
                            NotificationManager.IMPORTANCE_HIGH
                        )
                    notificationChannel.enableLights(true)
                    notificationChannel.lightColor = Color.GREEN
                    notificationChannel.enableVibration(true)
                    notificationManager.createNotificationChannel(notificationChannel)

                    builder = Notification.Builder(this@MainActivity, channelId)
                        .setContentTitle("Time is up!")
                        .setContentText(quote.getQuote().quote)
                        .setSmallIcon(R.drawable.ic_baseline_timer_24)
                        .setAutoCancel(true)

                }else{
                    builder = Notification.Builder(this@MainActivity)
                        .setContentTitle("Time is up!")
                        .setContentText(quote.getQuote().quote)
                        .setAutoCancel(true)
                        .setSmallIcon(R.drawable.ic_baseline_timer_24)

                }

                notificationManager.notify(12345, builder.build())
                reset()
                hour.visibility = View.VISIBLE
                min.visibility = View.VISIBLE
                second.visibility = View.VISIBLE
                timeMain.visibility = View.GONE
                start_btn.visibility = View.VISIBLE
                timeTaken = 0
            }
        }.start()
    }

    private fun reset() {
        pause_btn.setImageResource(R.drawable.ic_baseline_pause_24)
        pause_btn.visibility = View.GONE
        cancel_btn.visibility = View.GONE
        timeTaken = 0
    }

    private fun setTime() {
        pause_btn.setImageResource(R.drawable.ic_baseline_pause_24)
        pause_btn.visibility = View.GONE
        cancel_btn.visibility = View.GONE
        hour.upText.text = ""
        min.upText.text = ""
        second.upText.text = ""

        hour.inText.setText(R.string._00)
        min.inText.setText(R.string._00)
        second.inText.setText(R.string._00)

        hour.downText.setText(R.string._01)
        min.downText.setText(R.string._01)
        second.downText.setText(R.string._01)

        hour.textName.setText(R.string.hours)
        min.textName.setText(R.string.min)
        second.textName.setText(R.string.sec)
    }

    private fun updateProgressBar() {
        progressBar.progress = progr.toInt()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        reset()
    }

    private var hourUpStatusChecker = object : Runnable {
        override fun run() {
            try {
                runOnUiThread {
                    if (hour.inText.text.toString().toInt() < 23) {
                        hour.upText.text = hour.inText.text.toString()
                        hour.inText.text = (hour.inText.text.toString().toInt() + 1).toString()
                        if (hour.inText.text.toString().toInt() == 23) {
                            hour.downText.text = ""
                        } else {
                            hour.downText.text = (hour.inText.text.toString().toInt() + 1).toString()
                        }
                    }
                }
            } finally {
                handler.postDelayed(this, interval)
            }
        }
    }

    private fun startRepeatingTaskHourUp() {
        hourUpStatusChecker.run()
    }

    private fun stopRepeatingTaskHourUp() {
        handler.removeCallbacks(hourUpStatusChecker)
    }

    private var hourDownStatusChecker = object : Runnable {
        override fun run() {
            try {
                runOnUiThread {
                    if (hour.inText.text.toString().toInt() > 0) {
                        hour.downText.text = hour.inText.text.toString()
                        hour.inText.text = (hour.inText.text.toString().toInt() - 1).toString()
                        if (hour.inText.text.toString().toInt() == 0) {
                            hour.upText.text = ""
                        } else {
                            hour.upText.text = (hour.inText.text.toString().toInt() - 1).toString()
                        }
                    }
                }
            } finally {
                handler.postDelayed(this, interval)
            }
        }
    }

    private fun startRepeatingTaskHourDown() {
        hourDownStatusChecker.run()
    }

    private fun stopRepeatingTaskHourDown() {
        handler.removeCallbacks(hourDownStatusChecker)
    }

    private var minUpStatusChecker = object : Runnable {
        override fun run() {
            try {
                runOnUiThread {
                    if (min.inText.text.toString().toInt() < 59) {
                        min.upText.text = min.inText.text.toString()
                        min.inText.text = (min.inText.text.toString().toInt() + 1).toString()
                        if (min.inText.text.toString().toInt() == 59) {
                            min.downText.text = ""
                        } else {
                            min.downText.text = (min.inText.text.toString().toInt() + 1).toString()
                        }
                    }
                }
            } finally {
                handler.postDelayed(this, interval)
            }
        }
    }

    private fun startRepeatingTaskMinUp() {
        minUpStatusChecker.run()
    }

    private fun stopRepeatingTaskMinUp() {
        handler.removeCallbacks(minUpStatusChecker)
    }

    private var minDownStatusChecker = object : Runnable {
        override fun run() {
            try {
                runOnUiThread {
                    if (min.inText.text.toString().toInt() > 0) {
                        min.downText.text = min.inText.text.toString()
                        min.inText.text = (min.inText.text.toString().toInt() - 1).toString()
                        if (min.inText.text.toString().toInt() == 0) {
                            min.upText.text = ""
                        } else {
                            min.upText.text = (min.inText.text.toString().toInt() - 1).toString()
                        }
                    }
                }
            } finally {
                handler.postDelayed(this, interval)
            }
        }
    }

    private fun startRepeatingTaskMinDown() {
        minDownStatusChecker.run()
    }

    private fun stopRepeatingTaskMinDown() {
        handler.removeCallbacks(minDownStatusChecker)
    }

    private var secUpStatusChecker = object : Runnable {
        override fun run() {
            try {
                runOnUiThread {
                    if (second.inText.text.toString().toInt() < 59) {
                        second.upText.text = second.inText.text.toString()
                        second.inText.text = (second.inText.text.toString().toInt() + 1).toString()
                        if (second.inText.text.toString().toInt() == 59) {
                            second.downText.text = ""
                        } else {
                            second.downText.text = (second.inText.text.toString().toInt() + 1).toString()
                        }
                    }
                }
            } finally {
                handler.postDelayed(this, interval)
            }
        }
    }

    private fun startRepeatingTaskSecUp() {
        secUpStatusChecker.run()
    }

    private fun stopRepeatingTaskSecUp() {
        handler.removeCallbacks(secUpStatusChecker)
    }

    private var secDownStatusChecker = object : Runnable {
        override fun run() {
            try {
                runOnUiThread {
                    if (second.inText.text.toString().toInt() > 0) {
                        second.downText.text = second.inText.text.toString()
                        second.inText.text = (second.inText.text.toString().toInt() - 1).toString()
                        if (second.inText.text.toString().toInt() == 0) {
                            second.upText.text = ""
                        } else {
                            second.upText.text = (second.inText.text.toString().toInt() - 1).toString()
                        }
                    }
                }
            } finally {
                handler.postDelayed(this, interval)
            }
        }
    }

    private fun startRepeatingTaskSecDown() {
        secDownStatusChecker.run()
    }

    private fun stopRepeatingTaskSecDown() {
        handler.removeCallbacks(secDownStatusChecker)
    }
}
