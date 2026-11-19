package com.jarvis.ai.model

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Device capability tier. Drives which features JARVIS attempts. */
enum class CapabilityTier { LOW, MEDIUM, HIGH }

/** On-device LLM availability. v1 ships no model — this is reported honestly. */
sealed interface LocalLlmStatus {
    data object NotInstalled : LocalLlmStatus
    data class Ready(val modelPath: String) : LocalLlmStatus
    data class Incompatible(val reason: String) : LocalLlmStatus
}

data class DeviceReport(
    val tier: CapabilityTier,
    val totalMemoryBytes: Long,
    val availableStorageBytes: Long,
    val androidVersion: Int,
    val llmStatus: LocalLlmStatus
)

/**
 * Checks device compatibility before attempting to load any local model.
 * Never promises a large model can run on every phone: low-memory devices
 * get the deterministic intent engine, full stop.
 */
@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /** Pure tier computation — unit-testable without a device. */
    fun tierFor(totalMemoryBytes: Long, freeStorageBytes: Long): CapabilityTier = when {
        totalMemoryBytes < 3L * 1024 * 1024 * 1024 || freeStorageBytes < 2L * 1024 * 1024 * 1024 -> CapabilityTier.LOW
        totalMemoryBytes < 6L * 1024 * 1024 * 1024 -> CapabilityTier.MEDIUM
        else -> CapabilityTier.HIGH
    }

    fun evaluate(): DeviceReport {
        val totalMemory = totalMemoryBytes()
        val freeStorage = availableStorageBytes()
        val tier = tierFor(totalMemory, freeStorage)
        return DeviceReport(
            tier = tier,
            totalMemoryBytes = totalMemory,
            availableStorageBytes = freeStorage,
            androidVersion = Build.VERSION.SDK_INT,
            llmStatus = localLlmStatus(tier)
        )
    }

    /**
     * v1: no model is bundled, so this honestly reports NotInstalled.
     * When a model file is present in the app-private models directory AND the
     * device tier supports it, it reports Ready. The actual inference backend
     * (MediaPipe LLM Inference API) is a documented roadmap item — see
     * docs/KNOWN_LIMITATIONS.md.
     */
    fun localLlmStatus(tier: CapabilityTier): LocalLlmStatus {
        if (tier == CapabilityTier.LOW) {
            return LocalLlmStatus.Incompatible("Device has insufficient RAM for an on-device LLM.")
        }
        val modelFile = File(context.filesDir, "models/jarvis-llm.bin")
        return if (modelFile.exists() && modelFile.length() > 0) {
            LocalLlmStatus.Ready(modelFile.absolutePath)
        } else {
            LocalLlmStatus.NotInstalled
        }
    }

    private fun totalMemoryBytes(): Long {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        return info.totalMem
    }

    private fun availableStorageBytes(): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBytes
    }
}
