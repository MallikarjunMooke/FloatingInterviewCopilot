package com.example.floatinginterviewcopilot

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CopilotScreen(
                    onStart = {
                        val i = Intent(this, ListeningService::class.java).setAction(ListeningService.ACTION_START)
                        ContextCompat.startForegroundService(this, i)
                    },
                    onStop = {
                        startService(Intent(this, ListeningService::class.java).setAction(ListeningService.ACTION_STOP))
                    },
                    onOverlay = {
                        if (!Settings.canDrawOverlays(this)) {
                            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                        } else {
                            startService(Intent(this, ListeningService::class.java).setAction(ListeningService.ACTION_SHOW_BUBBLE))
                        }
                    },
                    onOpenChatGPT = { question -> openChatGPT(question) }
                )
            }
        }
    }

    private fun openChatGPT(question: String) {
        val prompt = """
            You are my concise interview copilot. Analyze the interviewer's question and return only:
            1. Question type
            2. What is being tested
            3. A four-bullet answer framework
            4. One likely follow-up question
            Keep the response under 80 words. For technical questions, consider approach, trade-offs, edge cases, and scalability. For behavioral questions, use STAR. Never invent my work experience. Use clear, natural language suitable for speaking in an interview.

            Interview question: $question
        """.trimIndent()

        // First try a standard Android share handoff. The user chooses ChatGPT if it accepts text.
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, prompt)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(Intent.createChooser(send, "Send interview question to an AI app"))
        } catch (_: Exception) {
            val launch = packageManager.getLaunchIntentForPackage("com.openai.chatgpt")
            if (launch != null) startActivity(launch)
        }
    }
}

@Composable
private fun CopilotScreen(
    onStart: () -> Unit,
    onStop: () -> Unit,
    onOverlay: () -> Unit,
    onOpenChatGPT: (String) -> Unit
) {
    var question by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("Not listening") }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        status = if (result[Manifest.permission.RECORD_AUDIO] == true) "Microphone permission granted" else "Microphone permission required"
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Floating Interview Copilot") }) }) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Prototype: speech → transcript → ChatGPT handoff", style = MaterialTheme.typography.titleMedium)
            Text("Status: $status")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    permissionLauncher.launch(
                        if (Build.VERSION.SDK_INT >= 33) arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS)
                        else arrayOf(Manifest.permission.RECORD_AUDIO)
                    )
                }) { Text("Grant permissions") }
                OutlinedButton(onClick = onOverlay) { Text("Enable floating bubble") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    onStart()
                    status = "Listening service requested"
                }) { Text("Start listening") }
                OutlinedButton(onClick = {
                    onStop()
                    status = "Stopped"
                }) { Text("Stop") }
            }
            OutlinedTextField(
                value = question,
                onValueChange = { question = it },
                label = { Text("Recognized question / edit before handoff") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp)
            )
            Button(
                onClick = { if (question.isNotBlank()) onOpenChatGPT(question.trim()) },
                enabled = question.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Transfer question to AI app") }
            Text(
                "Important: Android's share sheet may require you to select ChatGPT and tap Send. This prototype does not claim automatic submission. Speech recognition and overlay behavior need device testing.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
