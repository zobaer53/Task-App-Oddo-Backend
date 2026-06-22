package com.example.odootask.data.local.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.odootask.data.local.AppDatabase
import com.example.odootask.data.local.entity.TaskEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Phase 16 — exercises [TaskDao] against a real in-memory SQLite instance so the persistence
 * behaviour the repositories rely on (reactive observe, atomic replace, targeted mutations) is
 * verified end-to-end, not mocked.
 */
@RunWith(AndroidJUnit4::class)
class TaskDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: TaskDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        dao = db.taskDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun entity(
        id: Int,
        name: String = "Task $id",
        stageId: Int = 1,
        stageName: String = "To Do",
        dateDeadline: String? = null,
    ) = TaskEntity(
        id = id,
        name = name,
        stageId = stageId,
        stageName = stageName,
        dateDeadline = dateDeadline,
    )

    @Test
    fun upsertAll_thenObserveAll_emitsRowsOrderedByIdDesc() = runTest {
        dao.upsertAll(listOf(entity(1), entity(2), entity(3)))

        val rows = dao.observeAll().first()

        assertThat(rows.map { it.id }).containsExactly(3, 2, 1).inOrder()
    }

    @Test
    fun upsert_replacesExistingRowWithSamePrimaryKey() = runTest {
        dao.upsertAll(listOf(entity(1, name = "Original", stageName = "To Do")))

        dao.upsertAll(listOf(entity(1, name = "Updated", stageId = 2, stageName = "Done")))

        val rows = dao.observeAll().first()
        assertThat(rows).hasSize(1)
        assertThat(rows.first().name).isEqualTo("Updated")
        assertThat(rows.first().stageName).isEqualTo("Done")
    }

    @Test
    fun getById_returnsMatchingRow_orNullWhenAbsent() = runTest {
        dao.upsertAll(listOf(entity(7, name = "Findable", dateDeadline = "2026-07-01")))

        val found = dao.getById(7)
        assertThat(found).isNotNull()
        assertThat(found!!.name).isEqualTo("Findable")
        assertThat(found.dateDeadline).isEqualTo("2026-07-01")

        assertThat(dao.getById(999)).isNull()
    }

    @Test
    fun updateStage_mutatesOnlyTheTargetRow() = runTest {
        dao.upsertAll(listOf(entity(1, stageId = 1, stageName = "To Do"), entity(2, stageId = 1, stageName = "To Do")))

        dao.updateStage(id = 1, stageId = 3, stageName = "Done")

        val target = dao.getById(1)!!
        val untouched = dao.getById(2)!!
        assertThat(target.stageId).isEqualTo(3)
        assertThat(target.stageName).isEqualTo("Done")
        assertThat(untouched.stageId).isEqualTo(1)
        assertThat(untouched.stageName).isEqualTo("To Do")
    }

    @Test
    fun deleteById_removesOnlyTheTargetRow() = runTest {
        dao.upsertAll(listOf(entity(1), entity(2)))

        dao.deleteById(1)

        val rows = dao.observeAll().first()
        assertThat(rows.map { it.id }).containsExactly(2)
    }

    @Test
    fun clear_emptiesTheTable() = runTest {
        dao.upsertAll(listOf(entity(1), entity(2), entity(3)))

        dao.clear()

        assertThat(dao.observeAll().first()).isEmpty()
    }

    @Test
    fun replaceAll_swapsTheCachedSetAtomically() = runTest {
        dao.upsertAll(listOf(entity(1), entity(2)))

        dao.replaceAll(listOf(entity(10), entity(11), entity(12)))

        val rows = dao.observeAll().first()
        assertThat(rows.map { it.id }).containsExactly(12, 11, 10).inOrder()
    }

    @Test
    fun observeAll_isReactive_andReplaceAllNeverEmitsEmptyMidTransaction() = runTest {
        dao.upsertAll(listOf(entity(1), entity(2)))

        dao.observeAll().test {
            // Initial emission with the seeded rows.
            assertThat(awaitItem().map { it.id }).containsExactly(2, 1).inOrder()

            // An atomic swap should surface as a single new emission — never an empty flicker
            // from the clear() inside the @Transaction.
            dao.replaceAll(listOf(entity(5), entity(6)))

            val next = awaitItem()
            assertThat(next).isNotEmpty()
            assertThat(next.map { it.id }).containsExactly(6, 5).inOrder()

            cancelAndIgnoreRemainingEvents()
        }
    }
}
