package io.github.gaming32.additiveinstaller

import io.github.gaming32.additiveinstaller.installer.ProgressHandler
import javax.swing.JProgressBar
import javax.swing.SwingUtilities

class JProgressBarProgressHandler(private val bar: JProgressBar) : ProgressHandler {
    private var prepared = false

    override fun prepareNewTaskSet(prepareMessage: String) = SwingUtilities.invokeLater {
        prepared = true
        bar.value = 0
        bar.string = prepareMessage
    }

    override fun newTaskSet(numTasks: Int) = SwingUtilities.invokeLater {
        bar.maximum = numTasks
        if (!prepared) {
            bar.value = 0
            bar.string = ""
        }
        prepared = false
    }

    override fun newTask(title: String) = SwingUtilities.invokeLater {
        prepared = false
        bar.value++
        bar.string = title
    }

    override fun done() = SwingUtilities.invokeLater {
        prepared = false
        bar.value = 1
        bar.maximum = 1
        bar.string = "Done!"
    }
}
