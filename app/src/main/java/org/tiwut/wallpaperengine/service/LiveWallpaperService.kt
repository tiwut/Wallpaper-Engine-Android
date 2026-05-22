package org.tiwut.wallpaperengine.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.SurfaceHolder
import android.view.animation.AccelerateDecelerateInterpolator
import android.animation.ValueAnimator
import org.tiwut.wallpaperengine.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

class LiveWallpaperService : android.service.wallpaper.WallpaperService() {
    override fun onCreateEngine(): Engine = WallpaperEngine()

    inner class WallpaperEngine : Engine(), SensorEventListener {
        private val engineScope = CoroutineScope(Dispatchers.Main + Job())
        private lateinit var database: AppDatabase
        
        private var sensorManager: SensorManager? = null
        private var rotationSensor: Sensor? = null
        private val rotationMatrix = FloatArray(9)
        private val orientationAngles = FloatArray(3)
        private var currentAzimuth = 0f
        private var currentPitch = 0f
        
        private var currentBitmap: Bitmap? = null
        private var previousBitmap: Bitmap? = null
        
        private var transitionProgress = 1f
        private var currentAnimation = "FADE"
        private var animator: ValueAnimator? = null
        private var isVisibleState = false
        
        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            database = AppDatabase.getDatabase(this@LiveWallpaperService)
            
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            
            engineScope.launch {
                database.wallpaperDao().getActiveWallpaper().collect { active ->
                    active?.let { updateWallpaper(it.localUri, it.transitionAnimation) }
                }
            }
        }
        
        private fun updateWallpaper(uri: String, animation: String) {
            val file = File(uri)
            if (!file.exists()) return
            
            val newBitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            
            if (currentBitmap == null) {
                currentBitmap = newBitmap
                currentAnimation = animation
                checkSensorRegistration()
                drawFrame()
                return
            }
            
            if (previousBitmap != null) {
                animator?.cancel()
            }
            previousBitmap = currentBitmap
            currentBitmap = newBitmap
            currentAnimation = animation
            transitionProgress = 0f
            
            checkSensorRegistration()
            
            animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 1200L
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener { anim ->
                    transitionProgress = anim.animatedValue as Float
                    drawFrame()
                }
                start()
            }
        }

        private fun checkSensorRegistration() {
            if (isVisibleState && currentAnimation == "PANORAMA_360") {
                sensorManager?.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
            } else {
                sensorManager?.unregisterListener(this)
            }
        }

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                
                val targetAzimuth = orientationAngles[0]
                val targetPitch = orientationAngles[1]
                
                var deltaAzi = targetAzimuth - currentAzimuth
                while (deltaAzi > Math.PI) deltaAzi -= (2 * Math.PI).toFloat()
                while (deltaAzi < -Math.PI) deltaAzi += (2 * Math.PI).toFloat()
                currentAzimuth += deltaAzi * 0.03f
                
                var deltaPitch = targetPitch - currentPitch
                while (deltaPitch > Math.PI) deltaPitch -= (2 * Math.PI).toFloat()
                while (deltaPitch < -Math.PI) deltaPitch += (2 * Math.PI).toFloat()
                currentPitch += deltaPitch * 0.03f
                
                if (currentAnimation == "PANORAMA_360" && transitionProgress >= 1f) {
                    drawFrame()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try { holder.lockHardwareCanvas() } catch (e: Exception) { holder.lockCanvas() }
                } else {
                    holder.lockCanvas()
                }
                
                if (canvas != null) {
                    val width = canvas.width.toFloat()
                    val height = canvas.height.toFloat()
                    
                    if (previousBitmap != null && transitionProgress < 1f) {
                        drawWithTransition(canvas, width, height)
                    } else {
                        currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = 255) }
                    }
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

        private fun drawWithTransition(canvas: Canvas, width: Float, height: Float) {
            val progress = transitionProgress
            val invProgress = 1f - progress
            
            canvas.drawColor(Color.BLACK)
            
            when (currentAnimation) {
                "FADE", "PANORAMA_360" -> {
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (invProgress * 255).toInt()) }
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (progress * 255).toInt()) }
                }
                "SLIDE_LEFT" -> {
                    val offsetX1 = -progress * width
                    val offsetX2 = width - progress * width
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, offsetX1, 0f) }
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, offsetX2, 0f) }
                }
                "SLIDE_RIGHT" -> {
                    val offsetX1 = progress * width
                    val offsetX2 = -width + progress * width
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, offsetX1, 0f) }
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, offsetX2, 0f) }
                }
                "SLIDE_UP" -> {
                    val offsetY1 = -progress * height
                    val offsetY2 = height - progress * height
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, 0f, offsetY1) }
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, 0f, offsetY2) }
                }
                "SLIDE_DOWN" -> {
                    val offsetY1 = progress * height
                    val offsetY2 = -height + progress * height
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, 0f, offsetY1) }
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, 0f, offsetY2) }
                }
                "ZOOM" -> {
                    canvas.save()
                    val scale1 = 1f + progress * 0.3f
                    canvas.scale(scale1, scale1, width / 2, height / 2)
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (invProgress * 255).toInt()) }
                    canvas.restore()
                    
                    canvas.save()
                    val scale2 = 0.7f + progress * 0.3f
                    canvas.scale(scale2, scale2, width / 2, height / 2)
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (progress * 255).toInt()) }
                    canvas.restore()
                }
                "CROSSFADE_ZOOM" -> {
                    canvas.save()
                    val scale1 = 1f + progress * 0.2f
                    canvas.scale(scale1, scale1, width / 2, height / 2)
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (invProgress * 255).toInt()) }
                    canvas.restore()
                    
                    canvas.save()
                    val scale2 = 1.2f - progress * 0.2f
                    canvas.scale(scale2, scale2, width / 2, height / 2)
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (progress * 255).toInt()) }
                    canvas.restore()
                }
                "WIPE" -> {
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                    canvas.save()
                    canvas.clipRect(0f, 0f, width * progress, height)
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                    canvas.restore()
                }
                "DIAGONAL_WIPE" -> {
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                    val path = android.graphics.Path()
                    path.moveTo(0f, 0f)
                    path.lineTo(width * progress * 2, 0f)
                    path.lineTo(0f, height * progress * 2)
                    path.close()
                    canvas.save()
                    canvas.clipPath(path)
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                    canvas.restore()
                }
                "CIRCULAR_REVEAL" -> {
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                    val maxRadius = kotlin.math.hypot(width.toDouble(), height.toDouble()).toFloat()
                    val path = android.graphics.Path()
                    path.addCircle(width/2, height/2, maxRadius * progress, android.graphics.Path.Direction.CW)
                    canvas.save()
                    canvas.clipPath(path)
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                    canvas.restore()
                }
                "FLIP_X" -> {
                    val camera = android.graphics.Camera()
                    val matrix = android.graphics.Matrix()
                    
                    if (progress < 0.5f) {
                        camera.save()
                        camera.rotateY(progress * 180f)
                        camera.getMatrix(matrix)
                        camera.restore()
                        matrix.preTranslate(-width / 2, -height / 2)
                        matrix.postTranslate(width / 2, height / 2)
                        canvas.save()
                        canvas.concat(matrix)
                        previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                        canvas.restore()
                    } else {
                        camera.save()
                        camera.rotateY(progress * 180f - 180f)
                        camera.getMatrix(matrix)
                        camera.restore()
                        matrix.preTranslate(-width / 2, -height / 2)
                        matrix.postTranslate(width / 2, height / 2)
                        canvas.save()
                        canvas.concat(matrix)
                        currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                        canvas.restore()
                    }
                }
                "FLIP_Y" -> {
                    val camera = android.graphics.Camera()
                    val matrix = android.graphics.Matrix()
                    
                    if (progress < 0.5f) {
                        camera.save()
                        camera.rotateX(progress * 180f)
                        camera.getMatrix(matrix)
                        camera.restore()
                        matrix.preTranslate(-width / 2, -height / 2)
                        matrix.postTranslate(width / 2, height / 2)
                        canvas.save()
                        canvas.concat(matrix)
                        previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                        canvas.restore()
                    } else {
                        camera.save()
                        camera.rotateX(progress * 180f - 180f)
                        camera.getMatrix(matrix)
                        camera.restore()
                        matrix.preTranslate(-width / 2, -height / 2)
                        matrix.postTranslate(width / 2, height / 2)
                        canvas.save()
                        canvas.concat(matrix)
                        currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height) }
                        canvas.restore()
                    }
                }
                "DRIFT" -> {
                    val driftX1 = -progress * 100f
                    val driftX2 = (1f - progress) * 100f
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, driftX1, 0f, (invProgress * 255).toInt()) }
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, driftX2, 0f, (progress * 255).toInt()) }
                }
                "SPIN" -> {
                    canvas.save()
                    canvas.rotate(progress * 360f, width/2, height/2)
                    canvas.scale(progress, progress, width/2, height/2)
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (invProgress * 255).toInt()) }
                    canvas.restore()
                    
                    canvas.save()
                    canvas.rotate((progress - 1f) * 360f, width/2, height/2)
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (progress * 255).toInt()) }
                    canvas.restore()
                }
                "SHRINK_GROW" -> {
                    canvas.save()
                    val scale1 = 1f - progress
                    canvas.scale(scale1, scale1, width/2, height/2)
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (invProgress * 255).toInt()) }
                    canvas.restore()
                    
                    canvas.save()
                    val scale2 = progress
                    canvas.scale(scale2, scale2, width/2, height/2)
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (progress * 255).toInt()) }
                    canvas.restore()
                }
                else -> {
                    previousBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (invProgress * 255).toInt()) }
                    currentBitmap?.let { drawBitmapCenterCrop(canvas, it, width, height, alpha = (progress * 255).toInt()) }
                }
            }
        }
        
        private fun drawBitmapCenterCrop(
            canvas: Canvas, 
            bitmap: Bitmap, 
            viewWidth: Float, 
            viewHeight: Float, 
            offsetX: Float = 0f, 
            offsetY: Float = 0f,
            alpha: Int = 255
        ) {
            val scale: Float
            var dx = 0f
            var dy = 0f

            val isPanoramaMode = currentAnimation == "PANORAMA_360"

            if (isPanoramaMode) {
                val baseScale = Math.max(viewWidth / bitmap.width.toFloat(), viewHeight / bitmap.height.toFloat())
                scale = baseScale * 1.5f
                
                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale
                
                val maxScrollX = scaledWidth - viewWidth
                val maxScrollY = scaledHeight - viewHeight
                
                if (maxScrollX > 0) {
                    val normalizedAzimuth = (currentAzimuth + Math.PI) / (2 * Math.PI)
                    dx = - (normalizedAzimuth.toFloat() * maxScrollX)
                } else {
                    dx = (viewWidth - scaledWidth) * 0.5f 
                }
                
                if (maxScrollY > 0) {
                    val normalizedPitch = (currentPitch + Math.PI / 2) / Math.PI
                    val clampedPitch = Math.max(0.0, Math.min(1.0, normalizedPitch))
                    dy = - (clampedPitch.toFloat() * maxScrollY)
                } else {
                    dy = (viewHeight - scaledHeight) * 0.5f
                }
            } else {
                if (bitmap.width * viewHeight > viewWidth * bitmap.height) {
                    scale = viewHeight / bitmap.height.toFloat()
                    dx = (viewWidth - bitmap.width * scale) * 0.5f
                } else {
                    scale = viewWidth / bitmap.width.toFloat()
                    dy = (viewHeight - bitmap.height * scale) * 0.5f
                }
            }

            val matrix = Matrix()
            matrix.setScale(scale, scale)
            matrix.postTranslate((dx + offsetX).roundToInt().toFloat(), (dy + offsetY).roundToInt().toFloat())
            
            val paint = Paint()
            paint.isFilterBitmap = true
            paint.alpha = alpha
            
            canvas.drawBitmap(bitmap, matrix, paint)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            isVisibleState = visible
            checkSensorRegistration()
            if (visible) drawFrame()
        }
        
        override fun onDestroy() {
            super.onDestroy()
            sensorManager?.unregisterListener(this)
            engineScope.cancel()
            animator?.cancel()
            currentBitmap?.recycle()
            previousBitmap?.recycle()
        }
    }
}
