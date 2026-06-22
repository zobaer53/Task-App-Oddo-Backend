package com.example.odootask.feature.task_create

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
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
 * Phase 17 Compose UI tests for the create task screen. Drives the stateless [CreateTaskContent]
 * with a hand-built [CreateTaskUiState] and a recording onEvent spy.
 */
class CreateTaskScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<CreateTaskUiEvent>()

    private fun setContent(state: CreateTaskUiState) {
        composeRule.setContent {
            AppTheme {
                CreateTaskContent(
                    state = state,
                    snackbarHostState = remember { SnackbarHostState() },
                    onEvent = { events.add(it) },
                )
            }
        }
    }

    @Test
    fun fields_areShown() {
        setContent(CreateTaskUiState())

        composeRule.onNodeWithText("Title").assertExists()
        composeRule.onNodeWithText("Description").assertExists()
        composeRule.onNodeWithText("Due Date").assertExists()
    }

    @Test
    fun typingTitle_firesNameChanged() {
        setContent(CreateTaskUiState())

        composeRule.onNodeWithText("Title").performTextInput("New task")

        assertThat(events).contains(CreateTaskUiEvent.NameChanged("New task"))
    }

    @Test
    fun typingDescription_firesDescriptionChanged() {
        setContent(CreateTaskUiState())

        composeRule.onNodeWithText("Description").performTextInput("Details")

        assertThat(events).contains(CreateTaskUiEvent.DescriptionChanged("Details"))
    }

    @Test
    fun dateField_opensDatePicker() {
        setContent(CreateTaskUiState())

        composeRule.onNodeWithText("Due Date").performClick()

        // The Material3 DatePickerDialog exposes OK / Cancel actions once shown.
        composeRule.onNodeWithText("OK").assertExists()
        composeRule.onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun createButton_disabledWhenNameBlank() {
        setContent(CreateTaskUiState(name = ""))

        composeRule.onNodeWithText("CREATE TASK").assertIsNotEnabled()
    }

    @Test
    fun createButton_enabledWhenNamePresent() {
        setContent(CreateTaskUiState(name = "New task"))

        composeRule.onNodeWithText("CREATE TASK").assertIsEnabled()
    }

    @Test
    fun createButtonClick_firesCreateClicked() {
        setContent(CreateTaskUiState(name = "New task"))

        composeRule.onNodeWithText("CREATE TASK").performClick()

        assertThat(events).contains(CreateTaskUiEvent.CreateClicked)
    }

    @Test
    fun backButton_firesBackClicked() {
        setContent(CreateTaskUiState())

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertThat(events).contains(CreateTaskUiEvent.BackClicked)
    }
}
