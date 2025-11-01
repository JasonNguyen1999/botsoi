package com.example.botsoi

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AppContent()
            }
        }
    }

    // Hàm khởi chạy FloatingService
    private fun startFloatingService(tableId: Int, tableName: String) {
        val intent = Intent(this, FloatingService::class.java).apply {
            putExtra(FloatingService.EXTRA_TABLE_ID, tableId)
            putExtra(FloatingService.EXTRA_TABLE_NAME, tableName)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        // đóng Activity để app thu nhỏ
        finish()
    }

    @Composable
    fun AppContent() {
        var loggedIn by rememberSaveable { mutableStateOf(false) }
        var username by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        val tables = List(12) { index -> "Bàn ${index + 1}" }

        if (!loggedIn) {
            // ===== MÀN HÌNH ĐĂNG NHẬP =====
            val focusManager = LocalFocusManager.current

            Box(modifier = Modifier.fillMaxSize()) {
                // Ảnh nền
                Image(
                    painter = painterResource(id = R.drawable.bg_login), // thêm ảnh vào thư mục res/drawable/bg_login.jpg
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Lớp phủ mờ để chữ dễ đọc
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.Black.copy(alpha = 0.6f)
                                )
                            )
                        )
                )

                // Form đăng nhập
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        elevation = CardDefaults.cardElevation(12.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "BotSoi",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    color = Color(0xFF2C3E50),
                                    fontSize = 28.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("Tài khoản") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null)
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Mật khẩu") },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null)
                                },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    if (username == "admin" && password == "1234") {
                                        loggedIn = true
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A90E2))
                            ) {
                                Text("Đăng nhập", color = Color.White, fontSize = 18.sp)
                            }
                        }
                    }
                }
            }
        } else {
            // ===== DANH SÁCH BÀN =====
            Box(modifier = Modifier.fillMaxSize()) {
                // Nền gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF4A90E2), Color(0xFF8E44AD))
                            )
                        )
                )

                Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                    Text(
                        text = "Danh sách bàn",
                        style = MaterialTheme.typography.headlineSmall.copy(color = Color.White),
                        modifier = Modifier.padding(8.dp)
                    )

                    LazyVerticalGrid(columns = GridCells.Fixed(3), modifier = Modifier.fillMaxSize()) {
                        items(tables) { t ->
                            Card(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clickable { ensureOverlayPermissionAndStart(t) },
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(8.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f))
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        t,
                                        style = MaterialTheme.typography.bodyLarge.copy(color = Color(0xFF2C3E50))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun ensureOverlayPermissionAndStart(tableName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        } else {
            val tableId = tableName.filter { it.isDigit() }.toIntOrNull() ?: 0
            startFloatingService(tableId, tableName)
        }
    }
}
