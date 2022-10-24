package com.sorbonne.atom_d

import android.annotation.SuppressLint
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.nearby.connection.Payload
import com.sorbonne.atom_d.ui.dashboard.DashboardFragment
import com.sorbonne.d2d.D2D
import com.sorbonne.d2d.D2DListener
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.sorbonne.atom_d.services.Socket
import com.sorbonne.atom_d.ui.relay_selection.RelaySelectionFragment
import org.json.JSONObject


class MainActivity : AppCompatActivity(), D2DListener {

    private val TAG = MainActivity::class.simpleName

    private lateinit var viewModel: MainViewModel

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var appBarConfiguration: AppBarConfiguration

    private var socketServiceIntent: Intent ?= null
    private var isSocketServiceBound = false

    var d2d: D2D ?= null
    var androidId: String ?= null
    var socketService: Socket ?= null

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

        socketServiceIntent = Intent(this@MainActivity, Socket::class.java)

        viewModel.instance = d2d

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        findViewById<View>(R.id.main_layout)

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_container) as NavHostFragment

        navController = navHostFragment.navController
        findViewById<BottomNavigationView>(R.id.bottom_nav).setupWithNavController(navController)

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.relaySelectionFragment2, R.id.dashboardFragment, R.id.experimentFragment, R.id. aboutUsFragment)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration)
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

    override fun onDeviceConnected(isActive: Boolean, endPointInfo: JSONObject) {
        super.onDeviceConnected(isActive, endPointInfo)
        navHostFragment.childFragmentManager.fragments.forEach{
            try {
                (it as? DashboardFragment)?.onDeviceConnected(isActive, endPointInfo)
                (it as? RelaySelectionFragment)?.onDeviceConnected(isActive, endPointInfo)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

	override fun onReceivedChunk(payload: Payload) {
        super.onReceivedChunk(payload)
        navHostFragment?.childFragmentManager?.fragments?.forEach{
            try {
                (it as? RelaySelectionFragment)?.onReceivedChunk(payload)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }
    
    override fun onExperimentProgress(isExperimentBar: Boolean, progression: Int) {
        super.onExperimentProgress(isExperimentBar, progression)
        navHostFragment.childFragmentManager.fragments.forEach{
            try {
                (it as? DashboardFragment)?.onExperimentProgress(isExperimentBar, progression)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onInfoPacketReceived(payload: String) {
        super.onInfoPacketReceived(payload)
        navHostFragment.childFragmentManager.fragments.forEach{
            try {
                (it as? DashboardFragment)?.onInfoPacketReceived(payload)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onReceivedTaskResul(from: D2D.ParameterTag, value: JSONObject) {
        super.onReceivedTaskResul(from, value)
        navHostFragment.childFragmentManager.fragments.forEach{
            try {
                (it as? DashboardFragment)?.onReceivedTaskResul(from, value)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    override fun onLastLocation(location: Location) {
        super.onLastLocation(location)
        navHostFragment.childFragmentManager.fragments.forEach{
            try {
                (it as? DashboardFragment)?.onLastLocation(location)
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

    override fun onResume() {
        super.onResume()
        startService(Intent(this, Socket::class.java))
        isSocketServiceBound = bindService(socketServiceIntent, SocketServiceConnection, BIND_AUTO_CREATE)
    }

    override fun onPause() {
        super.onPause()
        if(isSocketServiceBound){
            unbindService(SocketServiceConnection)
            isSocketServiceBound = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, Socket::class.java))
    }
    
    private val SocketServiceConnection : ServiceConnection = object : ServiceConnection{
        override fun onServiceConnected(componentName: ComponentName?, service: IBinder?) {
            socketService = (service as Socket.LocalBinder).getService()
            viewModel.socketService = socketService
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            TODO("Not yet implemented")
        }
    }
}