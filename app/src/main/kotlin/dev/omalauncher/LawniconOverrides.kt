package dev.omalauncher

import dev.omalauncher.upgrade.Versioned
import org.json.JSONObject

/** Persist resource names, never resource IDs or bitmaps: pack updates may renumber resources. */
internal object LawniconOverrides {
    private val name = Regex("[a-z][a-z0-9_]*")

    fun validName(value: String) = name.matches(value)

    fun resourceName(packageName: String?, resourceName: String?): String? {
        if (packageName != Lawnicons.PACKAGE) return null
        val prefix = "${Lawnicons.PACKAGE}:drawable/"
        if (resourceName == null || !resourceName.startsWith(prefix)) return null
        return resourceName.removePrefix(prefix).takeIf(::validName)
    }

    fun read(text: String?): Map<String, String> {
        val data = Versioned.read(text, 1) as? JSONObject ?: return emptyMap()
        return data.keys().asSequence().mapNotNull { key ->
            (data.opt(key) as? String)?.takeIf(::validName)?.let { key to it }
        }.toMap()
    }

    fun write(values: Map<String, String>): String = Versioned.write(1, JSONObject(values))
}
