package com.sorbonne.atom_d

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.sorbonne.atom_d.ui.dashboard.DashboardFragment
import com.sorbonne.d2d.D2D
import com.sorbonne.d2d.D2DListener
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity(), D2DListener {

    private val TAG = MainActivity::class.simpleName

    private lateinit var viewModel: MainViewModel

    private lateinit var navHostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration

    var d2d: D2D ?= null
    var androidId: String ?= null

    private val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val isGranted = !result.containsValue(false)
        if (isGranted) {
            Log.i(TAG, "permissions granted")
        }
        else {
            result.filter { !it.value }.keys.forEach { permission ->
                Log.w(TAG, "permission $permission missing")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, MainViewModel.Factory(this))[MainViewModel::class.java]

        @SuppressLint("HardwareIds")
        androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        viewModel.deviceId = androidId

        androidId?.let {deviceId ->
            d2d = D2D.Builder(
                this,
                 deviceId,
                this
            ).setListener(this)
                .build()
        }

        viewModel.instance = d2d

        setContentView(R.layout.activity_main)
        findViewById<View>(R.id.main_layout)

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment

        val navController = navHostFragment.navController
        findViewById<BottomNavigationView>(R.id.bottom_nav).setupWithNavController(navController)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.dashboardFragment, R.id.experimentFragment, R.id. aboutUsFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onConnectivityChange(active: Boolean) {
        super.onConnectivityChange(active)
        navHostFragment.childFragmentManager.fragments.forEach{
            try {
                (it as? DashboardFragment)?.onConnectivityChange(active)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onDiscoveryChange(active: Boolean) {
        super.onDiscoveryChange(active)
        navHostFragment.childFragmentManager.fragments.forEach{
            try {
                (it as? DashboardFragment)?.onDiscoveryChange(active)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        d2d?.getRequiredPermissions()?.let {
            requestPermissionsLauncher.launch(it.toTypedArray())
        }
    }
}