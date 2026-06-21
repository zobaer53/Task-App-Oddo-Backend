package com.example.odootask.util

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

/**
 * Phase 13 sanity check: proves the test toolchain (JUnit, Truth, coroutines-test, Turbine),
 * the [MainDispatcherRule], the model builders, and the repository fakes all wire up and run.
 * The real test suites land in phases 14–15.
 */
class ToolchainSmokeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun builders_and_fakes_wire_up() = runTest {
        val repo = FakeTaskRepository()
        repo.emitTasks(listOf(task(id = 1), task(id = 2)))

        repo.observeTasks().test {
            assertThat(awaitItem()).hasSize(2)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(repo.getTask(1)).isEqualTo(task(id = 1))
    }
}
