package com.example.skbt_up_gibdd_eyewitness.data.message

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaMimeTypeTest {
    @Test
    fun `photo content type is restored from response header`() {
        assertEquals("image/jpeg", "Image/JPEG; charset=binary".normalizedMediaMimeType())
    }

    @Test
    fun `video content type is restored from response header`() {
        assertEquals("video/mp4", "video/mp4".normalizedMediaMimeType())
    }

    @Test
    fun `unsupported content type is ignored`() {
        assertNull("application/octet-stream".normalizedMediaMimeType())
    }
}
