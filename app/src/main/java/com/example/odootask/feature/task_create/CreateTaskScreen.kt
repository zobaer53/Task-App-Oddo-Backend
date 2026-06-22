package com.example.odootask.feature.task_create

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.odootask.core.mvi.CollectEffect
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun CreateTaskScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateTaskViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onEvent(CreateTaskUiEvent.Appeared) }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            CreateTaskUiEffect.NavigateBack -> onNavigateBack()
            is CreateTaskUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
        }
    }

    CreateTaskContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = viewModel::onEvent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateTaskContent(
    state: CreateTaskUiState,
    snackbarHostState: SnackbarHostState,
    onEvent: (CreateTaskUiEvent) -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Create Task", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(CreateTaskUiEvent.BackClicked) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = { onEvent(CreateTaskUiEvent.NameChanged(it)) },
                label = { Text("Title") },
                singleLine = true,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = { onEvent(CreateTaskUiEvent.DescriptionChanged(it)) },
                label = { Text("Description") },
                placeholder = { Text("Enter task details") },
                enabled = !state.isSaving,
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            // Read-only field that opens the date picker dialog when tapped.
            Box {
                OutlinedTextField(
                    value = formatDeadline(state.deadline),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Due Date") },
                    trailingIcon = {
                        Icon(imageVector = Icons.Filled.DateRange, contentDescription = null)
                    },
                    colors = disabledFieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickableNoRipple(enabled = !state.isSaving) { showDatePicker = true },
                )
            }

            Box(modifier = Modifier.weight(1f))

            Button(
                onClick = { onEvent(CreateTaskUiEvent.CreateClicked) },
                enabled = state.canCreate,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(text = "CREATE TASK", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.deadline.toEpochMillisOrNull(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onEvent(CreateTaskUiEvent.DeadlineSelected(millis.toIsoDate()))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun disabledFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    disabledTextColor = MaterialTheme.colorScheme.onSurface,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

/** Tap target without the visual ripple, so the field reads as a plain input. */
@Composable
private fun Modifier.clickableNoRipple(enabled: Boolean, onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        enabled = enabled,
        indication = null,
        interactionSource = interactionSource,
        onClick = onClick,
    )
}

private val displayFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

private fun formatDeadline(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return runCatching { LocalDate.parse(isoDate).format(displayFormatter) }.getOrDefault(isoDate)
}

private fun String.toEpochMillisOrNull(): Long? =
    runCatching { LocalDate.parse(this).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }
        .getOrNull()

private fun Long.toIsoDate(): String =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()
