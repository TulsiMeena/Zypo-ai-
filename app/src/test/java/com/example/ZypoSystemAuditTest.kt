package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.security.SecureKeyStorage
import com.example.navigation.ZypoRoutes
import com.example.tools.CalculatorTool
import com.example.tools.DateTimeTool
import com.example.tools.ToolRegistry
import com.example.tools.UnitConversionTool
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ZypoSystemAuditTest {

    private lateinit var context: Context
    private lateinit var secureKeyStorage: SecureKeyStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        secureKeyStorage = SecureKeyStorage(context)
    }

    @Test
    fun `test secure key storage encryption and masking`() {
        val testApiKey = "AIzaSyTestApiKey1234567890"

        // Save Key
        secureKeyStorage.saveGeminiApiKey(testApiKey)

        // Retrieve Key
        val retrieved = secureKeyStorage.getGeminiApiKey()
        assertEquals(testApiKey, retrieved)

        // Mask Key
        val masked = secureKeyStorage.maskApiKey(testApiKey)
        assertTrue(masked.startsWith("AIza"))
        assertTrue(masked.endsWith("7890"))
        assertTrue(masked.contains("••••"))

        // Clear Key
        secureKeyStorage.clearGeminiApiKey()
        assertEquals(null, secureKeyStorage.getGeminiApiKey())
    }

    @Test
    fun `test calculator tool execution`() = runBlocking {
        val tool = CalculatorTool()
        val args = JSONObject().apply {
            put("expression", "2 + 3 * 4")
        }
        val result = tool.execute(args)
        assertTrue(result.optBoolean("success"))
        assertEquals("14", result.optString("result"))
    }

    @Test
    fun `test unit conversion tool execution`() = runBlocking {
        val tool = UnitConversionTool()
        val args = JSONObject().apply {
            put("value", 100.0)
            put("fromUnit", "m")
            put("toUnit", "km")
        }
        val result = tool.execute(args)
        assertTrue(result.optBoolean("success"))
        assertEquals(0.1, result.optDouble("result"), 0.001)
    }

    @Test
    fun `test date time tool execution`() = runBlocking {
        val tool = DateTimeTool()
        val args = JSONObject()
        val result = tool.execute(args)
        assertTrue(result.optBoolean("success"))
        assertNotNull(result.optString("formatted"))
    }

    @Test
    fun `test tool registry registration`() {
        val registry = ToolRegistry()
        assertEquals(5, registry.getAllTools().size)
        assertNotNull(registry.getTool("calculator"))
        assertNotNull(registry.getTool("unitConversion"))
        assertNotNull(registry.getTool("dateTime"))
        assertNotNull(registry.getTool("openWebsite"))
        assertNotNull(registry.getTool("webSearch"))
    }

    @Test
    fun `test navigation routes constants`() {
        assertEquals("splash", ZypoRoutes.SPLASH)
        assertEquals("chat", ZypoRoutes.CHAT)
        assertEquals("settings", ZypoRoutes.SETTINGS)
        assertEquals("api_settings", ZypoRoutes.API_SETTINGS)
        assertEquals("developer_diagnostics", ZypoRoutes.DEVELOPER_DIAGNOSTICS)
    }
}
