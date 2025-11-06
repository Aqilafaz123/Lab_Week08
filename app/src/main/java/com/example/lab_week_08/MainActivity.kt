package com.example.lab_week_08

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.work.*
import com.example.lab_week_08.worker.FirstWorker
import com.example.lab_week_08.worker.SecondWorker
import com.example.lab_week_08.worker.ThirdWorker

class MainActivity : AppCompatActivity() {

    private val workManager by lazy { WorkManager.getInstance(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val sysBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(sysBars.left, sysBars.top, sysBars.right, sysBars.bottom)
            insets
        }

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }

        // Constraint: must have internet
        val networkConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val id = "001"

        val firstRequest = OneTimeWorkRequestBuilder<FirstWorker>()
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(FirstWorker.INPUT_DATA_ID, id))
            .build()

        val secondRequest = OneTimeWorkRequestBuilder<SecondWorker>()
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(SecondWorker.INPUT_DATA_ID, id))
            .build()

        val thirdRequest = OneTimeWorkRequestBuilder<ThirdWorker>()
            .setConstraints(networkConstraints)
            .setInputData(getIdInputData(ThirdWorker.INPUT_DATA_ID, id))
            .build()

        // Jalankan hanya First -> Second dulu
        workManager.beginWith(firstRequest)
            .then(secondRequest)
            .enqueue()

        // --- Observers ---
        workManager.getWorkInfoByIdLiveData(firstRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) showToast("First process is done")
        }

        workManager.getWorkInfoByIdLiveData(secondRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                showToast("Second process is done")
                // Jalankan NotificationService (channel 001)
                launchNotificationService(thirdRequest)
            }
        }

        // Observe ThirdWorker -> panggil SecondNotificationService
        workManager.getWorkInfoByIdLiveData(thirdRequest.id).observe(this) { info ->
            if (info != null && info.state.isFinished) {
                showToast("Third process is done")
                launchSecondNotificationService()
            }
        }
    }

    private fun getIdInputData(key: String, value: String) =
        Data.Builder().putString(key, value).build()

    private fun showToast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    /**
     * Jalankan NotificationService dan tunggu sampai selesai.
     * Baru setelah selesai, jalankan ThirdWorker.
     */
    private fun launchNotificationService(thirdRequest: OneTimeWorkRequest) {
        NotificationService.trackingCompletion.observe(this) { id ->
            showToast("Process for Notification Channel ID $id is done!")
            // Setelah notification countdown selesai → lanjut ke ThirdWorker
            workManager.enqueue(thirdRequest)
        }

        val serviceIntent = Intent(this, NotificationService::class.java).apply {
            putExtra(NotificationService.EXTRA_ID, "001")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    /**
     * Jalankan SecondNotificationService setelah ThirdWorker selesai.
     */
    private fun launchSecondNotificationService() {
        SecondNotificationService.trackingCompletion.observe(this) { id ->
            showToast("Process for Notification Channel ID $id is done!")
        }

        val serviceIntent = Intent(this, SecondNotificationService::class.java).apply {
            putExtra(SecondNotificationService.EXTRA_ID, "002")
        }
        ContextCompat.startForegroundService(this, serviceIntent)
    }

    companion object {
        const val EXTRA_ID = "Id"
    }
}
