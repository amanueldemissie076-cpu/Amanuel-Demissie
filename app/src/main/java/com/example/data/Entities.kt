package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "registrations")
data class RegistrationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val fullName: String,
    val phoneNumber: String,
    val normalizedPhone: String,
    val email: String,
    val normalizedEmail: String,
    val ideaTitle: String,
    val categoryKey: String,
    val ideaDescription: String,
    val projectFileUri: String,
    val projectFileName: String,
    val projectFileMime: String,
    val projectFileSizeBytes: Long,
    val paymentChannelKey: String,
    val paymentReference: String,
    val receiptFileUri: String,
    val receiptFileName: String,
    val receiptFileMime: String,
    val receiptFileSizeBytes: Long,
    val statusKey: String,
    val submittedAtMillis: Long,
    val reviewedAtMillis: Long? = null
)

@Entity(
    tableName = "votes",
    indices = [Index(value = ["normalizedIdentifier"], unique = true)]
)
data class VoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val registrationId: Long,
    val rawIdentifier: String,
    val normalizedIdentifier: String,
    val identifierType: String,
    val votedAtMillis: Long
)
