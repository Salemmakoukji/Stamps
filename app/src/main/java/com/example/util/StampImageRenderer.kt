package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.DailyStamp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object StampImageRenderer {

    fun generateStampBitmap(context: Context, stamp: DailyStamp, targetWidth: Int = 1080): Bitmap {
        val width = targetWidth
        val height = (targetWidth * 1.35f).toInt() // 4:5 portrait social media standard

        val outputBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(outputBitmap)

        // Clean warm off-white background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F7F5EE")
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Stamp Placement
        val stampMarginX = width * 0.10f
        val stampMarginTop = height * 0.08f
        val stampWidth = width - (stampMarginX * 2)
        val stampHeight = when (stamp.shapeType) {
            "SQUARE", "CIRCLE" -> stampWidth
            else -> stampWidth * 1.28f
        }
        val stampRect = RectF(stampMarginX, stampMarginTop, stampMarginX + stampWidth, stampMarginTop + stampHeight)

        val notchRadius = width * 0.022f
        val notchSpacing = notchRadius * 2.8f

        // Draw soft drop shadow behind stamp
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#18000000")
        }
        val shadowRect = RectF(stampRect.left, stampRect.top + 8f, stampRect.right, stampRect.bottom + 8f)
        val shadowPath = createPerforatedPath(shadowRect, notchRadius, notchSpacing, stamp.shapeType)
        canvas.drawPath(shadowPath, shadowPaint)

        // Base white border path
        val stampPath = createPerforatedPath(stampRect, notchRadius, notchSpacing, stamp.shapeType)
        val whiteBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawPath(stampPath, whiteBorderPaint)

        // Photo inside stamp
        val loadedBitmap = loadBitmapFromUri(context, stamp.imageUri)
        if (loadedBitmap != null) {
            canvas.save()
            canvas.clipPath(stampPath)
            drawCenterCrop(canvas, loadedBitmap, stampRect)
            canvas.restore()
        }

        // Crisp white border rim
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = width * 0.012f
        }
        canvas.drawPath(stampPath, rimPaint)

        // Bottom area: Caption & Date/Time
        val contentTop = stampRect.bottom + height * 0.04f
        val captionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#262320")
            textSize = width * 0.038f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val captionText = if (stamp.caption.isNotBlank()) stamp.caption else "A beautiful moment"
        val captionWidth = (width - stampMarginX * 2).toInt()

        val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StaticLayout.Builder.obtain(captionText, 0, captionText.length, captionPaint, captionWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .setMaxLines(3)
                .build()
        } else {
            @Suppress("DEPRECATION")
            StaticLayout(captionText, captionPaint, captionWidth, Layout.Alignment.ALIGN_NORMAL, 1.2f, 0f, false)
        }

        canvas.save()
        canvas.translate(stampMarginX, contentTop)
        staticLayout.draw(canvas)
        canvas.restore()

        // Date and Location row
        val metaY = contentTop + staticLayout.height + height * 0.03f
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#78716C")
            textSize = width * 0.028f
            typeface = Typeface.DEFAULT_BOLD
        }
        val dateText = stamp.dateFormatted + if (stamp.locationOrTag.isNotBlank()) " • ${stamp.locationOrTag}" else ""
        canvas.drawText(dateText, stampMarginX, metaY, metaPaint)

        return outputBitmap
    }

    private fun createPerforatedPath(rect: RectF, notchRadius: Float, notchSpacing: Float, shapeType: String): Path {
        if (shapeType == "CIRCLE") {
            val center = RectF(rect.centerX() - rect.width() / 2f, rect.centerY() - rect.width() / 2f, rect.centerX() + rect.width() / 2f, rect.centerY() + rect.width() / 2f)
            val basePath = Path().apply { addOval(center, Path.Direction.CW) }
            val notches = Path()
            val radius = rect.width() / 2f
            val count = (2 * PI * radius / notchSpacing).toInt().coerceAtLeast(12)
            val step = (2 * PI) / count
            for (i in 0 until count) {
                val angle = i * step
                val nx = rect.centerX() + radius * cos(angle).toFloat()
                val ny = rect.centerY() + radius * sin(angle).toFloat()
                notches.addCircle(nx, ny, notchRadius, Path.Direction.CW)
            }
            val finalPath = Path()
            finalPath.op(basePath, notches, Path.Op.DIFFERENCE)
            return finalPath
        }

        val basePath = Path().apply {
            if (shapeType == "CONTOUR") {
                addRoundRect(rect, 40f, 40f, Path.Direction.CW)
            } else {
                addRect(rect, Path.Direction.CW)
            }
        }
        val notches = Path()

        // Top notches
        var x = rect.left + notchSpacing
        while (x < rect.right - notchSpacing / 2) {
            notches.addCircle(x, rect.top, notchRadius, Path.Direction.CW)
            x += notchSpacing
        }

        // Bottom notches
        x = rect.left + notchSpacing
        while (x < rect.right - notchSpacing / 2) {
            notches.addCircle(x, rect.bottom, notchRadius, Path.Direction.CW)
            x += notchSpacing
        }

        // Left notches
        var y = rect.top + notchSpacing
        while (y < rect.bottom - notchSpacing / 2) {
            notches.addCircle(rect.left, y, notchRadius, Path.Direction.CW)
            y += notchSpacing
        }

        // Right notches
        y = rect.top + notchSpacing
        while (y < rect.bottom - notchSpacing / 2) {
            notches.addCircle(rect.right, y, notchRadius, Path.Direction.CW)
            y += notchSpacing
        }

        val finalPath = Path()
        finalPath.op(basePath, notches, Path.Op.DIFFERENCE)
        return finalPath
    }

    private fun drawCenterCrop(canvas: Canvas, source: Bitmap, dest: RectF) {
        val srcRatio = source.width.toFloat() / source.height.toFloat()
        val destRatio = dest.width() / dest.height()

        val srcRect = Rect()
        if (srcRatio > destRatio) {
            val targetSrcWidth = (source.height * destRatio).toInt()
            val left = (source.width - targetSrcWidth) / 2
            srcRect.set(left, 0, left + targetSrcWidth, source.height)
        } else {
            val targetSrcHeight = (source.width / destRatio).toInt()
            val top = (source.height - targetSrcHeight) / 2
            srcRect.set(0, top, source.width, top + targetSrcHeight)
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, srcRect, dest, paint)
    }

    private fun loadBitmapFromUri(context: Context, uriString: String): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            if (uri.scheme == "file") {
                BitmapFactory.decodeFile(uri.path)
            } else {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun shareStamp(context: Context, stamp: DailyStamp) {
        try {
            val bitmap = generateStampBitmap(context, stamp)
            val cacheFolder = File(context.cacheDir, "stamps")
            if (!cacheFolder.exists()) cacheFolder.mkdirs()

            val fileName = "stamp_${stamp.id}_${System.currentTimeMillis()}.png"
            val imageFile = File(cacheFolder, fileName)
            FileOutputStream(imageFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                val shareText = if (stamp.caption.isNotBlank()) "${stamp.caption}\n${stamp.dateFormatted}" else stamp.dateFormatted
                putExtra(Intent.EXTRA_TEXT, shareText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Stamp")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveStampToGallery(context: Context, stamp: DailyStamp): Boolean {
        return try {
            val bitmap = generateStampBitmap(context, stamp)
            val filename = "Stamp_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(Date())}.png"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Stamps")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear()
                    values.put(MediaStore.Images.Media.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
