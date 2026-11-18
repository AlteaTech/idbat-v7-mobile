package com.idbat.mobile.data.model

import com.idbat.mobile.data.entities.CarteContratEntity
import com.idbat.mobile.data.entities.ContratEntity
import com.idbat.mobile.data.entities.SeuilEtatEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** #513 — seuils en unités de passage (miroir de SeuilAtteintHelper.estAtteintPourCarte côté back). */
class SeuilAtteintTest {

    private fun contrat(decompte: Boolean) =
        ContratEntity(id = 1L, trigramme = "TST", nom = "Test", hasDecompteunites = decompte)

    private fun carte(type: String?, unites: Int?) = CarteContratEntity(
        id = 10L, libelle = "AB-123-CD", type = type, valeur = "AB-123-CD", uidRfid = null,
        isCreationByQRCode = false, carteGriseJ1 = null, carteGriseF3 = null,
        contratId = 1L, carteId = 10L, nbUnitesADecompter = unites
    )

    /** Usager à 27 unités sur un seuil annuel de 30, en zone de prévention (isAlerte = true). */
    private fun seuil(
        isAlerte: Boolean = true, isBloquant: Boolean = true, effectues: Int = 27, autorises: Int = 30,
        isPreventionAtteinte: Boolean = true, nom: String = "Annuel", periode: String = "annuel",
        messageAtteint: String? = null
    ) =
        SeuilEtatEntity(
            usagerId = 100L, seuilId = 5L, nom = nom, nbPassagesAutorises = autorises,
            nbPassagesEffectues = effectues, isAlerte = isAlerte, isBloquant = isBloquant,
            seuilDetailNom = nom, seuilDetailType = "Periode", seuilDetailPeriode = periode,
            seuilDetailNbPassage = autorises, seuilDetailSeuilPrevention = 75, seuilDetailContratId = 1L,
            isPreventionAtteinte = isPreventionAtteinte, messageAtteint = messageAtteint
        )

    // ── Mise à jour locale entre deux synchros (apresPassage) ───────────────────

    @Test
    fun `apresPassage - 10 sur 30 plus 6u reste sous la prevention`() {
        val s = SeuilAtteint.apresPassage(seuil(effectues = 10, isPreventionAtteinte = false), 6)
        assertEquals(16, s.nbPassagesEffectues)
        assertTrue(s.isAlerte)
        assertFalse(s.isPreventionAtteinte) // floor(75 × 30 / 100) = 22
    }

    @Test
    fun `apresPassage - franchit la prevention a floor(75 x 30 sur 100) = 22`() {
        val s = SeuilAtteint.apresPassage(seuil(effectues = 20, isPreventionAtteinte = false), 2)
        assertEquals(22, s.nbPassagesEffectues)
        assertTrue(s.isAlerte)
        assertTrue(s.isPreventionAtteinte)
    }

    @Test
    fun `apresPassage - atteint le plafond, messageAtteint perime remplace par le libelle local`() {
        val s = SeuilAtteint.apresPassage(seuil(messageAtteint = "Seuil Annuel atteint, 27 passages sur l'année pour 30 autorisés"), 3)
        assertEquals(30, s.nbPassagesEffectues)
        assertFalse(s.isAlerte)
        assertTrue(s.isPreventionAtteinte)
        assertEquals(null, s.messageAtteint)
        assertEquals("Seuil Annuel atteint, 30 passages sur l'année pour 30 autorisés", SeuilEvalue.evaluer(s, contrat(true), carte("I", 1)).message)
    }

    @Test
    fun `apresPassage - CHLOE 3 sur 6 plus 2u passe en prevention (floor 4,5 = 4)`() {
        val s = SeuilAtteint.apresPassage(seuilChloe(), 2)
        assertEquals(5, s.nbPassagesEffectues)
        assertTrue(s.isAlerte)
        assertTrue(s.isPreventionAtteinte)
        // passage suivant avec la carte 2u : 2 > 1 restante → atteint, libellé local avec la bonne période
        val e = SeuilEvalue.evaluer(s, contrat(true), carte("I", 2))
        assertTrue(e.bloque)
        assertEquals("Seuil TEST513 Journalier atteint, 5 passages sur la journée pour 6 autorisés", e.message)
    }

    @Test
    fun `comptePassages - seul le type passage est incremente`() {
        assertTrue(SeuilAtteint.comptePassages(seuil()))
        assertFalse(SeuilAtteint.comptePassages(seuil().copy(seuilDetailType = "V")))
    }

    // ── #513 complément : isPreventionAtteinte = false (sous la prévention) ─────

    private val msgChloe = "Seuil TEST513 Journalier atteint, 3 passages sur la journée pour 6 autorisés"
    private fun seuilChloe() = seuil(
        effectues = 3, autorises = 6, isPreventionAtteinte = false,
        nom = "TEST513 Journalier", periode = "Quotidien", messageAtteint = msgChloe
    )

    @Test
    fun `BRUNO 10 sur 30 sous prevention, carte 6u - accepte, rien affiche`() {
        val e = SeuilEvalue.evaluer(seuil(effectues = 10, isPreventionAtteinte = false), contrat(true), carte("I", 6))
        assertFalse(e.atteint)
        assertFalse(e.visible)
        assertFalse(e.bloque)
    }

    @Test
    fun `CHLOE 3 sur 6 sous prevention, carte 6u - refuse avec messageAtteint du back`() {
        val e = SeuilEvalue.evaluer(seuilChloe(), contrat(true), carte("I", 6))
        assertTrue(e.atteint)
        assertTrue(e.atteintParUnites)
        assertTrue(e.visible)
        assertTrue(e.bloque)
        assertEquals(msgChloe, e.message)
    }

    @Test
    fun `CHLOE 3 sur 6 sous prevention, carte 2u - accepte, rien affiche`() {
        val e = SeuilEvalue.evaluer(seuilChloe(), contrat(true), carte("I", 2))
        assertFalse(e.atteint)
        assertFalse(e.visible)
        assertFalse(e.bloque)
    }

    @Test
    fun `sous prevention, carte C ou unites null ou contrat sans decompte - rien affiche`() {
        listOf(
            contrat(true) to carte("C", 6),
            contrat(true) to carte("I", null),
            contrat(false) to carte("I", 6)
        ).forEach { (c, k) ->
            val e = SeuilEvalue.evaluer(seuilChloe(), c, k)
            assertFalse(e.atteint)
            assertFalse(e.visible)
        }
    }

    @Test
    fun `ALICE 27 sur 30 en prevention, carte 3u ou code-barres - avertissement de prevention`() {
        listOf(carte("I", 3), carte("C", null)).forEach { k ->
            val e = SeuilEvalue.evaluer(seuil(), contrat(true), k)
            assertFalse(e.atteint)
            assertTrue(e.visible)
            assertFalse(e.bloque)
            assertEquals("Le seuil Annuel autorise 30 passages sur l'année, 27 déjà effectués", e.message)
        }
    }

    @Test
    fun `DENIS 30 sur 30 - refuse, libelle local historique`() {
        val e = SeuilEvalue.evaluer(
            seuil(isAlerte = false, effectues = 30, messageAtteint = "ignoré hors RG3"), contrat(true), carte("I", 1)
        )
        assertTrue(e.bloque)
        assertTrue(e.visible)
        assertEquals("Seuil Annuel atteint, 30 passages sur l'année pour 30 autorisés", e.message)
    }

    // ── RG3 : exemple du ticket 27/30 ──────────────────────────────────────────

    @Test
    fun `27 sur 30 avec vehicule de 6 unites - seuil atteint, bloque, libelle atteint`() {
        val e = SeuilEvalue.evaluer(seuil(), contrat(true), carte("I", 6))
        assertTrue(e.atteint)
        assertTrue(e.atteintParUnites)
        assertTrue(e.bloque)
        // RG3.2 : libellé « atteint » même si 27 < 30
        assertEquals("Seuil Annuel atteint, 27 passages sur l'année pour 30 autorisés", e.message)
    }

    @Test
    fun `27 sur 30 avec vehicule de 3 unites - passe (egalite non stricte)`() {
        val e = SeuilEvalue.evaluer(seuil(), contrat(true), carte("I", 3))
        assertFalse(e.atteint)
        assertFalse(e.atteintParUnites)
        assertFalse(e.bloque)
        assertEquals("Le seuil Annuel autorise 30 passages sur l'année, 27 déjà effectués", e.message)
    }

    @Test
    fun `27 sur 30 avec vehicule de 1 ou 2 unites - passe`() {
        assertFalse(SeuilAtteint.estAtteint(seuil(), contrat(true), carte("I", 1)))
        assertFalse(SeuilAtteint.estAtteint(seuil(), contrat(true), carte("I", 2)))
    }

    @Test
    fun `seuil non bloquant atteint par unites - atteint mais pas bloque`() {
        val e = SeuilEvalue.evaluer(seuil(isBloquant = false), contrat(true), carte("I", 6))
        assertTrue(e.atteint)
        assertTrue(e.atteintParUnites)
        assertFalse(e.bloque)
    }

    // ── RG1 / RG2 : hors règle → comportement historique (atteint = !isAlerte) ──

    @Test
    fun `contrat sans decompte - regle ignoree`() {
        val e = SeuilEvalue.evaluer(seuil(), contrat(false), carte("I", 6))
        assertFalse(e.atteint)
        assertFalse(e.atteintParUnites)
    }

    @Test
    fun `carte de type C - regle ignoree`() {
        val e = SeuilEvalue.evaluer(seuil(), contrat(true), carte("C", 6))
        assertFalse(e.atteint)
        assertFalse(e.atteintParUnites)
    }

    @Test
    fun `nbUnitesADecompter null - regle ignoree`() {
        val e = SeuilEvalue.evaluer(seuil(), contrat(true), carte("I", null))
        assertFalse(e.atteint)
        assertFalse(e.atteintParUnites)
    }

    @Test
    fun `carte ou contrat absents - regle ignoree`() {
        assertFalse(SeuilAtteint.estAtteint(seuil(), null, carte("I", 6)))
        assertFalse(SeuilAtteint.estAtteint(seuil(), contrat(true), null))
    }

    // ── Comportement existant : seuil déjà atteint (isAlerte = false) ──────────

    @Test
    fun `seuil deja atteint - toujours atteint, pas compte comme RG3`() {
        listOf(contrat(false) to carte("C", null), contrat(true) to carte("I", 1)).forEach { (c, k) ->
            val e = SeuilEvalue.evaluer(seuil(isAlerte = false, effectues = 30), c, k)
            assertTrue(e.atteint)
            assertFalse(e.atteintParUnites)
            assertTrue(e.bloque)
            assertEquals("Seuil Annuel atteint, 30 passages sur l'année pour 30 autorisés", e.message)
        }
    }
}
