package com.example.odootask.feature.tasks

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.odootask.ui.theme.AppTheme
import com.example.odootask.domain.model.Task
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Phase 17 Compose UI tests for the task list. Drives the stateless [TasksContent] with a
 * hand-built [TasksUiState] and a recording onEvent spy.
 */
class TasksScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<TasksUiEvent>()

    private fun setContent(state: TasksUiState) {
        composeRule.setContent {
            AppTheme {
                TasksContent(
                    state = state,
                    snackbarHostState = remember { SnackbarHostState() },
                    onEvent = { events.add(it) },
                )
            }
        }
    }

    private val sampleTasks = listOf(
        Task(id = 1, name = "Write report", stageId = 2, stageName = "In Progress", dateDeadline = "2026-07-01"),
        Task(id = 2, name = "Ship release", stageId = 3, stageName = "Done", dateDeadline = null),
    )

    @Test
    fun tasks_renderNamesStagesAndDeadline() {
        setContent(TasksUiState(tasks = sampleTasks))

        composeRule.onNodeWithText("Write report").assertExists()
        composeRule.onNodeWithText("Ship release").assertExists()
        composeRule.onNodeWithText("In Progress").assertExists()
        composeRule.onNodeWithText("Done").assertExists()
        composeRule.onNodeWithText("Jul 1, 2026").assertExists()
        composeRule.onNodeWithText("No deadline").assertExists()
    }

    @Test
    fun emptyList_showsEmptyState() {
        setContent(TasksUiState(tasks = emptyList()))

        composeRule.onNodeWithText(
            "No tasks yet.\nPull down to refresh or tap + to create one.",
        ).assertExists()
    }

    @Test
    fun greeting_usesName() {
        setContent(TasksUiState(name = "Zobaer"))

        composeRule.onNodeWithText("Welcome, Zobaer!").assertExists()
    }

    @Test
    fun fab_firesCreateTaskClicked() {
        setContent(TasksUiState(tasks = sampleTasks))

        composeRule.onNodeWithContentDescription("Create task").performClick()

        assertThat(events).contains(TasksUiEvent.CreateTaskClicked)
    }

    @Test
    fun taskRowClick_firesTaskClickedWithId() {
        setContent(TasksUiState(tasks = sampleTasks))

        composeRule.onNodeWithText("Write report").performClick()

        assertThat(events).contains(TasksUiEvent.TaskClicked(1))
    }

    @Test
    fun overflowLogout_firesLogoutClicked() {
        setContent(TasksUiState(tasks = sampleTasks))

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Log out").performClick()

        assertThat(events).contains(TasksUiEvent.LogoutClicked)
    }

    @Test
    fun overflowUpdateAccount_firesUpdateAccountClicked() {
        setContent(TasksUiState(tasks = sampleTasks))

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Update Account").performClick()

        assertThat(events).contains(TasksUiEvent.UpdateAccountClicked)
    }
}
