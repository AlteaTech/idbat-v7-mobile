package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.ui.theme.VeoliaBorderGray
import com.idbat.mobile.ui.theme.VeoliaCardBackground
import com.idbat.mobile.ui.theme.VeoliaPrincipal
import com.idbat.mobile.ui.theme.VeoliaScreenBackground
import com.idbat.mobile.ui.theme.VeoliaTextDark
import com.idbat.mobile.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    errorMessage: String? = null,
    availableSites: List<SiteEntity> = emptyList(),
    onLoginClick: (String, String, Long?) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    // État pour le dropdown des sites
    var expanded by remember { mutableStateOf(false) }
    var selectedSite by remember { mutableStateOf<SiteEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VeoliaScreenBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "VEOLIA IDBAT",
            color = VeoliaPrincipal,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = VeoliaCardBackground),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                
                // --- DROPDOWN SITES ---
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedSite?.nom ?: "",
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Sélectionner un site", color = VeoliaBorderGray) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Place, contentDescription = "Site", tint = VeoliaBorderGray)
                        },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = White,
                            unfocusedContainerColor = White,
                            focusedBorderColor = VeoliaPrincipal,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = VeoliaPrincipal,
                            focusedTextColor = VeoliaTextDark,
                            unfocusedTextColor = VeoliaTextDark
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(White)
                    ) {
                        availableSites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.nom, color = VeoliaTextDark) },
                                onClick = {
                                    selectedSite = site
                                    expanded = false
                                }
                            )
                        }
                        if (availableSites.isEmpty()) {
                             DropdownMenuItem(
                                text = { Text("Aucun site disponible", color = VeoliaBorderGray) },
                                onClick = { expanded = false },
                                enabled = false
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- IDENTIFIANT ---
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    placeholder = { Text("Identifiant", color = VeoliaBorderGray) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Person, contentDescription = "Identifiant", tint = VeoliaBorderGray)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedBorderColor = VeoliaPrincipal,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = VeoliaPrincipal,
                        focusedTextColor = VeoliaTextDark,
                        unfocusedTextColor = VeoliaTextDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- MOT DE PASSE ---
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Mot de passe", color = VeoliaBorderGray) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = "Mot de passe", tint = VeoliaBorderGray)
                    },
                    trailingIcon = {
                        val image = if (passwordVisible)
                            Icons.Filled.Visibility
                        else Icons.Filled.VisibilityOff

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = "Afficher le mot de passe", tint = VeoliaBorderGray)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = White,
                        unfocusedContainerColor = White,
                        focusedBorderColor = VeoliaPrincipal,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = VeoliaPrincipal,
                        focusedTextColor = VeoliaTextDark,
                        unfocusedTextColor = VeoliaTextDark
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // --- BOUTON CONNEXION ---
                Button(
                    onClick = { onLoginClick(username, password, selectedSite?.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal),
                    shape = RoundedCornerShape(8.dp),
                    // Désactiver le bouton si le site n'est pas sélectionné
                    enabled = selectedSite != null && username.isNotBlank() && password.isNotBlank()
                ) {
                    Text(
                        text = "CONNEXION",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = if (selectedSite != null && username.isNotBlank() && password.isNotBlank()) White else Color.LightGray
                    )
                }
            }
        }
    }
}
