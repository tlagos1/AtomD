package com.sorbonne.atom_d

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import com.sorbonne.atom_d.ui.dashboard.DashboardFragment
import com.sorbonne.d2d.D2D
import com.sorbonne.d2d.D2DListener
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity(), D2DListener {

    private val tag = MainActivity::class.simpleName

    private lateinit var viewModel: MainViewModel


    private val navHostFragment: NavHostFragment? by lazy {
        supportFragmentManager.findFragmentById(R.id.nav_host_container) as? NavHostFragment
    }


    var d2d: D2D ?= null
    var androidId: String ?= null

    private val requestPermissionsLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val isGranted = !result.containsValue(false)
        if (isGranted) {
            Log.i(tag, "permissions granted")
        }
        else {
            result.filter { !it.value }.keys.forEach { permission ->
                Log.w(tag, "permission $permission missing")
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

        if(savedInstanceState==null) {
            setupBottomNavigation()
        }
    }

    override fun onConnectivityChange(active: Boolean) {
        super.onConnectivityChange(active)
        navHostFragment?.childFragmentManager?.fragments?.forEach{
            try {
                (it as? DashboardFragment)?.onConnectivityChange(active)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onDiscoveryChange(active: Boolean) {
        super.onDiscoveryChange(active)
        navHostFragment?.childFragmentManager?.fragments?.forEach{
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


    private fun setupBottomNavigation(){
        val bottomNavigationView : BottomNavigationView = findViewById(R.id.bottom_nav)

        val navGraphList = mutableListOf<Int>()
        navGraphList.add(R.navigation.dashboard)
        navGraphList.add(R.navigation.experiment)
        navGraphList.add(R.navigation.about_us)
        
        NavigationExtensions()
            .setupWithNavController(
                bottomNavigationView,
                navGraphList,
                supportFragmentManager,
                R.id.nav_host_container,
                intent
            )
    }
}