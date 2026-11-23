package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CompetitionDao {
    @Query("SELECT * FROM registrations ORDER BY submittedAtMillis DESC")
    fun observeAllRegistrations(): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE statusKey = :approvedStatus ORDER BY submittedAtMillis DESC")
    fun observeRegistrationsByStatus(approvedStatus: String): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE id = :id LIMIT 1")
    suspend fun getRegistrationById(id: Long): RegistrationEntity?

    @Query("SELECT * FROM registrations WHERE normalizedEmail = :normalizedEmail OR normalizedPhone = :normalizedPhone LIMIT 1")
    suspend fun findExistingRegistration(
        normalizedEmail: String,
        normalizedPhone: String
    ): RegistrationEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRegistration(entity: RegistrationEntity): Long

    @Query("UPDATE registrations SET statusKey = :statusKey, reviewedAtMillis = :reviewedAtMillis WHERE id = :id")
    suspend fun updateRegistrationStatus(id: Long, statusKey: String, reviewedAtMillis: Long)

    @Query("SELECT * FROM votes ORDER BY votedAtMillis DESC")
    fun observeAllVotes(): Flow<List<VoteEntity>>

    @Query("SELECT * FROM votes WHERE normalizedIdentifier = :normalizedIdentifier LIMIT 1")
    suspend fun findVoteByIdentifier(normalizedIdentifier: String): VoteEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertVote(vote: VoteEntity): Long
}
