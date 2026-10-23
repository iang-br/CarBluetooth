package com.example.carbluetooth
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.carbluetooth.ui.theme.CarBluetoothTheme
import java.util.Timer
import java.util.TimerTask


class MainActivity : Activity() {

    companion object {
        const val REQUEST_ENABLE_BT = 1
        const val REQUEST_BLUETOOTH_SCAN = 2
        const val REQUEST_BLUETOOTH_CONNECT_PERMISSION = 3
    }
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var devicesAdapter: ArrayAdapter<String>
    private val devicesList = mutableListOf<String>()
    private val targetDeviceName = "BT-163"
    private var isRegisteredReceiver = false

    private fun checkAndStartDiscovery() {
        if ((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED) ||
            (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            // Request permissions
            ActivityCompat.requestPermissions(
                this, arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ),
                REQUEST_BLUETOOTH_SCAN
            )
        } else {
            // Permission already granted, start discovery
            bluetoothAdapter.startDiscovery()
            afterTimeCancelDiscoveryAndReceiver()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        devicesList.add("Dispositivos Bluetooth")
        setContentView(R.layout.activity_main)

        val listView: ListView = findViewById(R.id.bluetoothDevicesList)
        devicesAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, devicesList)
        listView.adapter = devicesAdapter

        val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth não disponível neste dispositivo", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Ativar Bluetooth, se necessário
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Toast.makeText(this, "Bluetooth está desativado", Toast.LENGTH_SHORT).show()
                return
            }
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                REQUEST_BLUETOOTH_CONNECT_PERMISSION
            )
        } else {
            Log.d("BluetoothPermission","Permission BLUETOOTH_CONNECT already granted.")
        }

        // Registrar o BroadcastReceiver
        val filter1 = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter1)
        val filter2 = IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        registerReceiver(receiver, filter2)
        isRegisteredReceiver = true
        Log.d("Receiver","On")

        checkAndStartDiscovery()
    }

//            if (isDiscoveryStarted) {
//                Toast.makeText(this, "Discovery started successfully", Toast.LENGTH_SHORT).show()
//                Log.d("BluetoothDiscovery", "Discovery started successfully")
//            } else {
//                Toast.makeText(this, "Failed to start discovery", Toast.LENGTH_SHORT).show()
//                Log.e("BluetoothDiscovery", "Failed to start discovery")
//            }
//            Toast.makeText(this,"Iniciada verificação de Bluetooth",Toast.LENGTH_LONG).show()




    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        checkAndStartDiscovery()


//        when (requestCode) {
//            REQUEST_BLUETOOTH_SCAN -> {
//                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                    // Permission granted, start discovery
//
//                    Toast.makeText(this,"Iniciada verificação de Bluetooth",Toast.LENGTH_LONG).show()
//                } else {
//                    // Permission denied, handle accordingly
//                    Toast.makeText(this, "Bluetooth Scan permission denied", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
    }

    private fun afterTimeCancelDiscoveryAndReceiver() {
        val timer = Timer()
        timer.schedule(object : TimerTask() {
            @SuppressLint("MissingPermission")
            override fun run() {
                bluetoothAdapter.cancelDiscovery()
                if (isRegisteredReceiver) {
                    unregisterReceiver(receiver)
                    isRegisteredReceiver = false
                    Log.d("Receiver","TimerOff")
                }
                Toast.makeText(this@MainActivity,"Cancel discovery",Toast.LENGTH_LONG).show()
            }
        }, (10 * 60 * 1000).toLong()) // Delay of 10 seconds (10 * 1000 ms)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action: String = intent.action ?: return
            if (action == BluetoothDevice.ACTION_ACL_DISCONNECTED) {
                checkAndStartDiscovery()
            }
            else if (action == BluetoothDevice.ACTION_FOUND) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }

                val deviceName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        (device?.name ?: "Dispositivo desconhecido")
                    } else {
                        "Permissão negada"
                    }
                } else {
                    device?.name ?: "Dispositivo desconhecido"
                }

                val deviceAddress = device?.address

                // Atualizar a lista com destaque
                val displayName = if (deviceName == targetDeviceName) {
                    // Tentar conectar
                    pairAndConnect(device)
                    "<font color='red'>$deviceName</font>"
                } else {
                    deviceName
                }
                devicesList.add(displayName+ " \n(" + deviceAddress + ")")
                devicesAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun pairAndConnect(device: BluetoothDevice?) {
        device?.let {
            Toast.makeText(this, "Tentando conectar ao $targetDeviceName", Toast.LENGTH_SHORT).show()
            try {
                val method = device.javaClass.getMethod("createBond")
                method.invoke(device)
            } catch (e: Exception) {
                Toast.makeText(this, "Erro ao parear: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isRegisteredReceiver) {
            unregisterReceiver(receiver)
            isRegisteredReceiver = false
            Log.d("Receiver","DestroyOff")
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this,"Sem permissão para cancelar o bluetooth.",Toast.LENGTH_LONG).show()
            return
        }
        if (::bluetoothAdapter.isInitialized) {
            bluetoothAdapter.cancelDiscovery()
        }
    }

    override fun onStop() {
        super.onStop()
        if (isRegisteredReceiver) {
            unregisterReceiver(receiver)
            isRegisteredReceiver = false
            Log.d("Receiver","StopOff")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    CarBluetoothTheme {
        Greeting("Android")
    }
}