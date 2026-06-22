package com.example.odootask.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.odootask.core.mvi.CollectEffect

@Composable
fun LoginScreen(
    onNavigateToTasks: () -> Unit,
    viewModel: LoginViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            LoginUiEffect.NavigateToTasks -> onNavigateToTasks()
        }
    }

    LoginContent(
        state = state,
        onEvent = viewModel::onEvent,
    )
}

@Composable
internal fun LoginContent(
    state: LoginUiState,
    onEvent: (LoginUiEvent) -> Unit,
) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            ) {
                BrandHeader()

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = { onEvent(LoginUiEvent.UsernameChanged(it)) },
                        label = { Text("Email") },
                        singleLine = true,
                        enabled = !state.isLoading,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    OutlinedTextField(
                        value = state.password,
                        onValueChange = { onEvent(LoginUiEvent.PasswordChanged(it)) },
                        label = { Text("Password") },
                        singleLine = true,
                        enabled = !state.isLoading,
                        visualTransformation = if (state.isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = { onEvent(LoginUiEvent.PasswordVisibilityToggled) },
                            ) {
                                Icon(
                                    imageVector = if (state.isPasswordVisible) {
                                        Icons.Filled.VisibilityOff
                                    } else {
                                        Icons.Filled.Visibility
                                    },
                                    contentDescription = if (state.isPasswordVisible) {
                                        "Hide password"
                                    } else {
                                        "Show password"
                                    },
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (state.errorMessage != null) {
                        Text(
                            text = state.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    androidx.compose.material3.Button(
                        onClick = { onEvent(LoginUiEvent.LoginClicked) },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text(
                                text = "LOGIN",
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .padding(vertical = 36.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "odoo",
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Mobile",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}
