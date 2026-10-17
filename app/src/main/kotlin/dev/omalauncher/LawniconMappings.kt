package dev.omalauncher

import org.xmlpull.v1.XmlPullParser

/** Icon pack mappings only: no app entries, shortcuts, or artwork are created. */
internal class LawniconMappings {
    private val activities = HashMap<String, String>()
    private val packages = HashMap<String, String>()

    fun read(parser: XmlPullParser, resourceName: (Int) -> String = { it.toString() }) {
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                val value = parser.getAttributeValue(null, "drawable")
                val drawable = value?.removePrefix("@drawable/")?.let {
                    if (it.startsWith("@")) it.drop(1).toIntOrNull()?.let(resourceName) else it
                }
                if (!drawable.isNullOrBlank()) {
                    when (parser.name) {
                        "item" -> parser.getAttributeValue(null, "component")?.let { component ->
                            normalize(component)?.let { activities[it] = drawable }
                        }
                        "icon" -> parser.getAttributeValue(null, "package")?.takeIf { it.isNotBlank() }?.let {
                            packages.putIfAbsent(it, drawable)
                        }
                    }
                }
            }
            parser.next()
        }
    }

    fun drawable(packageName: String, className: String): String? =
        activities[normalize("$packageName/$className")] ?: packages[packageName]

    private fun normalize(value: String): String? {
        val flat = value.removePrefix("ComponentInfo{").removeSuffix("}")
        val pkg = flat.substringBefore('/', "")
        val cls = flat.substringAfter('/', "")
        if (pkg.isBlank() || cls.isBlank()) return null
        return "$pkg/" + if (cls.startsWith('.')) pkg + cls else cls
    }
}
