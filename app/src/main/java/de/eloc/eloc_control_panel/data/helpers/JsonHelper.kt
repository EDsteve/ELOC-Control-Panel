package de.eloc.eloc_control_panel.data.helpers

import org.json.JSONObject

object JsonHelper {

    const val PATH_SEPARATOR = ":"

    fun getJSONBooleanAttribute(
        path: String,
        jsonObject: JSONObject,
        defaultValue: Boolean = false,
    ): Boolean {
        try {
            val (key, node) = getJSONAttributeNode(path, jsonObject)
            return node?.getBoolean(key) ?: defaultValue
        } catch (_: Exception) {

        }
        return defaultValue
    }

    fun getJSONStringAttribute(
        path: String,
        jsonObject: JSONObject,
        defaultValue: String = ""
    ): String {
        var attribute = defaultValue
        try {
            val (key, node) = getJSONAttributeNode(path, jsonObject)
            attribute = node?.getString(key) ?: defaultValue
        } catch (_: Exception) {

        }
        return attribute
    }

    fun getJSONNumberAttribute(
        path: String,
        jsonObject: JSONObject,
        defaultValue: Double = 0.0
    ): Double {
        try {
            val (key, node) = getJSONAttributeNode(path, jsonObject)
            return node?.getDouble(key) ?: defaultValue
        } catch (_: Exception) {

        }
        return defaultValue
    }

    private fun getJSONAttributeNode(
        path: String,
        jsonObject: JSONObject
    ): Pair<String, JSONObject?> {
        var result: JSONObject? = null
        var key = ""
        try {
            val parts = path.split(PATH_SEPARATOR)
            val iterator = parts.iterator()
            var node = jsonObject
            while (iterator.hasNext()) {
                val name = iterator.next()
                if (!iterator.hasNext()) {
                    result = node
                    key = name
                } else {
                    node = node.getJSONObject(name)
                }
            }
        } catch (_: Exception) {

        }
        return (key to result)
    }
}