package com.idbat.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idbat.mobile.data.model.InfoCartePassage
import com.idbat.mobile.data.model.SaisieMatiereLigne
import com.idbat.mobile.ui.theme.VeoliaGradientTop
import com.idbat.mobile.ui.theme.VeoliaPrincipal
import com.idbat.mobile.ui.theme.VeoliaSubtle
import com.idbat.mobile.ui.theme.White
import java.text.SimpleDateFormat
import java.util.*

private val dateFmt = SimpleDateFormat("dd/MM/yyyy à HH:mm", Locale.FRANCE)

@Composable
fun ValidationPassageScreen(
    siteName: String,
    siteId: Long,
    info: InfoCartePassage,
    lignes: List<SaisieMatiereLigne>,
    onBack: () -> Unit,
    onValider: () -> Unit = {}
) {
    val bgColor = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to VeoliaGradientTop,
                        0.35f to bgColor,
                        1f to bgColor
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Retour",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                Text(
                    text = "Validation",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = siteName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Carte usager / passage
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Passage",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))

                            ValidationRow(label = "Site", value = siteName)
                            ValidationRow(label = "Site ID", value = siteId.toString(), isId = true)
                            if (!info.societe.isNullOrBlank())
                                ValidationRow(label = "Société", value = info.societe)
                            ValidationRow(label = "Titulaire", value = info.nomTitulaire)
                            ValidationRow(label = "N° carte", value = info.numeroCarte, isId = true)
                            ValidationRow(label = "Type apporteur", value = info.typeApporteur)
                            ValidationRow(label = "Contact", value = info.contact)
                        }
                    }
                }

                // Carte matières
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Inventory2,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Matières (${lignes.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            lignes.forEachIndexed { index, ligne ->
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = VeoliaSubtle)
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = ligne.matiere.libelle,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                ValidationRow(label = "Matière ID",    value = ligne.matiere.matiereId.toString(), isId = true)
                                ValidationRow(label = "Site ID",       value = ligne.matiere.siteId.toString(),    isId = true)
                                ValidationRow(label = "Quantité",      value = "${ligne.quantite} ${ligne.matiere.unitesDesApportLibelle}")
                                ValidationRow(label = "Unité ID",      value = ligne.matiere.unitesDesApportId.toString(), isId = true)
                                ValidationRow(label = "Usager",        value = ligne.usagerNom)
                                ValidationRow(label = "N° carte",      value = ligne.numeroCarte, isId = true)
                                ValidationRow(label = "Date",          value = dateFmt.format(Date(ligne.date)))
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(4.dp)) }
            }

            Button(
                onClick = onValider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = VeoliaPrincipal)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Valider le passage",
                    color = White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ValidationRow(label: String, value: String?, isId: Boolean = false) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isId) FontWeight.Normal else FontWeight.Medium,
            color = if (isId) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
        )
    }
}
