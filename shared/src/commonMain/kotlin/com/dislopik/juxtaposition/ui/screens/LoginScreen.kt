package com.dislopik.juxtaposition.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dislopik.juxtaposition.data.Juxt
import com.dislopik.juxtaposition.ui.JuxtColors
import com.dislopik.juxtaposition.ui.components.juxtTextFieldColors
import com.dislopik.juxtaposition.ui.describeError
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(onSignedIn: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun submit() {
        if (loading || username.isBlank() || password.isBlank()) return
        loading = true
        error = null
        scope.launch {
            try {
                Juxt.api.login(username.trim(), password)
                onSignedIn()
            } catch (e: Throwable) {
                error = describeError(e)
            } finally {
                loading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(JuxtColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(40.dp))

            Text(
                text = "Juxtaposition",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Pretendo Network",
                style = MaterialTheme.typography.labelLarge,
                color = JuxtColors.PurpleLight,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Mirrors the site's `form.account` card.
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JuxtColors.LoginCard)
                    .padding(horizontal = 24.dp, vertical = 28.dp)
            ) {
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Enter your account details below",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JuxtColors.TextSecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )

                Spacer(Modifier.height(20.dp))

                FieldLabel("Username")
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    singleLine = true,
                    enabled = !loading,
                    shape = RoundedCornerShape(6.dp),
                    colors = juxtTextFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                FieldLabel("Password")
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    singleLine = true,
                    enabled = !loading,
                    shape = RoundedCornerShape(6.dp),
                    colors = juxtTextFieldColors(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 18.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(JuxtColors.ErrorToast)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = { submit() },
                    enabled = !loading && username.isNotBlank() && password.isNotBlank(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = JuxtColors.Purple,
                        contentColor = Color.White,
                        disabledContainerColor = JuxtColors.PurpleMuted,
                        disabledContentColor = JuxtColors.TextMuted
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Login")
                    }
                }
            }

            Text(
                text = "Your credentials go straight to juxt.pretendo.network and are never stored.",
                style = MaterialTheme.typography.labelSmall,
                color = JuxtColors.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(top = 20.dp, bottom = 40.dp)
            )
        }
    }
}

/** The site labels its login fields in small uppercase text. */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = JuxtColors.TextSecondary,
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}
