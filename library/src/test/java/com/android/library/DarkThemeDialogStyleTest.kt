package com.android.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.pow

/**
 * Dark-mode AlertDialog buttons tint from colorPrimary. AppTheme.Dark keeps indigo
 * for the Toolbar, so dialog themes must override colorPrimary to stay readable on
 * the Material dark dialog surface (#424242).
 */
class DarkThemeDialogStyleTest {

    @Test
    fun darkDialogColorPrimaryMeetsContrastOnDarkSurface() {
        val styles = loadStyles()
        val colors = loadColors()
        val dialogColor = effectiveDialogColorPrimary(styles, colors, "alertDialogTheme")
        val ratio = contrastRatio(dialogColor, DARK_DIALOG_SURFACE)

        assertTrue(
            "Dark dialog colorPrimary #${dialogColor.toString(16)} contrast $ratio is below 4.5:1 on #424242",
            ratio >= WCAG_AA_NORMAL_TEXT
        )
    }

    @Test
    fun appThemeDarkWiresLightPrimaryOnBothDialogThemes() {
        val styles = loadStyles()
        val colors = loadColors()
        val dark = style(styles, "AppTheme.Dark")

        assertEquals("@style/AppTheme.Dark.AlertDialog", item(dark, "alertDialogTheme"))
        val alert = style(styles, "AppTheme.Dark.AlertDialog")
        assertEquals("ThemeOverlay.MaterialComponents.Dialog.Alert", alert.getAttribute("parent"))
        assertEquals("@color/colorPrimaryLight", item(alert, "colorPrimary"))

        assertEquals("@style/AppTheme.Dark.MaterialAlertDialog", item(dark, "materialAlertDialogTheme"))
        val material = style(styles, "AppTheme.Dark.MaterialAlertDialog")
        assertEquals(
            "ThemeOverlay.MaterialComponents.MaterialAlertDialog",
            material.getAttribute("parent")
        )
        assertEquals("@color/colorPrimaryLight", item(material, "colorPrimary"))

        assertEquals(COLOR_PRIMARY_LIGHT, resolveColor("@color/colorPrimaryLight", colors))
        assertEquals(
            COLOR_PRIMARY_LIGHT,
            effectiveDialogColorPrimary(styles, colors, "materialAlertDialogTheme")
        )
    }

    @Test
    fun toolbarAndLightThemeKeepIndigoPrimary() {
        val styles = loadStyles()
        val colors = loadColors()

        val darkPrimaryRef = item(style(styles, "AppTheme.Dark"), "colorPrimary")
        val lightPrimaryRef = item(style(styles, "AppTheme"), "colorPrimary")
        assertEquals("@color/colorPrimary", darkPrimaryRef)
        assertEquals("@color/colorPrimary", lightPrimaryRef)

        val darkPrimary = resolveColor(darkPrimaryRef!!, colors)
        val lightPrimary = resolveColor(lightPrimaryRef!!, colors)
        assertEquals(COLOR_PRIMARY, darkPrimary)
        assertEquals(COLOR_PRIMARY, lightPrimary)
        assertTrue(contrastRatio(darkPrimary, WHITE) >= WCAG_AA_NORMAL_TEXT)
        assertTrue(contrastRatio(lightPrimary, WHITE) >= WCAG_AA_NORMAL_TEXT)
    }

    private fun effectiveDialogColorPrimary(
        styles: Document,
        colors: Map<String, String>,
        dialogThemeAttr: String
    ): Int {
        val dark = style(styles, "AppTheme.Dark")
        val dialogThemeRef = item(dark, dialogThemeAttr)
        val colorRef = if (dialogThemeRef == null) {
            item(dark, "colorPrimary")
        } else {
            item(style(styles, dialogThemeRef.removePrefix("@style/")), "colorPrimary")
        }
        return resolveColor(colorRef ?: error("No colorPrimary for $dialogThemeAttr"), colors)
    }

    private fun loadStyles(): Document = parse(resourceFile("styles.xml"))

    private fun loadColors(): Map<String, String> {
        val nodes = parse(resourceFile("colors.xml")).getElementsByTagName("color")
        val colors = mutableMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val el = nodes.item(i) as Element
            colors[el.getAttribute("name")] = el.textContent.trim()
        }
        return colors
    }

    private fun resourceFile(name: String): File {
        val candidates = listOf(
            File("src/main/res/values", name),
            File("library/src/main/res/values", name)
        )
        return candidates.firstOrNull { it.isFile }
            ?: error(
                "Missing $name. Looked in ${candidates.joinToString { it.path }} " +
                    "from ${File(".").absolutePath}"
            )
    }

    private fun parse(file: File): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = false
        return factory.newDocumentBuilder().parse(file)
    }

    private fun style(doc: Document, name: String): Element {
        val styles = doc.getElementsByTagName("style")
        for (i in 0 until styles.length) {
            val el = styles.item(i) as Element
            if (el.getAttribute("name") == name) return el
        }
        error("Missing style $name")
    }

    private fun item(style: Element, name: String): String? {
        val items = style.getElementsByTagName("item")
        for (i in 0 until items.length) {
            val el = items.item(i) as Element
            if (el.parentNode !== style) continue
            if (el.getAttribute("name") == name) return el.textContent.trim()
        }
        return null
    }

    private fun resolveColor(ref: String, colors: Map<String, String>): Int {
        var value = ref
        val seen = mutableSetOf<String>()
        while (value.startsWith("@color/")) {
            val name = value.removePrefix("@color/")
            check(seen.add(name)) { "Color cycle at $name" }
            value = colors[name] ?: error("Missing color $name")
        }
        return parseHex(value)
    }

    private fun parseHex(hex: String): Int {
        val raw = hex.removePrefix("#")
        val rgb = when (raw.length) {
            3 -> raw.map { "$it$it" }.joinToString("")
            6 -> raw
            8 -> {
                // Text color must be opaque: a translucent value would blend with the surface
                check(raw.substring(0, 2).equals("FF", ignoreCase = true)) { "Non-opaque color $hex" }
                raw.substring(2)
            }
            else -> error("Bad color $hex")
        }
        return rgb.toInt(16)
    }

    private fun contrastRatio(foreground: Int, background: Int): Double {
        val lighter = maxOf(relativeLuminance(foreground), relativeLuminance(background))
        val darker = minOf(relativeLuminance(foreground), relativeLuminance(background))
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(color: Int): Double {
        val r = channel((color shr 16) and 0xFF)
        val g = channel((color shr 8) and 0xFF)
        val b = channel(color and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun channel(component: Int): Double {
        val s = component / 255.0
        return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }

    companion object {
        private const val WCAG_AA_NORMAL_TEXT = 4.5
        private const val DARK_DIALOG_SURFACE = 0x424242
        private const val WHITE = 0xFFFFFF
        private const val COLOR_PRIMARY = 0x3F51B5
        private const val COLOR_PRIMARY_LIGHT = 0xC5CAE9
    }
}
