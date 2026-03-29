package com.expensemanager.ui.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.expensemanager.R
import com.expensemanager.ui.theme.DsRadius
import com.expensemanager.ui.theme.DsSpacing

@Composable
fun LockScreen(
    biometricEnabled: Boolean,
    onUnlocked: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as FragmentActivity
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var attemptedBiometric by remember { mutableStateOf(false) }

    val executor = remember { ContextCompat.getMainExecutor(context) }

    LaunchedEffect(biometricEnabled) {
        if (!biometricEnabled) {
            attemptedBiometric = false
            return@LaunchedEffect
        }
        if (attemptedBiometric) return@LaunchedEffect
        attemptedBiometric = true
        val mgr = BiometricManager.from(context)
        val canAuth = mgr.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK,
        )
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            return@LaunchedEffect
        }
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onUnlocked()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    error = context.getString(R.string.error_biometric)
                }

                override fun onAuthenticationFailed() {
                    error = context.getString(R.string.error_biometric)
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(context.getString(R.string.biometric_title))
            .setSubtitle(context.getString(R.string.biometric_subtitle))
            .setNegativeButtonText(context.getString(R.string.action_use_pin))
            .build()
        prompt.authenticate(info)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(DsSpacing.xl),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Card(
            shape = DsRadius.lg,
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            ),
        ) {
            Column(modifier = Modifier.padding(DsSpacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.padding(end = DsSpacing.xs))
                    Text(
                        text = stringResource(R.string.lock_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                }
                Text(
                    text = stringResource(R.string.lock_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = DsSpacing.xs, bottom = DsSpacing.lg),
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8 && it.all { d -> d.isDigit() }) pin = it },
                    label = { Text(stringResource(R.string.field_pin)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                )

                Spacer(Modifier.height(DsSpacing.md))

                Button(
                    onClick = {
                        if (onVerifyPin(pin)) {
                            onUnlocked()
                        } else {
                            error = context.getString(R.string.error_pin)
                            pin = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_unlock))
                }
            }
        }

        val canAuth = BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK,
        )
        if (biometricEnabled && canAuth == BiometricManager.BIOMETRIC_SUCCESS) {
            Spacer(Modifier.height(DsSpacing.sm))
            OutlinedButton(
                onClick = {
                    val prompt = BiometricPrompt(
                        activity,
                        executor,
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                onUnlocked()
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                error = context.getString(R.string.error_biometric)
                            }

                            override fun onAuthenticationFailed() {
                                error = context.getString(R.string.error_biometric)
                            }
                        },
                    )
                    val info = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(context.getString(R.string.biometric_title))
                        .setSubtitle(context.getString(R.string.biometric_subtitle))
                        .setNegativeButtonText(context.getString(android.R.string.cancel))
                        .build()
                    prompt.authenticate(info)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                    Text(
                        text = stringResource(R.string.action_biometric),
                        modifier = Modifier.padding(start = DsSpacing.xs),
                    )
                }
            }
        }
    }
}
