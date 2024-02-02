package io.github.teamteds.tedsmodpacksinstaller

interface ProgressHandler {
    fun prepareNewTaskSet(prepareMessage: String)

    fun newTaskSet(numTasks: Int)

    fun newTask(title: String)

    fun done()
}