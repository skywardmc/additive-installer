package io.github.gaming32.additiveinstaller

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import io.github.oshai.KotlinLogging
import java.awt.BorderLayout
import javax.swing.*
import kotlin.concurrent.thread
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory

private val logger = KotlinLogging.logger {}

fun main() {
    if (isDarkMode()) {
        if (operatingSystem == OperatingSystem.MACOS) {
            FlatMacDarkLaf.setup()
        } else {
            FlatDarkLaf.setup()
        }
    } else {
        if (operatingSystem == OperatingSystem.MACOS) {
            FlatMacLightLaf.setup()
        } else {
            FlatLightLaf.setup()
        }
    }

    val additive = Modpack("additive")
    val adrenaline = Modpack("adrenaline")
    var selectedPack = additive

    val installDestChooser = JFileChooser(PackInstaller.DOT_MINECRAFT.toString()).apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
        dialogTitle = "Select installation folder"
        isAcceptAllFileFilterUsed = false
        resetChoosableFileFilters()
    }

    SwingUtilities.invokeLater { JFrame(selectedPack.windowTitle).apply root@ {
        iconImage = selectedPack.image

        val iconLabel = JLabel(ImageIcon(selectedPack.image))

        val packVersion = JComboBox<String>()

        lateinit var setupMinecraftVersions: () -> Unit

        val minecraftVersion = JComboBox<String>().apply {
            addItemListener {
                val gameVersion = selectedItem as? String ?: return@addItemListener
                packVersion.removeAllItems()
                selectedPack.versions[gameVersion]
                    ?.keys
                    ?.forEach(packVersion::addItem)
            }
        }

        val includeUnsupportedMinecraft = JCheckBox("Include unsupported Minecraft versions").apply {
            addActionListener { setupMinecraftVersions() }
        }

        setupMinecraftVersions = {
            val mcVersion = minecraftVersion.selectedItem
            minecraftVersion.removeAllItems()
            val all = includeUnsupportedMinecraft.isSelected
            val supported = selectedPack.supportedMcVersions
            selectedPack.versions
                .keys
                .asSequence()
                .filter { all || it in supported }
                .forEach(minecraftVersion::addItem)
            if (mcVersion != null) {
                minecraftVersion.selectedItem = mcVersion
            }
        }
        setupMinecraftVersions()

        val includeFeatures = JCheckBox("Include non-performance features").apply {
            isSelected = true
            addActionListener {
                selectedPack = if (isSelected) additive else adrenaline
                title = selectedPack.windowTitle
                iconImage = selectedPack.image
                iconLabel.icon = ImageIcon(selectedPack.image)

                setupMinecraftVersions()
            }
        }

        val installProgress = JProgressBar().apply {
            isStringPainted = true
        }

        val installationDir = JTextField(PackInstaller.DOT_MINECRAFT.toString())
        val browseButton = JButton("Browse...").apply {
            addActionListener {
                if (installDestChooser.showOpenDialog(this@root) != JFileChooser.APPROVE_OPTION) {
                    return@addActionListener
                }
                installationDir.text = installDestChooser.selectedFile.absolutePath
            }
        }

        lateinit var enableOptions: (Boolean) -> Unit

        val install = JButton("Install!").apply {
            addActionListener {
                enableOptions(false)
                val selectedMcVersion = minecraftVersion.selectedItem
                val selectedPackVersion = packVersion.selectedItem
                val destinationPath = Path(installationDir.text)
                if (!destinationPath.isDirectory()) {
                    if (destinationPath.exists()) {
                        JOptionPane.showMessageDialog(
                            this@root,
                            "Installation dir exists and is not a directory.",
                            title, JOptionPane.INFORMATION_MESSAGE
                        )
                    } else {
                        destinationPath.createDirectories()
                    }
                }
                thread(isDaemon = true, name = "InstallThread") {
                    val error = try {
                        selectedPack.versions[selectedMcVersion]
                            ?.get(selectedPackVersion)
                            ?.install(destinationPath, JProgressBarProgressHandler(installProgress))
                            ?: throw IllegalStateException(
                                "Couldn't find pack version $selectedPackVersion for $selectedMcVersion"
                            )
                        null
                    } catch (t: Throwable) {
                        logger.error("Failed to install pack", t)
                        t
                    }
                    SwingUtilities.invokeLater {
                        enableOptions(true)
                        if (error == null) {
                            JOptionPane.showMessageDialog(
                                this@root,
                                "Installation success!",
                                title, JOptionPane.INFORMATION_MESSAGE
                            )
                        } else {
                            JOptionPane.showMessageDialog(
                                this@root,
                                "Installation failed.\n${error.localizedMessage}",
                                title, JOptionPane.INFORMATION_MESSAGE
                            )
                        }
                    }
                }
            }
        }

        enableOptions = {
            includeFeatures.isEnabled = it
            minecraftVersion.isEnabled = it
            includeUnsupportedMinecraft.isEnabled = it
            packVersion.isEnabled = it
            installationDir.isEnabled = it
            browseButton.isEnabled = it
            install.isEnabled = it
        }

        contentPane = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            add(JPanel().apply {
                layout = BorderLayout()
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                add(iconLabel, BorderLayout.PAGE_START)
            })
            add(Box.createVerticalStrut(15))
            add(includeFeatures.withLabel())
            add(Box.createVerticalStrut(15))
            add(minecraftVersion.withLabel("Minecraft version: "))
            add(Box.createVerticalStrut(5))
            add(includeUnsupportedMinecraft.withLabel())
            add(Box.createVerticalStrut(15))
            add(packVersion.withLabel("Pack version: "))
            add(Box.createVerticalStrut(15))
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.LINE_AXIS)
                add(JLabel("Install to: "))
                add(installationDir)
                add(Box.createHorizontalStrut(5))
                add(browseButton)
            })
            add(Box.createVerticalStrut(15))
            add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                add(JPanel().apply {
                    layout = BorderLayout()
                    add(install)
                })
                add(Box.createVerticalStrut(10))
                add(installProgress)
            })
        }

        isResizable = false

        pack()
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        setLocationRelativeTo(null)
        isVisible = true
    } }
}
