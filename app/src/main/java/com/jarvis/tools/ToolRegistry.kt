package com.jarvis.tools

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Allowlist of tools the assistant may invoke. Only registered tools can run;
 * anything else is rejected. This is the enforcement point that prevents a
 * model (or prompt injection) from reaching arbitrary device capabilities.
 */
@Singleton
class ToolRegistry @Inject constructor() {

    private val tools = mutableMapOf<String, Tool>()

    fun register(tool: Tool) {
        tools[tool.name] = tool
    }

    fun get(name: String): Tool? = tools[name]

    fun all(): List<Tool> = tools.values.toList()

    fun names(): Set<String> = tools.keys.toSet()
}
