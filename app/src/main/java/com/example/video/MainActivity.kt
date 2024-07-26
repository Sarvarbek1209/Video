 package com.example.video

import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.video.databinding.ActivityMainBinding
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    // Инициализация переменных
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var isShowingImage: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Установка макета с помощью View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Установка полноэкранного режима
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        if (savedInstanceState !=null){
            isShowingImage = savedInstanceState.getBoolean("isShowingImage", false)
        }

        // Проверка разрешений
        if (allPermissionsGranted()) {
            // Если разрешения получены, запускаем камеру
            startCamera()
        } else {
            // Иначе запрашиваем необходимые разрешения
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        // Обработчик нажатия кнопки для захвата фотографии
        binding.buttonCapture.setOnClickListener {
            takePhoto()
        }

        // Инициализация отдельного потока для работы с камерой
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    // Функция для запуска камеры
    private fun startCamera() {
        // Получаем CameraProvider
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Добавляем слушатель для CameraProviderFuture
        cameraProviderFuture.addListener({
            // Получаем экземпляр CameraProvider
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Настройка предварительного просмотра камеры
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Настройка захвата изображения
            imageCapture = ImageCapture.Builder().build()

            // Используем камеру по умолчанию (задняя камера)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Отвязываем все предыдущие привязки камеры
                cameraProvider.unbindAll()

                // Привязываем жизненный цикл камеры к жизненному циклу активности
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )

            } catch (exc: Exception) {
                // Обработка ошибки при запуске камеры
                Toast.makeText(this, "Ошибка камеры", Toast.LENGTH_SHORT).show()
            }

        }, ContextCompat.getMainExecutor(this))
    }

    // Функция для захвата фотографии
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Создаем файл для сохранения фотографии
        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            "${SimpleDateFormat(FILENAME_FORMAT, Locale.US).format(System.currentTimeMillis())}.jpg"
        )

        // Настраиваем параметры для сохранения файла
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Захватываем фотографию
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    // Обработка ошибки сохранения фотографии
                    Toast.makeText(applicationContext, "Ошибка сохранения фото: ${exc.message}", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Уведомление о успешном сохранении фотографии
                    Toast.makeText(applicationContext, "Фото сохранено: ${photoFile.absolutePath}", Toast.LENGTH_SHORT).show()

                    // Отображение фотографии в ImageView
                    val photoUri = photoFile.toUri()
                    binding.photoView.setImageURI(photoUri)
                    binding.photoView.visibility=View.VISIBLE
                    binding.previewView.visibility = View.GONE
                    binding.buttonCapture.visibility = View.GONE



                }
            }
        )
    }

    // Проверка, что все необходимые разрешения получены
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    // Обработка результата запроса разрешений
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                // Если все разрешения получены, запускаем камеру
                startCamera()
            } else {
                // Иначе уведомляем пользователя и закрываем приложение
                Toast.makeText(this, "Разрешения не получены", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onBackPressed() {
        if (binding.photoView.visibility == View.VISIBLE) {
            binding.photoView.visibility = View.GONE
            binding.previewView.visibility = View.VISIBLE
            binding.buttonCapture.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
    }

    // Остановка работы потока камеры при разрушении активности
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}
