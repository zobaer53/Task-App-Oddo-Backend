package com.example.odootask.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.odootask.core.mvi.CollectEffect
import com.example.odootask.domain.model.Task
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TasksScreen(
    onNavigateToTaskDetail: (Int) -> Unit,
    onNavigateToCreateTask: () -> Unit,
    onNavigateToUpdateAccount: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: TasksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onEvent(TasksUiEvent.Appeared) }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is TasksUiEffect.NavigateToTaskDetail -> onNavigateToTaskDetail(effect.taskId)
            TasksUiEffect.NavigateToCreateTask -> onNavigateToCreateTask()
            TasksUiEffect.NavigateToUpdateAccount -> onNavigateToUpdateAccount()
            TasksUiEffect.NavigateToLogin -> onNavigateToLogin()
            is TasksUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    TasksContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksContent(
    state: TasksUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (TasksUiEvent) -> Unit,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(text = "Odoo", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text(
                            text = " Mobile",
                            fontSize = 14.sp,
                            letterSpacing = 1.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                },
                actions = { OverflowMenu(onEvent = onEvent) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(TasksUiEvent.CreateTaskClicked) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Create task")
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onEvent(TasksUiEvent.RefreshPulled) },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                state.isLoading && state.tasks.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item {
                            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                                Text(
                                    text = greeting(state.name),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                                if (state.email.isNotBlank()) {
                                    Text(
                                        text = state.email,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 2.dp),
                                    )
                                }
                            }
                        }

                        if (state.tasks.isEmpty()) {
                            item { EmptyState() }
                        } else {
                            items(state.tasks, key = { it.id }) { task ->
                                SwipeToDeleteContainer(
                                    onDelete = { onEvent(TasksUiEvent.TaskDeleted(task.id)) },
                                ) {
                                    TaskCard(
                                        task = task,
                                        onClick = { onEvent(TasksUiEvent.TaskClicked(task.id)) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Three-dot overflow menu in the top bar: update account and logout. */
@Composable
private fun OverflowMenu(onEvent: (TasksUiEvent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text("Update Account") },
            onClick = {
                expanded = false
                onEvent(TasksUiEvent.UpdateAccountClicked)
            },
            leadingIcon = {
                Icon(imageVector = Icons.Filled.Person, contentDescription = null)
            },
        )
        DropdownMenuItem(
            text = { Text("Log out") },
            onClick = {
                expanded = false
                onEvent(TasksUiEvent.LogoutClicked)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                )
            },
        )
    }
}

/**
 * Wraps content in a swipe-to-dismiss gesture (swipe either direction) that fires
 * [onDelete]. The item is removed from state once the delete lands in the cache, so we
 * leave the box settled and let the list recompose rather than holding a dismissed state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    // confirmValueChange can fire more than once per swipe; guard so the delete (and its
    // network unlink) runs only once, otherwise the second call hits a gone record.
    var deleted by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled && !deleted) {
                deleted = true
                onDelete()
            }
            // Don't latch the dismissed state; the row disappears via the data update.
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { SwipeDeleteBackground(dismissState.dismissDirection) },
        content = { content() },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDeleteBackground(direction: SwipeToDismissBoxValue) {
    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        else -> Alignment.CenterEnd
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 24.dp),
        contentAlignment = alignment,
    ) {
        Icon(
            imageVector = Icons.Filled.Delete,
            contentDescription = "Delete task",
            tint = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun TaskCard(task: Task, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDeadline(task.dateDeadline),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (task.stageName.isNotBlank()) {
                StatusChip(stageName = task.stageName)
            }
        }
    }
}

@Composable
private fun StatusChip(stageName: String) {
    val (bg, fg) = stageColors(stageName)
    Box(
        modifier = Modifier
            .background(color = bg, shape = RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(
            text = stageName,
            color = fg,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 96.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "No tasks yet.\nPull down to refresh or tap + to create one.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun greeting(name: String): String =
    if (name.isBlank()) "Welcome!" else "Welcome, $name!"

/** Background + foreground colors for the stage badge, keyed loosely on the stage name. */
private fun stageColors(stageName: String): Pair<Color, Color> {
    val key = stageName.lowercase()
    return when {
        "done" in key || "complete" in key -> Color(0xFFE6F4EA) to Color(0xFF1E7E34)
        "progress" in key -> Color(0xFFFFF3E0) to Color(0xFFB26A00)
        else -> Color(0xFFEDE8FD) to Color(0xFF5538C9)
    }
}

private val deadlineFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun formatDeadline(raw: String?): String {
    if (raw.isNullOrBlank()) return "No deadline"
    return runCatching { LocalDate.parse(raw).format(deadlineFormatter) }.getOrDefault(raw)
}
