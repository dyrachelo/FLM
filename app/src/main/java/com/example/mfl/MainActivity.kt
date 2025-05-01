package com.example.mfl

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.util.Log
import androidx.compose.foundation.shape.RoundedCornerShape

class MainActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth

        enableEdgeToEdge()

        setContent {
            Surface(
                color = Color.White,
                modifier = Modifier.fillMaxSize()
            ) {
                RegistrationScreen(auth)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationScreen(auth: FirebaseAuth) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Регистрация",
            fontSize = 24.sp,
            color = Color.Black,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Логин", color = Color.Black) },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                containerColor = Color.White,
                cursorColor = Color.Black,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Gray,
                disabledTextColor = Color.Gray,
                disabledBorderColor = Color.Gray,
                disabledLabelColor = Color.Gray,
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red
            ),
            shape = RoundedCornerShape(4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = Color.Black) },
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Black) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                containerColor = Color.White,
                cursorColor = Color.Black,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Gray,
                disabledTextColor = Color.Gray,
                disabledBorderColor = Color.Gray,
                disabledLabelColor = Color.Gray,
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red
            ),
            shape = RoundedCornerShape(4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        PasswordFieldWithValidation(
            password = password,
            onPasswordChange = { password = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (username.isBlank() || email.isBlank() || password.isBlank()) {
                    errorMessage = "Не все поля заполнены"
                } else if (!isPasswordValid(password)) {
                    errorMessage = "Пароль не соответствует требованиям"
                } else {
                    isLoading = true
                    errorMessage = null
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            isLoading = false
                            if (task.isSuccessful) {
                                Log.d("MyLog", "Регистрация успешна")
                                signIn(auth, email, password, context)
                            } else {
                                errorMessage = task.exception?.message ?: "Ошибка регистрации"
                                Log.d("MyLog", "Ошибка регистрации: ${task.exception?.message}")
                            }
                        }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(4.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Black,
                contentColor = Color.White,
                disabledContainerColor = Color.DarkGray,
                disabledContentColor = Color.Gray
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text("ЗАРЕГИСТРИРОВАТЬСЯ", color = Color.White)
            }
        }

        AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            errorMessage?.let { message ->
                Text(
                    text = message,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            context.startActivity(Intent(context, AuthActivity::class.java))
        }) {
            Text("Уже есть аккаунт? Войти", color = Color.Black)
        }
    }
}

private fun signIn(auth: FirebaseAuth, email: String, password: String, context: android.content.Context) {
    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("MyLog", "Авторизация успешна")
                context.startActivity(Intent(context, Panel::class.java).apply {
                    putExtra("userEmail", email)
                })
                if (context is android.app.Activity) {
                    context.finish()
                }
            } else {
                Log.d("MyLog", "Ошибка авторизации: ${task.exception?.message}")
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordFieldWithValidation(
    password: String,
    onPasswordChange: (String) -> Unit
) {
    val requirements = listOf(
        Requirement("8+ символов", password.length >= 8),
        Requirement("1+ цифра", password.any { it.isDigit() }),
        Requirement("1+ заглавная", password.any { it.isUpperCase() }),
        Requirement("1+ строчная", password.any { it.isLowerCase() }),
    )

    Column {
        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Пароль", color = Color.Black) },
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black) },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedTextColor = Color.Black,
                unfocusedTextColor = Color.Black,
                containerColor = Color.White,
                cursorColor = Color.Black,
                focusedBorderColor = Color.Black,
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color.Black,
                unfocusedLabelColor = Color.Gray,
                disabledTextColor = Color.Gray,
                disabledBorderColor = Color.Gray,
                disabledLabelColor = Color.Gray,
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red
            ),
            shape = RoundedCornerShape(4.dp)
        )

        AnimatedVisibility(
            visible = password.isNotEmpty(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                requirements.forEach { req ->
                    PasswordRequirementItem(
                        text = req.text,
                        fulfilled = req.fulfilled
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordRequirementItem(text: String, fulfilled: Boolean) {
    val color by animateColorAsState(
        targetValue = if (fulfilled) Color.Black else Color.Gray,
        animationSpec = tween(durationMillis = 300)
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (fulfilled) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = color,
            fontSize = 12.sp
        )
    }
}

data class Requirement(val text: String, val fulfilled: Boolean)

private fun isPasswordValid(password: String): Boolean {
    return password.length >= 8 &&
            password.any { it.isDigit() } &&
            password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() } &&
            password.any { !it.isLetterOrDigit() }
}
