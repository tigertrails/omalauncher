package dev.omalauncher

import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.kxml2.io.KXmlParser

class LawniconMappingsTest {
    private fun mappings(vararg xml: String) = LawniconMappings().apply {
        for (text in xml) read(KXmlParser().apply { setInput(StringReader(text)) })
    }

    @Test fun matchesFullAndShortActivityNames() {
        val pack = mappings("""<resources>
            <item component="ComponentInfo{com.example/com.example.Main}" drawable="example"/>
            <item component="ComponentInfo{com.other/.Main}" drawable="other"/>
        </resources>""")
        assertEquals("example", pack.drawable("com.example", ".Main"))
        assertEquals("other", pack.drawable("com.other", "com.other.Main"))
    }

    @Test fun preservesSeparateActivitiesInOnePackage() {
        val pack = mappings("""<resources>
            <item component="ComponentInfo{com.example/.Main}" drawable="main"/>
            <item component="ComponentInfo{com.example/.Camera}" drawable="camera"/>
        </resources>""")
        assertEquals("camera", pack.drawable("com.example", ".Camera"))
        assertEquals("main", pack.drawable("com.example", ".Main"))
        assertNull(pack.drawable("com.example", ".Unmapped"))
    }

    @Test fun exactActivityWinsOverExplicitPackageFallback() {
        val pack = mappings("""<resources>
            <item component="ComponentInfo{com.example/.Main}" drawable="main"/>
        </resources>""", """<icons>
            <icon package="com.example" drawable="@drawable/package_foreground"/>
        </icons>""")
        assertEquals("main", pack.drawable("com.example", ".Main"))
        assertEquals("package_foreground", pack.drawable("com.example", ".Renamed"))
        assertNull(pack.drawable("com.uncovered", ".Main"))
    }

    @Test fun ignoresMalformedMappingsAndPackDecorations() {
        val pack = mappings("""<resources>
            <item component="ComponentInfo{broken}" drawable="bad"/>
            <item component="ComponentInfo{com.example/.Main}"/>
            <item component="ComponentInfo{com.example/.Empty}" drawable=""/>
            <iconback img1="background"/>
            <scale factor="0.8"/>
        </resources>""")
        assertNull(pack.drawable("com.example", ".Main"))
        assertNull(pack.drawable("com.example", ".Empty"))
    }

    @Test fun resolvesCompiledDrawableResourceReferences() {
        val pack = LawniconMappings()
        val parser = KXmlParser().apply {
            setInput(StringReader("""<icons><icon package="com.example" drawable="@2131230721"/></icons>"""))
        }
        pack.read(parser) { id ->
            assertEquals(2131230721, id)
            "example_foreground"
        }
        assertEquals("example_foreground", pack.drawable("com.example", ".Main"))
    }

    @Test fun staticCalendarMappingRemainsAvailable() {
        val pack = mappings("""<resources>
            <calendar component="ComponentInfo{com.calendar/.Main}" prefix="calendar_"/>
            <item component="ComponentInfo{com.calendar/.Main}" drawable="calendar"/>
        </resources>""")
        assertEquals("calendar", pack.drawable("com.calendar", ".Main"))
    }
}
