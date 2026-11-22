package com.aethertv.v2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aethertv.v2.ui.theme.MatteGlassTokens
import com.aethertv.v2.ui.theme.livelyPress

@Composable
fun AddPlaylistDialog(
    initialName: String = "",
    initialHost: String = "",
    initialUser: String = "",
    initialPass: String = "",
    initialM3u: String = "",
    onDismiss: () -> Unit,
    onLoadXtream: (name: String, host: String, user: String, pass: String) -> Unit,
    onLoadM3u: (name: String, url: String) -> Unit
) {
    var mode by remember { mutableIntStateOf(0) }
    var serverName by remember { mutableStateOf(initialName) }
    var host by remember { mutableStateOf(initialHost) }
    var user by remember { mutableStateOf(initialUser) }
    var pass by remember { mutableStateOf(initialPass) }
    var m3uUrl by remember { mutableStateOf(initialM3u) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MatteGlassTokens.SheetBg)
                .border(1.dp, MatteGlassTokens.SpecularBorderBrush, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Add Server / Playlist",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MatteGlassTokens.CharcoalPrimary
            )

            OutlinedTextField(
                value = serverName,
                onValueChange = { serverName = it },
                label = { Text("Server Name (e.g. Strong 8K)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MatteGlassTokens.SubtleSurface)
                    .border(0.75.dp, MatteGlassTokens.NeutralBorder, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Xtream Codes", "M3U URL").forEachIndexed { idx, label ->
                    val selected = mode == idx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) MatteGlassTokens.CharcoalPrimary else Color.Transparent)
                            .clickable { mode = idx }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) MatteGlassTokens.CanvasWhite else MatteGlassTokens.CharcoalMuted
                        )
                    }
                }
            }

            if (mode == 0) {
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    label = { Text("Server URL (http://...)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = pass,
                    onValueChange = { pass = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = m3uUrl,
                    onValueChange = { m3uUrl = it },
                    label = { Text("M3U / M3U8 Playlist Link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .livelyPress(pressedScale = 0.94f, onClick = onDismiss)
                        .clip(RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text("Cancel", color = MatteGlassTokens.CharcoalMuted, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .livelyPress(pressedScale = 0.94f) {
                            val resolvedName = serverName.trim().ifEmpty {
                                if (mode == 0) host.trim().removePrefix("http://").removePrefix("https://").substringBefore("/")
                                else "M3U Playlist"
                            }.ifEmpty { "My Server" }

                            if (mode == 0 && host.isNotBlank() && user.isNotBlank() && pass.isNotBlank()) {
                                onLoadXtream(resolvedName, host, user, pass)
                            } else if (mode == 1 && m3uUrl.isNotBlank()) {
                                onLoadM3u(resolvedName, m3uUrl)
                            }
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(MatteGlassTokens.CharcoalPrimary)
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Text("Save & Load", color = MatteGlassTokens.CanvasWhite, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
