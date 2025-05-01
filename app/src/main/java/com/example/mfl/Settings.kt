package com.example.mfl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.BorderStroke


class Settings : Fragment() {
    private lateinit var auth: FirebaseAuth
    private lateinit var syncHelper: FirebaseSyncHelper
    private var isAdmin by mutableStateOf(false)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        auth = Firebase.auth
        syncHelper = FirebaseSyncHelper(requireContext())
        checkUserRole()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme(
                    colorScheme = lightColorScheme().copy(
                        primary = Color.Black,
                        onPrimary = Color.White,
                        primaryContainer = Color.Black,
                        onPrimaryContainer = Color.White,
                        inversePrimary = Color.White,
                        secondary = Color.Black,
                        onSecondary = Color.White,
                        secondaryContainer = Color(0xFFF5F5F5),
                        onSecondaryContainer = Color.Black,
                        tertiary = Color.Black,
                        onTertiary = Color.White,
                        background = Color.White,
                        onBackground = Color.Black,
                        surface = Color.White,
                        onSurface = Color.Black,
                        surfaceVariant = Color(0xFFF5F5F5),
                        onSurfaceVariant = Color(0xFF444444),
                        inverseSurface = Color.Black,
                        inverseOnSurface = Color.White,
                        error = Color(0xFFB00020),
                        onError = Color.White,
                        outline = Color(0xFFE0E0E0)
                    ),
                    typography = Typography()
                ) {
                    SettingsScreen(
                        onLogout = ::logout,
                        isAdmin = isAdmin,
                        onSendFeedback = { message ->
                            syncHelper.sendFeedbackToFirebase(auth.currentUser?.email ?: "anonymous", message)
                        }
                    )
                }
            }
        }
    }

    private fun checkUserRole() {
        val currentEmail = auth.currentUser?.email
        isAdmin = currentEmail == "vitopuk87@gmail.com"
    }

    private fun logout() {
        auth.signOut()
        startActivity(Intent(requireActivity(), AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        requireActivity().finish()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    isAdmin: Boolean,
    onSendFeedback: (String) -> Unit
) {
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Кнопка выхода
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text("Выйти из аккаунта")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Кнопка обратной связи
            OutlinedButton(
                onClick = { showFeedbackDialog = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(2.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    width = 1.dp
                )
            ) {
                Text("Оставить отзыв")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Раздел администратора
            if (isAdmin) {
                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                FeedbackAdminSection()
            }
        }
    }

    // Диалог обратной связи
    if (showFeedbackDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = {
                Text(
                    "Обратная связь",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = feedbackMessage,
                        onValueChange = { feedbackMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ваш отзыв") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        shape = RoundedCornerShape(2.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSendFeedback(feedbackMessage)
                        feedbackMessage = ""
                        showFeedbackDialog = false
                    },
                    enabled = feedbackMessage.isNotBlank()
                ) {
                    Text("ОТПРАВИТЬ")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("ОТМЕНА")
                }
            },
            shape = RoundedCornerShape(2.dp),
            containerColor = MaterialTheme.colorScheme.background
        )
    }
}

@Composable
fun FeedbackAdminSection() {
    val context = LocalContext.current
    val syncHelper = remember { FirebaseSyncHelper(context) }
    var feedbackList by remember { mutableStateOf<List<Feedback>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Функция для обновления статуса отзыва
    fun updateFeedbackStatus(feedbackId: String) {
        feedbackList = feedbackList.map { feedback ->
            if (feedback.id == feedbackId) {
                feedback.copy(status = "resolved") // Предполагая, что Feedback - data class
            } else {
                feedback
            }
        }
    }

    LaunchedEffect(Unit) {
        syncHelper.getFeedbackFromFirebase(
            onSuccess = { feedbacks ->
                feedbackList = feedbacks.sortedByDescending { it.timestamp }
                isLoading = false
            },
            onError = { message ->
                error = message
                isLoading = false
                Log.e("Feedback", message)
            }
        )
    }

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "ОТЗЫВЫ ПОЛЬЗОВАТЕЛЕЙ",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        when {
            isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurface
            )
            error != null -> Text(
                "Ошибка: $error",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
            feedbackList.isEmpty() -> Text(
                "Нет отзывов",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(feedbackList) { feedback ->
                        FeedbackItem(feedback) {
                            syncHelper.markFeedbackAsResolved(feedback.id) {
                                // Обновляем локальное состояние после успешного обновления в Firebase
                                feedbackList = feedbackList.map { f ->
                                    if (f.id == feedback.id) f.copy(status = "resolved") else f
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackItem(
    feedback: Feedback,
    onResolve: () -> Unit
) {
    val date = remember {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            .format(Date(feedback.timestamp))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(2.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = feedback.userEmail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = feedback.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (feedback.status != "resolved") {
                OutlinedButton(
                    onClick = {
                        onResolve() // Вызываем переданный callback
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(2.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("РЕШЕНО", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                Text(
                    text = "РЕШЕНО",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}