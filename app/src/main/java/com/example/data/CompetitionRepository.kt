package com.example.data

import android.util.Patterns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class CompetitionRepository(
    private val dao: CompetitionDao,
    private val preferences: CompetitionPreferences
) {
    val allRegistrations: Flow<List<RegistrationEntity>> = dao.observeAllRegistrations()
    val allVotes: Flow<List<VoteEntity>> = dao.observeAllVotes()
    val deviceVoteLock: Flow<DeviceVoteLock> = preferences.deviceVoteLock
    val paymentAccountsConfig: Flow<PaymentAccountsConfig> = preferences.paymentAccountsConfig
    val competitionStagesConfig: Flow<CompetitionStagesConfig> = preferences.competitionStagesConfig

    val approvedNomineesWithVotes: Flow<List<NomineeWithVotes>> = combine(
        dao.observeRegistrationsByStatus(RegistrationStatus.APPROVED.name),
        dao.observeAllVotes()
    ) { approved, votes ->
        val countsByReg = votes.groupingBy { it.registrationId }.eachCount()
        val totalVotes = votes.size.coerceAtLeast(1)
        approved.map { reg ->
            val count = countsByReg[reg.id] ?: 0
            NomineeWithVotes(
                registration = reg,
                voteCount = count,
                voteSharePercent = if (votes.isEmpty()) 0f else (count.toFloat() / totalVotes.toFloat())
            )
        }.sortedByDescending { it.voteCount }
    }

    val totalVotesCast: Flow<Int> = dao.observeAllVotes().combine(
        dao.observeRegistrationsByStatus(RegistrationStatus.APPROVED.name)
    ) { votes, _ -> votes.size }

    suspend fun submitRegistration(
        fullName: String,
        phoneNumber: String,
        email: String,
        ideaTitle: String,
        category: IdeaCategory,
        ideaDescription: String,
        projectFile: PickedFileInfo,
        paymentChannel: PaymentChannel,
        paymentReference: String,
        receiptFile: PickedFileInfo
    ): Result<SubmissionConfirmation> {
        val stages = preferences.getCurrentStagesConfig()
        if (!stages.registrationOpen) {
            return Result.failure(
                IllegalStateException(
                    "ምዝገባው በአሁኑ ጊዜ በአስተዳዳሪው ተዘግቷል። / Registration is currently closed by the organizer."
                )
            )
        }
        val cleanName = fullName.trim()
        val cleanPhone = phoneNumber.trim()
        val cleanEmail = email.trim()
        val normPhone = normalizePhone(cleanPhone)
        val normEmail = normalizeEmail(cleanEmail)
        val cleanTitle = ideaTitle.trim()
        val cleanDesc = ideaDescription.trim()
        val cleanRef = paymentReference.trim()

        val existing = dao.findExistingRegistration(normEmail, normPhone)
        if (existing != null) {
            return Result.failure(
                DuplicateContestantException(
                    "በዚህ ስልክ ቁጥር ወይም ኢሜል ቀድሞ ምዝገባ ተካሂዷል። / A registration with this phone number or email already exists."
                )
            )
        }

        val entity = RegistrationEntity(
            fullName = cleanName,
            phoneNumber = cleanPhone,
            normalizedPhone = normPhone,
            email = cleanEmail,
            normalizedEmail = normEmail,
            ideaTitle = cleanTitle,
            categoryKey = category.name,
            ideaDescription = cleanDesc,
            projectFileUri = projectFile.uriString,
            projectFileName = projectFile.displayName,
            projectFileMime = projectFile.mimeType,
            projectFileSizeBytes = projectFile.sizeBytes,
            paymentChannelKey = paymentChannel.name,
            paymentReference = cleanRef,
            receiptFileUri = receiptFile.uriString,
            receiptFileName = receiptFile.displayName,
            receiptFileMime = receiptFile.mimeType,
            receiptFileSizeBytes = receiptFile.sizeBytes,
            statusKey = RegistrationStatus.PENDING.name,
            submittedAtMillis = System.currentTimeMillis()
        )
        val insertedId = dao.insertRegistration(entity)
        return Result.success(
            SubmissionConfirmation(
                registrationId = insertedId,
                fullName = cleanName,
                phoneNumber = cleanPhone,
                email = cleanEmail,
                ideaTitle = cleanTitle,
                category = category,
                projectFileName = projectFile.displayName,
                paymentChannel = paymentChannel,
                paymentReference = cleanRef,
                receiptFileName = receiptFile.displayName
            )
        )
    }

    suspend fun updateRegistrationStatus(id: Long, status: RegistrationStatus) {
        dao.updateRegistrationStatus(
            id = id,
            statusKey = status.name,
            reviewedAtMillis = System.currentTimeMillis()
        )
    }

    suspend fun savePaymentAccountsConfig(config: PaymentAccountsConfig) {
        preferences.savePaymentAccountsConfig(config)
    }

    suspend fun saveCompetitionStagesConfig(config: CompetitionStagesConfig) {
        preferences.saveCompetitionStagesConfig(config)
    }

    suspend fun castVote(registrationId: Long, rawIdentifierInput: String): VoteOutcome {
        val stages = preferences.getCurrentStagesConfig()
        if (!stages.votingOpen) {
            return VoteOutcome.VotingClosed
        }

        val currentLock = preferences.getCurrentVoteLock()
        if (currentLock.hasVotedOnDevice) {
            return VoteOutcome.DeviceAlreadyLocked(currentLock.votedIdentifierMasked)
        }

        val nominee = dao.getRegistrationById(registrationId)
        if (nominee == null || nominee.statusKey != RegistrationStatus.APPROVED.name) {
            return VoteOutcome.NomineeNotApproved
        }

        val trimmed = rawIdentifierInput.trim()
        val parsed = parseVoterIdentifier(trimmed)
            ?: return VoteOutcome.InvalidIdentifier(
                reasonAmharic = "እባክዎ ትክክለኛ የኢሜል አድራሻ ወይም የስልክ ቁጥር ያስገቡ።",
                reasonEnglish = "Please enter a valid email address or Ethiopian phone number."
            )

        val existingVote = dao.findVoteByIdentifier(parsed.normalized)
        if (existingVote != null) {
            return VoteOutcome.IdentifierAlreadyVoted(trimmed)
        }

        val now = System.currentTimeMillis()
        return try {
            dao.insertVote(
                VoteEntity(
                    registrationId = registrationId,
                    rawIdentifier = trimmed,
                    normalizedIdentifier = parsed.normalized,
                    identifierType = parsed.type,
                    votedAtMillis = now
                )
            )
            preferences.lockDeviceVote(
                registrationId = registrationId,
                maskedIdentifier = parsed.masked,
                votedAtMillis = now
            )
            VoteOutcome.Success(
                nomineeTitle = nominee.ideaTitle,
                maskedIdentifier = parsed.masked
            )
        } catch (_: Exception) {
            VoteOutcome.IdentifierAlreadyVoted(trimmed)
        }
    }

    companion object {
        fun buildCsvReport(
            registrations: List<RegistrationEntity>,
            votes: List<VoteEntity>
        ): String {
            val counts = votes.groupingBy { it.registrationId }.eachCount()
            val sb = StringBuilder()
            sb.append('\uFEFF') // UTF-8 BOM for Excel Amharic compatibility
            sb.appendLine("ID,Full Name,Phone,Email,Business Idea Title,Category,Status,Payment Channel,Payment Reference,Project File,Receipt File,Votes")
            registrations.forEach { reg ->
                val cat = IdeaCategory.fromKey(reg.categoryKey)
                val status = RegistrationStatus.fromKey(reg.statusKey)
                val channel = PaymentChannel.fromKey(reg.paymentChannelKey)
                val voteCount = counts[reg.id] ?: 0
                val row = listOf(
                    "#CFG-${reg.id}",
                    reg.fullName,
                    reg.phoneNumber,
                    reg.email,
                    reg.ideaTitle,
                    "${cat.amharicLabel} (${cat.englishLabel})",
                    status.englishLabel,
                    channel.englishLabel,
                    reg.paymentReference,
                    reg.projectFileName,
                    reg.receiptFileName,
                    voteCount.toString()
                ).joinToString(",") { escapeCsv(it) }
                sb.appendLine(row)
            }
            return sb.toString()
        }

        fun buildShareSummary(
            registrations: List<RegistrationEntity>,
            votes: List<VoteEntity>
        ): String {
            val counts = votes.groupingBy { it.registrationId }.eachCount()
            val approved = registrations.filter { it.statusKey == RegistrationStatus.APPROVED.name }
                .sortedByDescending { counts[it.id] ?: 0 }
            val sb = StringBuilder()
            sb.appendLine("Call for Growth — Competition Report")
            sb.appendLine("Total Registrations: ${registrations.size} | Approved: ${approved.size} | Total Votes: ${votes.size}")
            sb.appendLine()
            if (approved.isNotEmpty()) {
                sb.appendLine("Approved Standings:")
                approved.forEachIndexed { idx, reg ->
                    sb.appendLine("${idx + 1}. ${reg.ideaTitle} (${reg.fullName}) — ${counts[reg.id] ?: 0} votes")
                }
            }
            return sb.toString()
        }

        private fun escapeCsv(value: String): String {
            val sanitized = value.replace("\"", "\"\"")
            return "\"$sanitized\""
        }

        fun isValidEmail(value: String): Boolean {
            val trimmed = value.trim()
            return trimmed.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
        }

        fun isValidPhone(value: String): Boolean {
            val digits = value.filter { it.isDigit() }
            if (digits.length < 9 || digits.length > 15) return false
            // Accept Ethiopian 09.../07.../2519.../2517... or general 9-15 digit phone
            return when {
                digits.startsWith("09") || digits.startsWith("07") -> digits.length == 10
                digits.startsWith("251") -> digits.length == 12
                else -> digits.length in 9..15
            }
        }

        fun normalizeEmail(value: String): String = value.trim().lowercase()

        fun normalizePhone(value: String): String {
            val digits = value.filter { it.isDigit() }
            return when {
                digits.startsWith("251") && digits.length == 12 -> digits
                (digits.startsWith("09") || digits.startsWith("07")) && digits.length == 10 ->
                    "251" + digits.substring(1)
                (digits.startsWith("9") || digits.startsWith("7")) && digits.length == 9 ->
                    "251$digits"
                else -> digits
            }
        }

        data class ParsedIdentifier(
            val normalized: String,
            val type: String,
            val masked: String
        )

        fun parseVoterIdentifier(raw: String): ParsedIdentifier? {
            val trimmed = raw.trim()
            if (isValidEmail(trimmed)) {
                val norm = "email:${normalizeEmail(trimmed)}"
                val parts = trimmed.split("@")
                val local = parts.firstOrNull().orEmpty()
                val domain = parts.getOrNull(1).orEmpty()
                val maskedLocal = if (local.length <= 2) "${local.take(1)}***" else "${local.take(2)}***"
                return ParsedIdentifier(norm, "EMAIL", "$maskedLocal@$domain")
            }
            if (isValidPhone(trimmed)) {
                val normPhone = normalizePhone(trimmed)
                val masked = if (normPhone.length >= 6) {
                    "+${normPhone.take(4)}****${normPhone.takeLast(3)}"
                } else {
                    "***${normPhone.takeLast(2)}"
                }
                return ParsedIdentifier("phone:$normPhone", "PHONE", masked)
            }
            return null
        }
    }
}

class DuplicateContestantException(message: String) : Exception(message)
