package com.example.mfl

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

class AuthActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var oneTapClient: SignInClient
    private lateinit var syncHelper: FirebaseSyncHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Инициализация Firebase
        auth = FirebaseAuth.getInstance()
        oneTapClient = Identity.getSignInClient(this)
        syncHelper = FirebaseSyncHelper(this)

        setContent {
            AuthScreen { email ->
                // Синхронизируем данные после успешной авторизации
                if (isOnline()) {
                    syncHelper.syncAllData()
                }
                val intent = Intent(this, Panel::class.java).apply {
                    putExtra("userEmail", email)
                }
                startActivity(intent)
                finish()
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun isOnline(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onResume() {
        super.onResume()
        // Синхронизация при возвращении в приложение
        if (isOnline()) {
            syncHelper.syncAllData()
        }
    }

    override fun onStart() {
        super.onStart()
        auth.currentUser?.let { user ->
            // Синхронизируем данные перед переходом в Panel
            if (isOnline()) {
                syncHelper.syncAllData()
            }

            val intent = Intent(this, Panel::class.java).apply {
                putExtra("userEmail", user.email ?: "email_not_available")
            }
            startActivity(intent)
            finish()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(onAuthSuccess: (String) -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val oneTapClient = remember { Identity.getSignInClient(context) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
        onResult = { result ->
            try {
                val credential = oneTapClient.getSignInCredentialFromIntent(result.data)
                val idToken = credential.googleIdToken
                if (idToken != null) {
                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    auth.signInWithCredential(firebaseCredential).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val userEmail = auth.currentUser?.email ?: "no_email"
                            onAuthSuccess(userEmail)
                        } else {
                            errorMessage = "Ошибка входа через Google"
                        }
                    }
                } else {
                    errorMessage = "Google ID Token is null"
                }
            } catch (e: Exception) {
                Log.e("AuthScreen", "Google Sign-In failed", e)
                errorMessage = "Ошибка Google авторизации"
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Авторизация",
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = Color.Black) },
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                containerColor = Color.White,
                cursorColor = Color.Black,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Gray
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль", color = Color.Black) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                containerColor = Color.White,
                cursorColor = Color.Black,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Gray
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = errorMessage,
                color = Color.DarkGray,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    errorMessage = "Пожалуйста, заполните все поля"
                } else {
                    coroutineScope.launch {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    onAuthSuccess(email)
                                } else {
                                    errorMessage = "Ошибка авторизации"
                                }
                            }
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти")
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = {
                val signInRequest = BeginSignInRequest.Builder()
                    .setGoogleIdTokenRequestOptions(
                        BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                            .setSupported(true)
                            .setServerClientId(context.getString(R.string.default_web_client_id))
                            .setFilterByAuthorizedAccounts(false)
                            .build()
                    )
                    .setAutoSelectEnabled(true)
                    .build()

                oneTapClient.beginSignIn(signInRequest)
                    .addOnSuccessListener { result ->
                        val intentSenderRequest = IntentSenderRequest.Builder(result.pendingIntent).build()
                        googleSignInLauncher.launch(intentSenderRequest)
                    }
                    .addOnFailureListener {
                        errorMessage = "Ошибка начала входа через Google"
                    }
            },
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.Black
            ),
            border = BorderStroke(1.dp, Color.Black),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Войти через Google")
        }

        Spacer(modifier = Modifier.height(24.dp))

        TextButton(
            onClick = {
                context.startActivity(Intent(context, MainActivity::class.java))
            }
        ) {
            Text("Нет аккаунта? Зарегистрироваться", color = Color.Black)
        }
    }
}