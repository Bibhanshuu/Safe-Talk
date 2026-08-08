package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ChatViewModel
import com.example.ui.screens.ChatRoomScreen
import com.example.ui.screens.ConnectionScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PeerChatApp()
                }
            }
        }
    }
}

@Composable
fun PeerChatApp(viewModel: ChatViewModel = viewModel()) {
    val context = LocalContext.current
    val activeRoomId by viewModel.activeRoomId.observeAsState()
    val toastMessage by viewModel.toastMessage.observeAsState()

    // Show Toast messages on State Trigger
    LaunchedEffect(toastMessage) {
        toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    AnimatedContent(
        targetState = activeRoomId,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "ScreenTransition"
    ) { roomId ->
        if (roomId.isNull_or_empty()) {
            ConnectionScreen(viewModel = viewModel)
        } else {
            ChatRoomScreen(viewModel = viewModel)
        }
    }
}

private fun String?.isNull_or_empty(): Boolean {
    return this == null || this.isEmpty()
}
