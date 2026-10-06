package com.kleos.sakshi

import com.kleos.sakshi.host.HostApiImpl
import com.kleos.sakshi.host.LakeWidget
import com.kleos.sakshi.host.PostNotificationsRequester
import com.kleos.sakshi.host.Scheduler
import com.kleos.sakshi.host.gen.SakshiHostApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        Scheduler.ensurePeriodic(applicationContext)
    }

    /** A tap on the Lake widget (or, later, the weekly note) opens the Mirror directly. */
    override fun getInitialRoute(): String? =
        if (intent?.getStringExtra(LakeWidget.EXTRA_ROUTE) == LakeWidget.ROUTE_MIRROR) "/mirror" else super.getInitialRoute()

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        SakshiHostApi.setUp(flutterEngine.dartExecutor.binaryMessenger, HostApiImpl(applicationContext, PostNotificationsRequester { requestPostNotifications() }))
    }

    private var pendingNotificationRequest: kotlinx.coroutines.CompletableDeferred<Boolean>? = null

    /** The one runtime permission Sakshi asks for, and only when the user turns the weekly note on (Android 13+). */
    private suspend fun requestPostNotifications(): Boolean {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) return true
        val answer = kotlinx.coroutines.CompletableDeferred<Boolean>()
        pendingNotificationRequest = answer
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST_CODE)
        }
        return answer.await()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_REQUEST_CODE) {
            pendingNotificationRequest?.complete(grantResults.firstOrNull() == android.content.pm.PackageManager.PERMISSION_GRANTED)
            pendingNotificationRequest = null
        }
    }

    private companion object {
        const val NOTIFICATION_REQUEST_CODE = 4202
    }
}
