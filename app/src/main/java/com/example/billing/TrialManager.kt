package com.example.billing

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.TimeUnit

enum class TrialStatus {
    ACTIVE,         // Essai en cours (4 à 30 jours restants)
    EXPIRING_SOON,  // Bientôt expiré (1 à 3 jours restants)
    EXPIRED         // Expiré (0 jour ou falsification d'horloge)
}

data class TrialState(
    val status: TrialStatus,
    val daysRemaining: Int,
    val firstLaunchTimestamp: Long,
    val lastRecordedTimestamp: Long,
    val isTampered: Boolean = false
) {
    /**
     * Indique si l'utilisateur est autorisé à lancer un scan (en essai ou si abonné).
     */
    fun isScanAllowed(isPremium: Boolean): Boolean {
        if (isPremium) return true
        return status != TrialStatus.EXPIRED && !isTampered
    }
}

/**
 * Gestionnaire de la période d'essai de 30 jours avec protection anti-fraude d'horloge système.
 */
class TrialManager(
    context: Context,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _trialState = MutableStateFlow(computeInitialTrialState())
    val trialState: StateFlow<TrialState> = _trialState.asStateFlow()

    private fun computeInitialTrialState(): TrialState {
        val now = clock()
        var firstLaunch = prefs.getLong(KEY_FIRST_LAUNCH, 0L)
        var lastRecorded = prefs.getLong(KEY_LAST_RECORDED, 0L)
        var isTampered = prefs.getBoolean(KEY_IS_TAMPERED, false)

        if (firstLaunch == 0L) {
            // Premier lancement de l'application : initialisation de l'horodatage inviolable
            firstLaunch = now
            lastRecorded = now
            prefs.edit()
                .putLong(KEY_FIRST_LAUNCH, firstLaunch)
                .putLong(KEY_LAST_RECORDED, lastRecorded)
                .putBoolean(KEY_IS_TAMPERED, false)
                .apply()
        } else {
            // Vérification anti-triche : si l'heure du système a reculé avant la dernière date connue
            // avec une marge de tolérance de 1 heure pour les fuseaux horaires
            if (now < (lastRecorded - TimeUnit.HOURS.toMillis(1))) {
                isTampered = true
                prefs.edit().putBoolean(KEY_IS_TAMPERED, true).apply()
            } else if (now > lastRecorded) {
                // Avance normale du temps : enregistrement de la nouvelle borne supérieure
                lastRecorded = now
                prefs.edit().putLong(KEY_LAST_RECORDED, lastRecorded).apply()
            }
        }

        return evaluateState(now, firstLaunch, lastRecorded, isTampered)
    }

    /**
     * Recalcule l'état actuel de la période d'essai.
     */
    fun refreshTrialState(): TrialState {
        val state = computeInitialTrialState()
        _trialState.value = state
        return state
    }

    private fun evaluateState(
        currentTime: Long,
        firstLaunch: Long,
        lastRecorded: Long,
        isTampered: Boolean
    ): TrialState {
        if (isTampered) {
            return TrialState(
                status = TrialStatus.EXPIRED,
                daysRemaining = 0,
                firstLaunchTimestamp = firstLaunch,
                lastRecordedTimestamp = lastRecorded,
                isTampered = true
            )
        }

        val elapsedMillis = (currentTime - firstLaunch).coerceAtLeast(0L)
        val elapsedDays = TimeUnit.MILLISECONDS.toDays(elapsedMillis).toInt()
        val daysRemaining = (TRIAL_DURATION_DAYS - elapsedDays).coerceAtLeast(0)

        val status = when {
            daysRemaining <= 0 -> TrialStatus.EXPIRED
            daysRemaining in 1..3 -> TrialStatus.EXPIRING_SOON
            else -> TrialStatus.ACTIVE
        }

        return TrialState(
            status = status,
            daysRemaining = daysRemaining,
            firstLaunchTimestamp = firstLaunch,
            lastRecordedTimestamp = lastRecorded,
            isTampered = false
        )
    }

    /**
     * Méthode de simulation pour tests unitaires et vérifications.
     */
    fun setCustomFirstLaunchForTesting(customTimestamp: Long) {
        prefs.edit()
            .putLong(KEY_FIRST_LAUNCH, customTimestamp)
            .putLong(KEY_LAST_RECORDED, customTimestamp)
            .putBoolean(KEY_IS_TAMPERED, false)
            .apply()
        refreshTrialState()
    }

    fun resetTrialForTesting() {
        prefs.edit().clear().apply()
        refreshTrialState()
    }

    companion object {
        const val TRIAL_DURATION_DAYS = 30
        private const val PREFS_NAME = "netward_trial_prefs"
        private const val KEY_FIRST_LAUNCH = "first_launch_timestamp"
        private const val KEY_LAST_RECORDED = "last_recorded_timestamp"
        private const val KEY_IS_TAMPERED = "is_clock_tampered"
    }
}
