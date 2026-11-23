package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.competitionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "call_for_growth_prefs"
)

class CompetitionPreferences(private val context: Context) {
    private object Keys {
        val HAS_VOTED = booleanPreferencesKey("has_voted_on_device")
        val VOTED_REG_ID = longPreferencesKey("voted_registration_id")
        val VOTED_MASKED_ID = stringPreferencesKey("voted_identifier_masked")
        val VOTED_AT = longPreferencesKey("voted_at_millis")

        val TELEBIRR_NUMBER = stringPreferencesKey("cfg_telebirr_number_v2")
        val TELEBIRR_NAME = stringPreferencesKey("cfg_telebirr_name_v2")
        val BANK_NUMBER = stringPreferencesKey("cfg_bank_number_v2")
        val BANK_NAME = stringPreferencesKey("cfg_bank_name_v2")
        val OTHER_BANK_NOTE = stringPreferencesKey("cfg_other_bank_note_v2")

        val STAGE_REG_OPEN = booleanPreferencesKey("stage_registration_open")
        val STAGE_VOTING_OPEN = booleanPreferencesKey("stage_voting_open")
        val STAGE_WINNERS_PUBLISHED = booleanPreferencesKey("stage_winners_published")
    }

    val deviceVoteLock: Flow<DeviceVoteLock> = context.competitionDataStore.data.map { prefs ->
        DeviceVoteLock(
            hasVotedOnDevice = prefs[Keys.HAS_VOTED] ?: false,
            votedRegistrationId = prefs[Keys.VOTED_REG_ID] ?: -1L,
            votedIdentifierMasked = prefs[Keys.VOTED_MASKED_ID] ?: "",
            votedAtMillis = prefs[Keys.VOTED_AT] ?: 0L
        )
    }

    val paymentAccountsConfig: Flow<PaymentAccountsConfig> = context.competitionDataStore.data.map { prefs ->
        val defaults = PaymentAccountsConfig()
        PaymentAccountsConfig(
            registrationFeeLabel = defaults.registrationFeeLabel,
            telebirrNumber = prefs[Keys.TELEBIRR_NUMBER] ?: defaults.telebirrNumber,
            telebirrAccountName = prefs[Keys.TELEBIRR_NAME] ?: defaults.telebirrAccountName,
            bankAccountNumber = prefs[Keys.BANK_NUMBER] ?: defaults.bankAccountNumber,
            bankAccountName = prefs[Keys.BANK_NAME] ?: defaults.bankAccountName,
            otherBankNote = prefs[Keys.OTHER_BANK_NOTE] ?: defaults.otherBankNote
        )
    }

    val competitionStagesConfig: Flow<CompetitionStagesConfig> = context.competitionDataStore.data.map { prefs ->
        CompetitionStagesConfig(
            registrationOpen = prefs[Keys.STAGE_REG_OPEN] ?: true,
            votingOpen = prefs[Keys.STAGE_VOTING_OPEN] ?: true,
            winnersPublished = prefs[Keys.STAGE_WINNERS_PUBLISHED] ?: false
        )
    }

    suspend fun getCurrentVoteLock(): DeviceVoteLock = deviceVoteLock.first()

    suspend fun getCurrentStagesConfig(): CompetitionStagesConfig = competitionStagesConfig.first()

    suspend fun lockDeviceVote(
        registrationId: Long,
        maskedIdentifier: String,
        votedAtMillis: Long
    ) {
        context.competitionDataStore.edit { prefs ->
            prefs[Keys.HAS_VOTED] = true
            prefs[Keys.VOTED_REG_ID] = registrationId
            prefs[Keys.VOTED_MASKED_ID] = maskedIdentifier
            prefs[Keys.VOTED_AT] = votedAtMillis
        }
    }

    suspend fun savePaymentAccountsConfig(config: PaymentAccountsConfig) {
        context.competitionDataStore.edit { prefs ->
            prefs[Keys.TELEBIRR_NUMBER] = config.telebirrNumber.trim()
            prefs[Keys.TELEBIRR_NAME] = config.telebirrAccountName.trim()
            prefs[Keys.BANK_NUMBER] = config.bankAccountNumber.trim()
            prefs[Keys.BANK_NAME] = config.bankAccountName.trim()
            prefs[Keys.OTHER_BANK_NOTE] = config.otherBankNote.trim()
        }
    }

    suspend fun saveCompetitionStagesConfig(config: CompetitionStagesConfig) {
        context.competitionDataStore.edit { prefs ->
            prefs[Keys.STAGE_REG_OPEN] = config.registrationOpen
            prefs[Keys.STAGE_VOTING_OPEN] = config.votingOpen
            prefs[Keys.STAGE_WINNERS_PUBLISHED] = config.winnersPublished
        }
    }

    suspend fun resetAllForTesting() {
        context.competitionDataStore.edit { it.clear() }
    }
}
