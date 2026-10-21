package com.libredisplay

import android.os.Bundle
import androidx.activity.enableEdgeToEdge

/** Debug-only host for production Compose screens; mirrors MainActivity configuration handling. */
class DeliveryTestActivity : androidx.activity.ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
	}
}

