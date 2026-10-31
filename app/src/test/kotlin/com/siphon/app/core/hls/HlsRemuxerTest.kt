package com.siphon.app.core.hls

import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class HlsRemuxerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testEmptyTrackFilesReturnsFalse() {
        val outputFile = tempFolder.newFile("output.mp4")
        val result = HlsRemuxer.remuxToMp4(emptyList(), outputFile)
        assertFalse(result)
    }

    @Test
    fun testNonExistentTrackFilesReturnsFalse() {
        val nonExistent = File(tempFolder.root, "does_not_exist.ts")
        val outputFile = tempFolder.newFile("output.mp4")
        val result = HlsRemuxer.remuxToMp4(listOf(nonExistent), outputFile)
        assertFalse(result)
    }

    @Test
    fun testZeroByteTrackFilesFilteredOut() {
        val emptyFile = tempFolder.newFile("empty.ts")
        val outputFile = tempFolder.newFile("output.mp4")
        val result = HlsRemuxer.remuxToMp4(listOf(emptyFile), outputFile)
        assertFalse(result)
    }
}
