package com.example.data

enum class IdeaCategory(
    val amharicLabel: String,
    val englishLabel: String
) {
    TECH_DIGITAL("ቴክኖሎጂ እና ዲጂታል", "Technology & Digital"),
    AGRI_FOOD("ግብርና እና ምግብ ዝግጅት", "Agriculture & Food Processing"),
    TRADE_SERVICES("ንግድ እና አገልግሎት", "Trade & Services"),
    CREATIVE_CULTURE("ፈጠራ እና ባህል", "Creativity & Culture");

    companion object {
        fun fromKey(key: String): IdeaCategory =
            entries.firstOrNull { it.name == key } ?: TECH_DIGITAL
    }
}

enum class PaymentChannel(
    val amharicLabel: String,
    val englishLabel: String
) {
    TELEBIRR("ቴሌብር", "Telebirr"),
    CBE("የኢትዮጵያ ንግድ ባንክ", "CBE"),
    ABYSSINIA("አቢሲኒያ ባንክ", "Bank of Abyssinia"),
    OTHER_BANK("ሌላ ባንክ", "Other Bank");

    companion object {
        fun fromKey(key: String): PaymentChannel =
            entries.firstOrNull { it.name == key } ?: TELEBIRR
    }
}

enum class RegistrationStatus(
    val amharicLabel: String,
    val englishLabel: String
) {
    PENDING("በግምገማ ላይ", "Pending Review"),
    APPROVED("ለህዝብ ድምጽ የጸደቀ", "Approved for Voting"),
    REJECTED("ውድቅ የተደረገ", "Rejected");

    companion object {
        fun fromKey(key: String): RegistrationStatus =
            entries.firstOrNull { it.name == key } ?: PENDING
    }
}

data class PickedFileInfo(
    val uriString: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
) {
    val isImage: Boolean
        get() = mimeType.startsWith("image/", ignoreCase = true) ||
            displayName.endsWith(".png", ignoreCase = true) ||
            displayName.endsWith(".jpg", ignoreCase = true) ||
            displayName.endsWith(".jpeg", ignoreCase = true)

    val extensionBadge: String
        get() {
            val dotIdx = displayName.lastIndexOf('.')
            if (dotIdx in 1 until displayName.length - 1) {
                return displayName.substring(dotIdx + 1).uppercase().take(4)
            }
            return when {
                mimeType.contains("pdf", true) -> "PDF"
                mimeType.contains("word", true) || mimeType.contains("document", true) -> "DOC"
                mimeType.contains("presentation", true) || mimeType.contains("powerpoint", true) -> "PPT"
                mimeType.startsWith("image/", true) -> "IMG"
                else -> "FILE"
            }
        }
}

data class PaymentAccountsConfig(
    val registrationFeeLabel: String = "1,000 ETB (1,000 ብር)",
    val telebirrNumber: String = "251934188825",
    val telebirrAccountName: String = "amanuel",
    val bankAccountNumber: String = "1000294899883",
    val bankAccountName: String = "CBE",
    val otherBankNote: String = ""
)

data class DeviceVoteLock(
    val hasVotedOnDevice: Boolean = false,
    val votedRegistrationId: Long = -1L,
    val votedIdentifierMasked: String = "",
    val votedAtMillis: Long = 0L
)

data class NomineeWithVotes(
    val registration: RegistrationEntity,
    val voteCount: Int,
    val voteSharePercent: Float
)

data class SubmissionConfirmation(
    val registrationId: Long,
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val ideaTitle: String,
    val category: IdeaCategory,
    val projectFileName: String,
    val paymentChannel: PaymentChannel,
    val paymentReference: String,
    val receiptFileName: String
)

data class CompetitionStagesConfig(
    val registrationOpen: Boolean = true,
    val votingOpen: Boolean = true,
    val winnersPublished: Boolean = false
)

sealed interface VoteOutcome {
    data class Success(val nomineeTitle: String, val maskedIdentifier: String) : VoteOutcome
    data class DeviceAlreadyLocked(val maskedIdentifier: String) : VoteOutcome
    data class IdentifierAlreadyVoted(val identifier: String) : VoteOutcome
    data class InvalidIdentifier(val reasonAmharic: String, val reasonEnglish: String) : VoteOutcome
    data object NomineeNotApproved : VoteOutcome
    data object VotingClosed : VoteOutcome
}
