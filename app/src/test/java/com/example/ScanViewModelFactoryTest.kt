package com.example

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ScanViewModel
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScanViewModelFactoryTest {

    @Test
    fun testAndroidViewModelFactoryCanCreateScanViewModel() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val viewModel = factory.create(ScanViewModel::class.java)
        assertNotNull(viewModel)
        assertNotNull(viewModel.billingRepository)
        assertNotNull(viewModel.trialManager)
    }
}
