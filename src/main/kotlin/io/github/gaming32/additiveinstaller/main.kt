package io.github.gaming32.additiveinstaller

import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.themes.FlatMacDarkLaf
import com.formdev.flatlaf.themes.FlatMacLightLaf
import java.awt.BorderLayout
import javax.swing.*

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

    SwingUtilities.invokeLater { JFrame(selectedPack.windowTitle).apply {
        iconImage = selectedPack.image

        val iconLabel = JLabel(ImageIcon(selectedPack.image))

        val packVersion = JComboBox<String>().apply {
        }

        val minecraftVersion = JComboBox<String>().apply {
            addItemListener {
                val gameVersion = selectedItem as? String ?: return@addItemListener
                packVersion.removeAllItems()
                selectedPack.versions[gameVersion]
                    ?.keys
                    ?.forEach(packVersion::addItem)
            }
            selectedPack.versions.keys.forEach(this::addItem)
        }

        val includeFeatures = JCheckBox("Include non-performance features").apply {
            isSelected = true
            addActionListener {
                selectedPack = if (isSelected) additive else adrenaline
                title = selectedPack.windowTitle
                iconImage = selectedPack.image
                iconLabel.icon = ImageIcon(selectedPack.image)

                val mcVersion = minecraftVersion.selectedItem
                minecraftVersion.removeAllItems()
                selectedPack.versions.keys.forEach(minecraftVersion::addItem)
                minecraftVersion.selectedItem = mcVersion
            }
        }

        contentPane = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            add(JPanel().apply {
                layout = BorderLayout()
                border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
                add(iconLabel, BorderLayout.PAGE_START)
            })
            add(Box.createVerticalStrut(10))
            add(includeFeatures)
            add(Box.createVerticalStrut(5))
            add(minecraftVersion.withLabel("Minecraft version: "))
            add(Box.createVerticalStrut(5))
            add(packVersion.withLabel("Pack version: "))
        }

        isResizable = false

        pack()
        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        isVisible = true
    } }
}
