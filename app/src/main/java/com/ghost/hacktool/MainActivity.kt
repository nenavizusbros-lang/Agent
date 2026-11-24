package com.ghost.hacktool

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.math.sin

class MainActivity : ComponentActivity() {
    
    private val hackPrank by lazy { HackPrank(this) }
    private var isHacking by mutableStateOf(false)
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            Toast.makeText(this, "✅ Все разрешения получены", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "⚠️ Некоторые разрешения не получены", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            HackToolTheme {
                MainScreen(
                    onRequestPermissions = { requestAllPermissions() },
                    onStartHack = { startHack() },
                    onStopHack = { stopHack() },
                    isHacking = isHacking
                )
            }
        }
    }
    
    private fun requestAllPermissions() {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
        }
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
    
    private fun startHack() {
        if (!checkPermissions()) {
            Toast.makeText(this, "❌ Сначала получи разрешения!", Toast.LENGTH_LONG).show()
            return
        }
        
        isHacking = true
        hackPrank.startFullHack()
        Toast.makeText(this, "💀 HACK ACTIVATED", Toast.LENGTH_SHORT).show()
    }
    
    private fun stopHack() {
        isHacking = false
        hackPrank.stopFullHack()
        Toast.makeText(this, "⏹ HACK STOPPED", Toast.LENGTH_SHORT).show()
    }
    
    private fun checkPermissions(): Boolean {
        val btPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        val locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        return btPermission && locationPermission
    }
}

class HackPrank(private val context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var audioTrack: AudioTrack? = null
    
    private fun startIrStrobe() {
        val irManager = context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(context, "⚠️ ИК-порт не найден", Toast.LENGTH_SHORT).show()
            return
        }
        
        val frequency = 38000
        val pattern = intArrayOf(500, 500)
        
        val runnable = object : Runnable {
            override fun run() {
                if (isRunning) {
                    try {
                        irManager.transmit(frequency, pattern)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    handler.postDelayed(this, 100)
                }
            }
        }
        handler.post(runnable)
    }
    
    private fun startBlePanic() {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val btAdapter = btManager?.adapter
        val advertiser: BluetoothLeAdvertiser = btAdapter?.bluetoothLeAdvertiser ?: run {
            Toast.makeText(context, "⚠️ BLE advertising не поддерживается", Toast.LENGTH_SHORT).show()
            return
        }
        
        val scaryNames = listOf(
            "AirTag-XXXX",
            "iPhone-Sync-Active",
            "Apple-ID-Transfer",
            "FindMy-Unknown",
            "Data-Exfil-88",
            "iCloud-Backup",
            "Microphone-Active",
            "Camera-Stream",
            "Location-Tracker",
            "System-Override"
        )
        
        var index = 0
        val runnable = object : Runnable {
            override fun run() {
                if (!isRunning) return
                
                try {
                    advertiser.stopAdvertising(object : AdvertiseCallback() {})
                    
                    val data = AdvertiseData.Builder()
                        .setIncludeDeviceName(false)
                        .addServiceUuid(android.os.ParcelUuid(UUID.randomUUID()))
                        .build()
                    
                    val settings = AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                        .setConnectable(false)
                        .build()
                    
                    advertiser.startAdvertising(settings, data, object : AdvertiseCallback() {
                        override fun onStartFailure(errorCode: Int) {}
                    })
                    
                    index++
                    handler.postDelayed(this, 2000)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        handler.post(runnable)
    }
    
    private fun startUltrasonic() {
        try {
            val sampleRate = 48000
            val frequency = 17500.0
            val duration = 60
            val numSamples = sampleRate * duration
            val buffer = ByteArray(numSamples * 2)
            
            for (i in 0 until numSamples) {
                val sample = (sin(2.0 * Math.PI * i / (sampleRate / frequency)) * 32767).toInt()
                buffer[i * 2] = (sample and 0xff).toByte()
                buffer[i * 2 + 1] = ((sample >> 8) and 0xff).toByte()
            }
            
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            
            audioTrack?.write(buffer, 0, buffer.size)
            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun startFullHack() {
        isRunning = true
        startIrStrobe()
        startBlePanic()
        startUltrasonic()
    }
    
    fun stopFullHack() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val advertiser = btManager?.adapter?.bluetoothLeAdvertiser
        try {
            advertiser?.stopAdvertising(object : AdvertiseCallback() {})
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun MainScreen(
    onRequestPermissions: () -> Unit,
    onStartHack: () -> Unit,
    onStopHack: () -> Unit,
    isHacking: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0a0a0a),
                        Color(0xFF1a0000)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Text(
                text = "💀 GHOST TOOL",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                color = Color.Red
            )
            
            Text(
                text = "iPhone Killer",
                fontSize = 20.sp,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { if (isHacking) onStopHack() else onStartHack() },
                modifier = Modifier
                    .size(200.dp)
                    .scale(if (isHacking) scale else 1f),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isHacking) Color.Red else Color(0xFF8B0000)
                )
            ) {
                Text(
                    text = if (isHacking) "STOP" else "HACK",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            if (isHacking) {
                Text(
                    text = "⚠️ ATTACK IN PROGRESS",
                    fontSize = 16.sp,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = onRequestPermissions,
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF333333)
                )
            ) {
                Text(
                    text = "🔓 ПОЛУЧИТЬ РАЗРЕШЕНИЯ",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "⚠️ Используйте только для пранков!",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun HackToolTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color.Red,
            background = Color.Black,
            surface = Color(0xFF1a1a1a)
        ),
        content = content
    )
}
