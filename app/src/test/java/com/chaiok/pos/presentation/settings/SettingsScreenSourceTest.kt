package com.chaiok.pos.presentation.settings

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsScreenSourceTest {
    @Test
    fun `settings screen source does not contain cash protocol picker`() {
        val sourceFile = listOf(
            File("src/main/java/com/chaiok/pos/presentation/settings/SettingsScreen.kt"),
            File("app/src/main/java/com/chaiok/pos/presentation/settings/SettingsScreen.kt")
        ).first { it.exists() }
        val source = sourceFile.readText()

        assertFalse(source.contains("Протокол кассы"))
        assertFalse(source.contains("Chai" + "OK JSON"))
        assertFalse(source.contains("pc" + "Ecr" + "Protocol"))
    }
}
