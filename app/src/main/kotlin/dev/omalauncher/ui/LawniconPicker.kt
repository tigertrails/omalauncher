package dev.omalauncher.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import dev.omalauncher.LawniconOverrides
import dev.omalauncher.Lawnicons
import dev.omalauncher.Runtime
import dev.omalauncher.theme.Style

/** Reuse Lawnicons' search and previews; ignore its baked bitmap so our theme keeps driving the tint. */
@Suppress("DEPRECATION")
@Composable
internal fun LawniconPicker(runtime: Runtime) {
    val context = LocalContext.current
    val catalog = runtime.androidApps
    // Survives activity/process recreation while the external picker is in front.
    var pendingKey by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val key = pendingKey
        pendingKey = null
        if (key != null && result.resultCode == Activity.RESULT_OK) {
            val resource = if (Build.VERSION.SDK_INT >= 33) {
                result.data?.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource::class.java)
            } else {
                result.data?.getParcelableExtra<Intent.ShortcutIconResource>(Intent.EXTRA_SHORTCUT_ICON_RESOURCE)
            }
            val name = LawniconOverrides.resourceName(resource?.packageName, resource?.resourceName)
            if (name != null) {
                catalog.setLawnicon(key, name)
                runtime.style.update { it.with(Style.LAWNICONS, true) }
            } else {
                Toast.makeText(context, "Lawnicons did not return a usable icon", Toast.LENGTH_LONG).show()
            }
        }
    }
    val target = catalog.lawniconPickerTarget
    LaunchedEffect(target) {
        if (target != null) {
            catalog.lawniconPickerTarget = null
            pendingKey = target
            try {
                val component = context.packageManager.getLaunchIntentForPackage(Lawnicons.PACKAGE)?.component
                    ?: throw ActivityNotFoundException()
                // This is the action Lawnicons recognizes as picker mode. An explicit component also
                // supports its Nova filter, which does not declare CATEGORY_DEFAULT.
                picker.launch(Intent("com.novalauncher.THEME").setComponent(component))
            } catch (_: ActivityNotFoundException) {
                pendingKey = null
                Toast.makeText(context, "Install Lawnicons in this Android profile first", Toast.LENGTH_LONG).show()
            } catch (_: SecurityException) {
                pendingKey = null
                Toast.makeText(context, "Android could not open the Lawnicons picker", Toast.LENGTH_LONG).show()
            }
        }
    }
}
