package com.libredisplay.data.demo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoDataPolicyTest {

    @Test
    fun isDemoSource_acceptsCurrentAndLegacyMarkers() {
        assertTrue(DemoDataPolicy.isDemoSource("DEMO"))
        assertTrue(DemoDataPolicy.isDemoSource("DemoMode"))
        assertTrue(DemoDataPolicy.isDemoSource("demomode"))
        assertFalse(DemoDataPolicy.isDemoSource("LibreLinkUp"))
    }

    @Test
    fun isDemoPatientId_matchesMikaPrefix() {
        assertTrue(DemoDataPolicy.isDemoPatientId("demo-person-mika"))
        assertFalse(DemoDataPolicy.isDemoPatientId("real-person-1"))
    }
}

