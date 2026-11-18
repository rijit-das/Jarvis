package com.jarvis.tools

import com.jarvis.core.common.DefaultDispatcherProvider
import com.jarvis.core.permissions.PermissionManager
import com.jarvis.core.security.AuditLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class ToolExecutorTest {

    private fun fakeTool(
        name: String,
        permission: String? = null,
        confirmation: Boolean = false
    ): Tool = object : Tool {
        override val name = name
        override val description = "fake $name"
        override val inputSchema = emptyMap<String, String>()
        override val outputDescription = "fake output"
        override val requiredPermission = permission
        override val requiresConfirmation = confirmation
        override val timeoutMs = 5_000L
        override suspend fun execute(arguments: Map<String, String>): ToolResult =
            ToolResult.Success("done")
    }

    private fun executor(
        registry: ToolRegistry,
        permissionManager: PermissionManager
    ): ToolExecutor {
        // log() returns Unit; an unstubbed mock is already a no-op.
        val auditLogger = mock<AuditLogger>()
        return ToolExecutor(
            registry = registry,
            permissionManager = permissionManager,
            auditLogger = auditLogger,
            dispatchers = DefaultDispatcherProvider()
        )
    }

    @Test
    fun `unknown tool is rejected`() = runTest {
        val permissionManager = mock<PermissionManager>()
        val exec = executor(ToolRegistry(), permissionManager)
        val result = exec.execute("shell_exec", emptyMap())
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun `missing permission blocks execution`() = runTest {
        val registry = ToolRegistry().apply {
            register(fakeTool("needs_mic", permission = "android.permission.RECORD_AUDIO"))
        }
        val permissionManager = mock<PermissionManager>().stub {
            on { isGranted(any()) } doReturn false
        }
        val exec = executor(registry, permissionManager)
        val result = exec.execute("needs_mic", emptyMap())
        assertTrue(result is ToolResult.Failure)
        assertTrue((result as ToolResult.Failure).reason.contains("permission"))
    }

    @Test
    fun `declined confirmation blocks execution`() = runTest {
        val registry = ToolRegistry().apply {
            register(fakeTool("dangerous", confirmation = true))
        }
        val permissionManager = mock<PermissionManager>()
        val exec = executor(registry, permissionManager)
        exec.confirmationHandler = { false }
        val result = exec.execute("dangerous", emptyMap())
        assertTrue(result is ToolResult.Failure)
    }

    @Test
    fun `approved confirmation allows execution`() = runTest {
        val registry = ToolRegistry().apply {
            register(fakeTool("dangerous", confirmation = true))
        }
        val permissionManager = mock<PermissionManager>()
        val exec = executor(registry, permissionManager)
        exec.confirmationHandler = { true }
        val result = exec.execute("dangerous", emptyMap())
        assertTrue(result is ToolResult.Success)
    }

    @Test
    fun `tool without gates executes`() = runTest {
        val registry = ToolRegistry().apply { register(fakeTool("safe")) }
        val permissionManager = mock<PermissionManager>()
        val exec = executor(registry, permissionManager)
        val result = exec.execute("safe", emptyMap())
        assertTrue(result is ToolResult.Success)
    }
}
