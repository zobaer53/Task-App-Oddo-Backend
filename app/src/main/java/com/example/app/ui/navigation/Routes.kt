package com.example.odootask.ui.navigation

object Routes {
    const val LOGIN = "login"
    const val TASKS = "tasks"
    const val TASK_DETAIL = "task_detail/{taskId}"
    const val CREATE_TASK = "create_task"

    fun taskDetail(taskId: Int) = "task_detail/$taskId"
}
