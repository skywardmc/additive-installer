package io.github.gaming32.additiveinstaller

interface ProgressHandler {
    fun prepareNewTaskSet(prepareMessage: String)

    fun newTaskSet(numTasks: Int)

    fun newTask(title: String)

    fun done()
}