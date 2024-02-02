package io.github.teamteds.tedsmodpacksinstaller

import io.github.oshai.KotlinLogging
import javax.swing.JProgressBar
import javax.swing.SwingUtilities

private val logger = KotlinLogging.logger {}

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

    override fun newTask(title: String) {
        logger.info("New task: $title")
        SwingUtilities.invokeLater {
            prepared = false
            bar.value++
            bar.string = "$title (${bar.value}/${bar.maximum})"
        }
    }

    override fun done() = SwingUtilities.invokeLater {
        prepared = false
        bar.value = 1
        bar.maximum = 1
        bar.string = I18N.getString("done")
    }
}
