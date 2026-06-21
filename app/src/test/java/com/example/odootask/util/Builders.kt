package com.example.odootask.util

import com.example.odootask.domain.model.OdooUser
import com.example.odootask.domain.model.Project
import com.example.odootask.domain.model.Stage
import com.example.odootask.domain.model.Task

/**
 * Domain-model builders with sensible defaults, so tests only spell out the fields they care
 * about. Shared across the phase 14–15 unit-test suites.
 */

fun user(
    uid: Int = 1,
    database: String = "zobaer",
    username: String = "admin",
    password: String = "admin",
) = OdooUser(uid = uid, database = database, username = username, password = password)

fun task(
    id: Int = 1,
    name: String = "Task $id",
    stageId: Int = 1,
    stageName: String = "In Progress",
    dateDeadline: String? = null,
) = Task(id = id, name = name, stageId = stageId, stageName = stageName, dateDeadline = dateDeadline)

fun stage(id: Int = 1, name: String = "In Progress") = Stage(id = id, name = name)

fun project(id: Int = 1, name: String = "Internal") = Project(id = id, name = name)
