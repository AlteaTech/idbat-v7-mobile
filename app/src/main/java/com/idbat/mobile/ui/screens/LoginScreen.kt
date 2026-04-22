package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.data.entities.SiteEntity
import com.idbat.mobile.data.entities.UtilisateurTPEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    errorMessage: String? = null,
    availableSites: List<SiteEntity> = emptyList(),
    availableUsers: List<UtilisateurTPEntity> = emptyList(),
    onLoginClick: (String, String, Long?) -> Unit
) {
    var password by remember { mutableStateOf("") }
    val passwordVisible by remember { mutableStateOf(false) }

    var expandedSite by remember { mutableStateOf(false) }
    var selectedSite by remember { mutableStateOf<SiteEntity?>(null) }
    
    var expandedUser by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UtilisateurTPEntity?>(null) }

    val GradientTop = Color(0xFFF8A282) // Orange clair
    val GradientBottom = Color(0xFFE96D71) // Rose corail
    val CoralText = Color(0xFFEA6E72) // Couleur du texte du bouton et des flèches

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GradientTop, GradientBottom)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // Gère l'espace de la barre de statut (heure/batterie)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(60.dp))

            // --- LOGO ---
            // Placeholder temporaire en attendant l'image :
            Text(
                text = "idbat",
                color = Color.White,
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp
            )
            Text(
                text = "by VEOLIA",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(60.dp))

            // --- DROPDOWN SITE ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Site",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = expandedSite,
                    onExpandedChange = { expandedSite = !expandedSite },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = selectedSite?.nom ?: "Selectionner un site", // ou "" par défaut
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = CoralText,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent, // Enlève la ligne du bas
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedSite,
                        onDismissRequest = { expandedSite = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        availableSites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.nom, color = Color.DarkGray) },
                                onClick = {
                                    selectedSite = site
                                    expandedSite = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- IDENTIFIANT (Désormais un Dropdown) ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Identifiant",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(
                    expanded = expandedUser,
                    onExpandedChange = { expandedUser = !expandedUser },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = selectedUser?.login ?: "Ex: Gardien",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = CoralText,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = if (selectedUser == null) Color.LightGray else Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = expandedUser,
                        onDismissRequest = { expandedUser = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        availableUsers.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.login, color = Color.DarkGray) },
                                onClick = {
                                    selectedUser = user
                                    expandedUser = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- MOT DE PASSE ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(
                    text = "Mot de passe",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = CoralText,
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage,
                    color = Color.Yellow, // Mieux visible sur fond rouge/rose que le Material error standard
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f)) // Pousse le bouton vers le bas

            // --- BOUTON CONNEXION ---
            Button(
                onClick = { 
                    selectedUser?.let { user ->
                        onLoginClick(user.login, password, selectedSite?.id) 
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(30.dp), // Bouton très arrondi (Pill shape)
                enabled = selectedSite != null && selectedUser != null && password.isNotBlank()
            ) {
                Text(
                    text = "Connexion",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = CoralText // Texte couleur corail
                )
            }

            Spacer(modifier = Modifier.height(40.dp)) // Espace en bas de l'écran
        }
    }
}
