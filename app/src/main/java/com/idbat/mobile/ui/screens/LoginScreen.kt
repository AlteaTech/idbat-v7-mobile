package com.idbat.mobile.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.idbat.mobile.R
import com.idbat.mobile.ui.theme.VeoliaCoralText
import com.idbat.mobile.ui.theme.VeoliaGradientBot
import com.idbat.mobile.ui.theme.VeoliaGradientTop
import com.idbat.mobile.ui.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(viewModel: LoginViewModel = hiltViewModel()) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(VeoliaGradientTop, VeoliaGradientBot)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(60.dp))

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "idbat by Veolia",
                modifier = Modifier.height(72.dp)
            )

            Spacer(modifier = Modifier.height(60.dp))

            // --- DROPDOWN SITE ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(text = "Site", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = formState.expandedSite,
                    onExpandedChange = { viewModel.toggleSiteExpanded() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = formState.selectedSite?.nom ?: "Selectionner un site",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = VeoliaCoralText,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = formState.expandedSite,
                        onDismissRequest = { viewModel.dismissSite() },
                        modifier = Modifier.background(Color.White)
                    ) {
                        authState.availableSites.forEach { site ->
                            DropdownMenuItem(
                                text = { Text(site.nom, color = Color.DarkGray) },
                                onClick = { viewModel.selectSite(site) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- DROPDOWN IDENTIFIANT ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(text = "Identifiant", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(
                    expanded = formState.expandedUser,
                    onExpandedChange = { viewModel.toggleUserExpanded() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextField(
                        value = formState.selectedUser?.login ?: "Ex: Gardien",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = VeoliaCoralText,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.DarkGray,
                            unfocusedTextColor = if (formState.selectedUser == null) Color.LightGray else Color.DarkGray
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    ExposedDropdownMenu(
                        expanded = formState.expandedUser,
                        onDismissRequest = { viewModel.dismissUser() },
                        modifier = Modifier.background(Color.White)
                    ) {
                        authState.availableUtilisateursTps.forEach { user ->
                            DropdownMenuItem(
                                text = { Text(user.login, color = Color.DarkGray) },
                                onClick = { viewModel.selectUser(user) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- MOT DE PASSE ---
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
                Text(text = "Mot de passe", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = formState.password,
                    onValueChange = { viewModel.setPassword(it) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.9f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.9f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = VeoliaCoralText,
                        focusedTextColor = Color.DarkGray,
                        unfocusedTextColor = Color.DarkGray
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            if (authState.loginError != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = authState.loginError!!,
                    color = Color.Yellow,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // --- BOUTON CONNEXION ---
            Button(
                onClick = { viewModel.submit() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    disabledContainerColor = Color.White.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(30.dp),
                enabled = formState.selectedSite != null && formState.selectedUser != null && formState.password.isNotBlank()
            ) {
                Text(
                    text = "Connexion",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = VeoliaCoralText
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}
