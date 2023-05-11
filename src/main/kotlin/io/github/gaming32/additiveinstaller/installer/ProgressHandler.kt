package io.github.gaming32.additiveinstaller.installer

interface ProgressHandler {
    object Null : ProgressHandler {
        override fun prepareNewTaskSet(prepareMessage: String) = Unit

        override fun newTaskSet(numTasks: Int) = Unit

        override fun newTask(title: String) = Unit

        override fun done() = Unit
    }

    fun prepareNewTaskSet(prepareMessage: String)

    fun newTaskSet(numTasks: Int)

    fun newTask(title: String)

    fun done()
}
