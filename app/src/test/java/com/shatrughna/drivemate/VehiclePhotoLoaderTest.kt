package com.shatrughna.drivemate

import android.graphics.BitmapFactory
import com.shatrughna.drivemate.util.VehiclePhotoLoader
import org.junit.Assert.assertEquals
import org.junit.Test

class VehiclePhotoLoaderTest {

    @Test
    fun testInSampleSizeCalculationForLargeImage() {
        val options = BitmapFactory.Options().apply {
            outWidth = 4000
            outHeight = 3000
        }
        val sampleSize = VehiclePhotoLoader.calculateInSampleSize(options, 1080, 1080)
        // 4000 / 2 = 2000 > 1080 (sample 2); 2000 / 2 = 1000 <= 1080 (stop) -> sample 2 or 4
        assertEquals(2, sampleSize)
    }

    @Test
    fun testInSampleSizeCalculationForHugeImage() {
        val options = BitmapFactory.Options().apply {
            outWidth = 8000
            outHeight = 6000
        }
        val sampleSize = VehiclePhotoLoader.calculateInSampleSize(options, 1080, 1080)
        assertEquals(4, sampleSize)
    }

    @Test
    fun testInSampleSizeCalculationForSmallImage() {
        val options = BitmapFactory.Options().apply {
            outWidth = 800
            outHeight = 600
        }
        val sampleSize = VehiclePhotoLoader.calculateInSampleSize(options, 1080, 1080)
        assertEquals(1, sampleSize)
    }
}
