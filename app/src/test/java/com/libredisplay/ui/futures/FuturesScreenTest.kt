package com.libredisplay.ui.futures

import android.app.Application
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w360dp-h800dp-port-notnight")
class FuturesScreenTest : FuturesContentContract()

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, qualifiers = "w800dp-h360dp-land-night")
class FuturesLandscapeScreenTest : FuturesContentContract()

