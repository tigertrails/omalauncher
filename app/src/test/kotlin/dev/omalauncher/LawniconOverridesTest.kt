package dev.omalauncher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LawniconOverridesTest {
    @Test fun acceptsPackDrawableReferencesInsteadOfBakedBitmaps() {
        assertEquals("_1111_foreground", LawniconOverrides.resourceName(
            "app.lawnchair.lawnicons", "app.lawnchair.lawnicons:drawable/_1111_foreground",
        ))
        assertEquals("folder_foreground", LawniconOverrides.resourceName(
            "app.lawnchair.lawnicons", "app.lawnchair.lawnicons:drawable/folder_foreground",
        ))
    }

    @Test fun rejectsOtherPackagesResourceTypesAndInvalidNames() {
        assertNull(LawniconOverrides.resourceName("other.pack", "app.lawnchair.lawnicons:drawable/folder"))
        assertNull(LawniconOverrides.resourceName("app.lawnchair.lawnicons", "other.pack:drawable/folder"))
        assertNull(LawniconOverrides.resourceName("app.lawnchair.lawnicons", "app.lawnchair.lawnicons:color/folder"))
        assertNull(LawniconOverrides.resourceName("app.lawnchair.lawnicons", "app.lawnchair.lawnicons:drawable/../file"))
        assertNull(LawniconOverrides.resourceName("app.lawnchair.lawnicons", null))
    }

    @Test fun keepsActivityAndProfileAssignmentsSeparateAcrossRestarts() {
        val assignments = mapOf(
            "com.example/.Main" to "folder_foreground",
            "com.example/.Camera" to "camera_foreground",
            "com.example/.Main@10" to "work_foreground",
        )
        assertEquals(assignments, LawniconOverrides.read(LawniconOverrides.write(assignments)))
    }

    @Test fun resetRemovesOnlyThatAssignment() {
        val saved = LawniconOverrides.write(mapOf("a/.Main" to "folder", "b/.Main" to "camera"))
        val changed = LawniconOverrides.read(saved).toMutableMap().apply { remove("a/.Main") }
        assertEquals(mapOf("b/.Main" to "camera"), LawniconOverrides.read(LawniconOverrides.write(changed)))
    }

    @Test fun badEntriesDoNotDiscardOtherAssignments() {
        val saved = """{"v":1,"data":{"a/.Main":"folder","b/.Main":42,"c/.Main":"../bad"}}"""
        assertEquals(mapOf("a/.Main" to "folder"), LawniconOverrides.read(saved))
        assertTrue(LawniconOverrides.read(null).isEmpty())
        assertTrue(LawniconOverrides.read("broken json").isEmpty())
    }
}
