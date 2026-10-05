package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ImageFileHelper {

    fun createCameraTempUri(context: Context): Uri {
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val tempFile = File.createTempFile("camera_capture_", ".jpg", photosDir)
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }

    fun persistImageToAppStorage(context: Context, sourceUri: Uri): String {
        return try {
            val photosDir = File(context.filesDir, "photos")
            if (!photosDir.exists()) photosDir.mkdirs()

            val targetFile = File(photosDir, "stamp_photo_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(targetFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            sourceUri.toString()
        }
    }

    // Creates an artistic starter sample photo if needed for demonstration
    fun createSamplePhoto(context: Context, color: Int, title: String): String {
        val photosDir = File(context.filesDir, "photos")
        if (!photosDir.exists()) photosDir.mkdirs()
        val file = File(photosDir, "starter_${System.currentTimeMillis()}.jpg")

        val bitmap = Bitmap.createBitmap(800, 800, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(color)

        // Draw soft circles / sun
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.parseColor("#44FFFFFF")
        }
        canvas.drawCircle(400f, 400f, 250f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = Color.WHITE
            textSize = 50f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(title, 400f, 420f, textPaint)

        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        return Uri.fromFile(file).toString()
    }
}
