// port-lint: tests patch/parse.rs
package io.github.kotlinmania.diffy.patch

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParseTests {
    @Test
    fun testEscapedFilenames() {
        // No escaped characters
        var s = "--- original\n+++ modified\n@@ -1,0 +1,1 @@\n+Oathbringer\n"
        assertTrue(parse(s).isSuccess)
        assertTrue(parseBytes(s.encodeToByteArray()).isSuccess)

        // unescaped characters fail parsing
        s = "--- ori\"ginal\n+++ modified\n@@ -1,0 +1,1 @@\n+Oathbringer\n"
        assertTrue(parse(s).isFailure)
        assertTrue(parseBytes(s.encodeToByteArray()).isFailure)

        // quoted with invalid escaped characters
        s = "--- \"ori\\\"g\rinal\"\n+++ modified\n@@ -1,0 +1,1 @@\n+Oathbringer\n"
        assertTrue(parse(s).isFailure)
        assertTrue(parseBytes(s.encodeToByteArray()).isFailure)

        // quoted with escaped characters
        s = "--- \"ori\\\"g\\tinal\"\n+++ \"mo\\0\\t\\r\\n\\\\dified\"\n@@ -1,0 +1,1 @@\n+Oathbringer\n"
        val p = parse(s).getOrThrow()
        assertEquals("ori\"g\tinal", p.original())
        assertEquals("mo\u0000\t\r\n\\dified", p.modified())
        val b = parseBytes(s.encodeToByteArray()).getOrThrow()
        assertContentEquals("ori\"g\tinal".encodeToByteArray(), b.original())
        assertContentEquals("mo\u0000\t\r\n\\dified".encodeToByteArray(), b.modified())
    }

    @Test
    fun testMissingFilenameHeader() {
        // Missing Both '---' and '+++' lines
        val patch =
            """
@@ -1,11 +1,12 @@
 diesel::table! {
     users1 (id) {
-        id -> Nullable<Integer>,
+        id -> Integer,
     }
 }

 diesel::table! {
-    users2 (id) {
-        id -> Nullable<Integer>,
+    users2 (myid) {
+        #[sql_name = "id"]
+        myid -> Integer,
     }
 }
            """.trimIndent()

        assertTrue(parse(patch).isSuccess)

        // Missing '---'
        var s = "+++ modified\n@@ -1,0 +1,1 @@\n+Oathbringer\n"
        assertTrue(parse(s).isSuccess)

        // Missing '+++'
        s = "--- original\n@@ -1,0 +1,1 @@\n+Oathbringer\n"
        assertTrue(parse(s).isSuccess)

        // Headers out of order
        s = "+++ modified\n--- original\n@@ -1,0 +1,1 @@\n+Oathbringer\n"
        assertTrue(parse(s).isSuccess)

        // multiple headers should fail to parse
        s = "--- original\n--- modified\n@@ -1,0 +1,1 @@\n+Oathbringer\n"
        assertTrue(parse(s).isFailure)
    }

    @Test
    fun adjacentHunksCorrectlyParse() {
        val s =
            "--- original\n" +
                "+++ modified\n" +
                "@@ -110,7 +110,7 @@\n" +
                " --\n" +
                "\n" +
                " I am afraid, however, that all I have known - that my story - will be forgotten.\n" +
                " I am afraid for the world that is to come.\n" +
                "-Afraid that my plans will fail. Afraid of a doom worse than the Deepness.\n" +
                "+Afraid that Alendi will fail. Afraid of a doom brought by the Deepness.\n" +
                "\n" +
                " Alendi was never the Hero of Ages.\n" +
                "@@ -117,7 +117,7 @@\n" +
                " At best, I have amplified his virtues, creating a Hero where there was none.\n" +
                "\n" +
                "-At worst, I fear that all we believe may have been corrupted.\n" +
                "+At worst, I fear that I have corrupted all we believe.\n" +
                "\n" +
                " --\n" +
                " Alendi must not reach the Well of Ascension. He must not take the power for himself.\n" +
                "\n"

        assertTrue(parse(s).isSuccess)
        assertTrue(parseBytes(s.encodeToByteArray()).isSuccess)
    }
}
