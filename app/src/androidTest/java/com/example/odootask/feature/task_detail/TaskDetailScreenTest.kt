package com.example.odootask.feature.task_detail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.odootask.ui.theme.AppTheme
import com.example.odootask.domain.model.Stage
import com.example.odootask.domain.model.Task
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Phase 17 Compose UI tests for the task detail / update screen. Drives the stateless
 * [TaskDetailContent] with a hand-built [TaskDetailUiState] and a recording onEvent spy.
 */
class TaskDetailScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<TaskDetailUiEvent>()

    private fun setContent(state: TaskDetailUiState) {
        composeRule.setContent {
            AppTheme {
                TaskDetailContent(
                    state = state,
                    snackbarHostState = remember { SnackbarHostState() },
                    onEvent = { events.add(it) },
                )
            }
        }
    }

    private val task = Task(
        id = 1,
        name = "Write report",
        stageId = 1,
        stageName = "To Do",
        dateDeadline = "2026-07-01",
    )
    private val stages = listOf(
        Stage(id = 1, name = "To Do"),
        Stage(id = 2, name = "In Progress"),
        Stage(id = 3, name = "Done"),
    )

    @Test
    fun taskNameAndDeadline_areRendered() {
        setContent(
            TaskDetailUiState(task = task, stages = stages, selectedStage = stages[0]),
        )

        composeRule.onNodeWithText("Write report").assertExists()
        composeRule.onNodeWithText("Due Jul 1, 2026").assertExists()
    }

    @Test
    fun missingTask_showsNotFound() {
        setContent(TaskDetailUiState(task = null))

        composeRule.onNodeWithText("Task not found").assertExists()
    }

    @Test
    fun dropdown_listsStages_andSelectingFiresEvent() {
        setContent(
            TaskDetailUiState(task = task, stages = stages, selectedStage = stages[0]),
        )

        composeRule.onNodeWithContentDescription("Back").assertExists()
        // Open the dropdown via the read-only status field showing the current stage.
        composeRule.onNodeWithText("To Do").performClick()
        composeRule.onNodeWithText("In Progress").performClick()

        assertThat(events).contains(TaskDetailUiEvent.StageSelected(stages[1]))
    }

    @Test
    fun updateButton_disabledWhenSameStageSelected() {
        setContent(
            TaskDetailUiState(task = task, stages = stages, selectedStage = stages[0]),
        )

        composeRule.onNodeWithText("UPDATE STATUS").assertIsNotEnabled()
    }

    @Test
    fun updateButton_enabledWhenDifferentStageSelected() {
        setContent(
            TaskDetailUiState(task = task, stages = stages, selectedStage = stages[1]),
        )

        composeRule.onNodeWithText("UPDATE STATUS").assertIsEnabled()
    }

    @Test
    fun updateButtonClick_firesUpdateClicked() {
        setContent(
            TaskDetailUiState(task = task, stages = stages, selectedStage = stages[1]),
        )

        composeRule.onNodeWithText("UPDATE STATUS").performClick()

        assertThat(events).contains(TaskDetailUiEvent.UpdateClicked)
    }

    @Test
    fun backButton_firesBackClicked() {
        setContent(
            TaskDetailUiState(task = task, stages = stages, selectedStage = stages[0]),
        )

        composeRule.onNodeWithContentDescription("Back").performClick()

        assertThat(events).contains(TaskDetailUiEvent.BackClicked)
    }
}
