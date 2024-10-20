package com.subhajitrajak.assignmentpointo.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.subhajitrajak.assignmentpointo.R
import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.subhajitrajak.assignmentpointo.databinding.ActivityBluetoothBinding

class BluetoothActivity : AppCompatActivity() {
    private val binding: ActivityBluetoothBinding by lazy {
        ActivityBluetoothBinding.inflate(layoutInflater)
    }
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanCallback: ScanCallback? = null
    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>

    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (checkPermissions(permissions)) {
            startScanning()
        } else {
            requestPermissions()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        enableBluetoothLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                startScanning()
            } else {
                Toast.makeText(
                    this,
                    "Please enable bluetooth in settings to continue",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        if (!bluetoothAdapter.isEnabled) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(intent)
        } else {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions()
            }
            if (scanCallback != null) {
                bluetoothLeScanner?.startScan(scanCallback)
            }
        }

        requestPermissions()
        binding.startScan.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                requestPermissions()
            }
            startScanning()
        }

        binding.stopScan.setOnClickListener {
            scanCallback?.let {
                stopScanning(it)
            }
        }
    }

    private fun checkPermissions(permissions: Map<String, Boolean>): Boolean {
        return permissions[Manifest.permission.BLUETOOTH] == true &&
                permissions[Manifest.permission.BLUETOOTH_SCAN] == true &&
                permissions[Manifest.permission.BLUETOOTH_ADMIN] == true &&
                permissions[Manifest.permission.BLUETOOTH_CONNECT] == true &&
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
    }

    private fun requestPermissions() {
        requestBluetoothPermissions.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        )
    }


    private fun startScanning() {
        scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val device = result.device
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermissions()

                } else {
                    connectToDevice(device)
                    runOnUiThread {
                        binding.apply {
                            deviceName.text = "Name : ${device.name}"
                            deviceAddress.text = "Address : ${device.address}"
                        }
                    }
                }
            }
        }

        bluetoothLeScanner?.startScan(scanCallback)
    }

    private fun stopScanning(scanCallback: ScanCallback) {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            return
        }
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    private fun connectToDevice(device: BluetoothDevice) {
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                super.onConnectionStateChange(gatt, status, newState)
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (ActivityCompat.checkSelfPermission(
                            applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {

                        requestPermissions()
                    }
                    Log.d("BLE", "Connected to ${device.name}")
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.d("BLE", "Disconnected from ${device.name}")
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                super.onServicesDiscovered(gatt, status)
                for (service in gatt.services) {
                    runOnUiThread {
                        binding.apply {
                            deviceService.text = "Services: ${service.uuid}"
                        }
                    }
                    for (characteristic in service.characteristics) {
                        runOnUiThread {
                            binding.apply {
                                deviceCharacteristics.text =
                                    "Characteristics : ${characteristic.uuid}"
                            }
                        }
                    }
                }
            }
        }

        device.connectGatt(this, false, gattCallback)
    }
}