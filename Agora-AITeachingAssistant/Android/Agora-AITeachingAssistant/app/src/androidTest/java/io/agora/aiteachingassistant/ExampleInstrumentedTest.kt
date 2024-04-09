package io.agora.aiteachingassistant

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.agora.aiteachingassistant.utils.Utils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("io.agora.ailivestreaming", appContext.packageName)
    }

    @Test
    fun testUtils() {
        assertTrue(Utils.onlyChinese("测试"))
        assertFalse(Utils.onlyChinese("test"))
        assertTrue(Utils.onlyChinese("好啊！今天去，怎么样？"))
        assertTrue(Utils.onlyChinese("好啊!今天去,怎么样?"))
        assertFalse(Utils.onlyChinese("好啊aa"))
        assertFalse(Utils.onlyChinese("what is your name?"))
        assertFalse(Utils.onlyChinese("what is your name？"))
        assertTrue(Utils.onlyChinese("好啊！今天去，怎么样？012"))
        assertTrue(Utils.onlyChinese("好啊！今天去，怎么样？345"))
        assertTrue(Utils.onlyChinese("3333"))
    }
}