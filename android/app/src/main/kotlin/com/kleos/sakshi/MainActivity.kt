package com.kleos.sakshi

import com.kleos.sakshi.host.HostApiImpl
import com.kleos.sakshi.host.LakeWidget
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
        SakshiHostApi.setUp(flutterEngine.dartExecutor.binaryMessenger, HostApiImpl(applicationContext))
    }
}
