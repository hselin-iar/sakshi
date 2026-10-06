package com.kleos.sakshi

import com.kleos.sakshi.host.HostApiImpl
import com.kleos.sakshi.host.gen.SakshiHostApi
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        SakshiHostApi.setUp(flutterEngine.dartExecutor.binaryMessenger, HostApiImpl(applicationContext))
    }
}
