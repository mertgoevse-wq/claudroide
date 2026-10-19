package org.claudroide.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.claudroide.app.feature.chat.ChatViewModel
import org.claudroide.app.feature.chat.SessionRecord
import org.claudroide.app.feature.project.UriAvailability
import org.claudroide.app.feature.project.AccessMode
import org.claudroide.app.feature.project.PersistentFolderAccess
import org.claudroide.app.feature.project.ProjectSaverProvider
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelCreationTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    private class TestApplication : Application()

    @Test
    fun testChatViewModelIsAndroidViewModel() {
        assertTrue(
            "ChatViewModel must extend AndroidViewModel so default factory can provide Application",
            AndroidViewModel::class.java.isAssignableFrom(ChatViewModel::class.java)
        )
    }

    @Test
    fun testApplicationConstructorExistsInBytecode() {
        val constructor = ChatViewModel::class.java.getConstructor(Application::class.java)
        assertNotNull("ChatViewModel must expose public (Application) constructor in bytecode", constructor)
    }

    @Test
    fun testFactoryCreationProducesChatViewModel() {
        val app = TestApplication()
        val factory = ChatViewModel.provideFactory(app)
        val vm = factory.create(ChatViewModel::class.java)
        assertNotNull(vm)
        assertTrue(vm is ChatViewModel)
    }

    @Test
    fun testFreshStartupWithoutFolder_neverCrashes() {
        val access = PersistentFolderAccess()
        assertFalse(access.hasGrants)
        assertTrue(access.grantedPaths.isEmpty())

        val availability = UriAvailability { _, _ -> false }
        val report = access.restore(availability)
        assertFalse(report.hasUsable)
        assertFalse(report.allInvalid)
    }
}
