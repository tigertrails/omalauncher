package dev.omalauncher

import android.content.ComponentName
import android.content.Context
import android.content.res.Resources
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import android.util.Xml

/** A snapshot of the installed pack, loaded off the UI thread once per catalog refresh. */
internal class Lawnicons private constructor(
    private val resources: Resources,
    private val mappings: LawniconMappings,
) {
    fun icon(component: ComponentName, tint: Int): Drawable? =
        runCatching {
            val name = mappings.drawable(component.packageName, component.className) ?: return@runCatching null
            val foreground = resources.getIdentifier(name + "_foreground", "drawable", PACKAGE)
            val id = if (foreground != 0) foreground else resources.getIdentifier(name, "drawable", PACKAGE)
            if (id == 0) return@runCatching null
            val drawable = resources.getDrawable(id, null).mutate()
            // Lawnicons wraps its monochrome artwork in an adaptive icon with a solid backdrop.
            // Use just the artwork; tinting the whole adaptive icon would produce a solid square.
            val layer = if (drawable is AdaptiveIconDrawable) {
                (if (Build.VERSION.SDK_INT >= 33) drawable.monochrome else null) ?: drawable.foreground
            } else {
                drawable
            }
            layer.mutate().apply { setTint(tint) }
        }.onFailure { Log.w("lawnicons", "cannot load $component", it) }.getOrNull()

    companion object {
        const val PACKAGE = "app.lawnchair.lawnicons"

        fun load(context: Context): Lawnicons? =
            runCatching {
                val resources = context.packageManager.getResourcesForApplication(PACKAGE)
                val mappings = LawniconMappings()
                val appfilter = resources.getIdentifier("appfilter", "xml", PACKAGE)
                if (appfilter != 0) {
                    resources.getXml(appfilter).use { mappings.read(it, resources::getResourceEntryName) }
                } else {
                    resources.assets.open("appfilter.xml").use { input ->
                        val parser = Xml.newPullParser()
                        parser.setInput(input, null)
                        mappings.read(parser)
                    }
                }
                // Lawnicons also supplies explicit package mappings, including activity aliases.
                val grayscale = resources.getIdentifier("grayscale_icon_map", "xml", PACKAGE)
                if (grayscale != 0) runCatching {
                    resources.getXml(grayscale).use { mappings.read(it, resources::getResourceEntryName) }
                }
                Lawnicons(resources, mappings)
            }.onFailure { Log.i("lawnicons", "pack unavailable; using original icons", it) }.getOrNull()
    }
}
