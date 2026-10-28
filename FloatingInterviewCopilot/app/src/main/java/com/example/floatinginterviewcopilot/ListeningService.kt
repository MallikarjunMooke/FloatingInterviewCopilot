package com.example.floatinginterviewcopilot

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import java.util.Locale

class ListeningService : Service() {
    private var recognizer: SpeechRecognizer? = null
    private var windowManager: WindowManager? = null
    private var bubble: android.view.View? = null
    private var lastText = ""
    private var lastSubmittedAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopListeningAndService()
            ACTION_SHOW_BUBBLE -> showBubble()
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                startListening()
                showBubble()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Interview Copilot", NotificationManager.IMPORTANCE_LOW))
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Interview Copilot is active")
            .setContentText("Microphone listening is active. Tap Stop in the app to end.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()
    }

    private fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            updateBubble("Speech recognition unavailable")
            return
        }
        try {
            recognizer?.destroy()
            recognizer = SpeechRecognizer.createSpeechRecognizer(this).also { sr ->
                sr.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: android.os.Bundle?) { updateBubble("Listening…") }
                    override fun onBeginningOfSpeech() { updateBubble("Hearing speech…") }
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { updateBubble("Processing speech…") }
                    override fun onError(error: Int) {
                        updateBubble("Recognition paused (error $error). Tap Start again.")
                    }
                    override fun onResults(results: android.os.Bundle?) {
                        val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty().trim()
                        if (text.isNotBlank() && text != lastText) {
                            lastText = text
                            getSharedPreferences("copilot", MODE_PRIVATE).edit().putString("last_question", text).apply()
                            updateBubble(text)
                            // SpeechRecognizer can stop after one utterance; do not auto-submit without user confirmation.
                        } else updateBubble("No new question detected")
                    }
                    override fun onPartialResults(partialResults: android.os.Bundle?) {
                        val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                        if (!text.isNullOrBlank()) updateBubble(text)
                    }
                    override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                sr.startListening(intent)
            }
        } catch (e: Exception) {
            updateBubble("Could not start recognition: ${e.message ?: "unknown error"}")
        }
    }

    private fun showBubble() {
        if (!Settings.canDrawOverlays(this) || bubble != null) return
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            setBackgroundColor(0xEE202124.toInt())
            addView(TextView(this@ListeningService).apply {
                text = "Interview Copilot"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
            })
            addView(TextView(this@ListeningService).apply {
                tag = "status"
                text = "Ready"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
            })
            addView(Button(this@ListeningService).apply {
                text = "Stop"
                setOnClickListener { stopListeningAndService() }
            })
        }
        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 20; y = 180 }

        layout.setOnTouchListener(object : android.view.View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onTouch(v: android.view.View, event: android.view.MotionEvent): Boolean {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        initialX = params.x; initialY = params.y
                        initialTouchX = event.rawX; initialTouchY = event.rawY
                        return true
                    }
                    android.view.MotionEvent.ACTION_MOVE -> {
                        params.x = initialX - (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager?.updateViewLayout(layout, params)
                        return true
                    }
                }
                return false
            }
        })
        bubble = layout
        try { windowManager?.addView(layout, params) } catch (_: Exception) { bubble = null }
    }

    private fun updateBubble(text: String) {
        val view = bubble ?: return
        val status = view.findViewWithTag<TextView>("status")
        status?.post { status.text = text.take(140) }
    }

    private fun stopListeningAndService() {
        try { recognizer?.stopListening(); recognizer?.destroy() } catch (_: Exception) {}
        recognizer = null
        bubble?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        bubble = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        try { recognizer?.destroy() } catch (_: Exception) {}
        bubble?.let { try { windowManager?.removeView(it) } catch (_: Exception) {} }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.floatinginterviewcopilot.START"
        const val ACTION_STOP = "com.example.floatinginterviewcopilot.STOP"
        const val ACTION_SHOW_BUBBLE = "com.example.floatinginterviewcopilot.SHOW_BUBBLE"
        private const val CHANNEL_ID = "copilot_listener"
        private const val NOTIFICATION_ID = 721
    }
}
