package com.example.odootask.feature.auth

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.odootask.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Phase 17 Compose UI tests for the login screen. Drives the stateless [LoginContent] with a
 * hand-built [LoginUiState] and a recording onEvent spy — no ViewModel or Hilt required.
 */
class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<LoginUiEvent>()

    private fun setContent(state: LoginUiState) {
        composeRule.setContent {
            AppTheme {
                LoginContent(state = state, onEvent = { events.add(it) })
            }
        }
    }

    @Test
    fun emailAndPasswordFields_areShown() {
        setContent(LoginUiState())

        composeRule.onNodeWithText("Email").assertExists()
        composeRule.onNodeWithText("Password").assertExists()
    }

    @Test
    fun typingEmail_firesUsernameChanged() {
        setContent(LoginUiState())

        composeRule.onNodeWithText("Email").performTextInput("admin")

        assertThat(events).contains(LoginUiEvent.UsernameChanged("admin"))
    }

    @Test
    fun typingPassword_firesPasswordChanged() {
        setContent(LoginUiState())

        composeRule.onNodeWithText("Password").performTextInput("secret")

        assertThat(events).contains(LoginUiEvent.PasswordChanged("secret"))
    }

    @Test
    fun maskedPassword_isNotReadable() {
        setContent(LoginUiState(password = "secret", isPasswordVisible = false))

        composeRule.onNodeWithText("secret").assertDoesNotExist()
    }

    @Test
    fun visiblePassword_isReadable() {
        setContent(LoginUiState(password = "secret", isPasswordVisible = true))

        composeRule.onNodeWithText("secret").assertExists()
    }

    @Test
    fun passwordToggle_firesVisibilityToggled() {
        setContent(LoginUiState(password = "secret", isPasswordVisible = false))

        composeRule.onNodeWithContentDescription("Show password").performClick()

        assertThat(events).contains(LoginUiEvent.PasswordVisibilityToggled)
    }

    @Test
    fun loginButton_firesLoginClicked() {
        setContent(LoginUiState())

        composeRule.onNodeWithText("LOGIN").performClick()

        assertThat(events).contains(LoginUiEvent.LoginClicked)
    }

    @Test
    fun loading_showsProgressAndHidesLabel() {
        setContent(LoginUiState(isLoading = true))

        composeRule.onNodeWithText("LOGIN").assertDoesNotExist()
    }

    @Test
    fun errorMessage_isRendered() {
        setContent(LoginUiState(errorMessage = "Invalid credentials"))

        composeRule.onNodeWithText("Invalid credentials").assertExists()
    }
}
