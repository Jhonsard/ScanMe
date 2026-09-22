package com.example

import com.example.network.PingSweepEngine
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PingSweepEngineTest {

    @Test
    fun testPingSweepOnLocalhost() = runBlocking {
        val engine = PingSweepEngine(maxConcurrency = 5, socketTimeoutMs = 100, icmpTimeoutMs = 100)
        val hosts = listOf("127.0.0.1")

        val result = engine.probeHost("127.0.0.1")
        assertNotNull(result)
        assertEquals("127.0.0.1", result.ip)
    }

    @Test
    fun testScanSubnetFlowEmission() = runBlocking {
        val engine = PingSweepEngine(maxConcurrency = 4, socketTimeoutMs = 50, icmpTimeoutMs = 50)
        val hosts = listOf("127.0.0.1", "127.0.0.2")

        val events = engine.scanSubnetFlow(hosts).toList()

        // Au minimum Started, les Progress et Completed
        assertTrue(events.isNotEmpty())
        assertTrue(events.first() is PingSweepEngine.ScanProgressEvent.Started)
        assertTrue(events.last() is PingSweepEngine.ScanProgressEvent.Completed)

        val completed = events.last() as PingSweepEngine.ScanProgressEvent.Completed
        assertEquals(2, completed.results.size)
    }
}
