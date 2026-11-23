package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AppDatabase
import com.example.data.CompetitionPreferences
import com.example.data.CompetitionRepository
import com.example.data.CompetitionStagesConfig
import com.example.data.DeviceVoteLock
import com.example.data.IdeaCategory
import com.example.data.NomineeWithVotes
import com.example.data.PaymentAccountsConfig
import com.example.data.PaymentChannel
import com.example.data.PickedFileInfo
import com.example.data.RegistrationEntity
import com.example.data.RegistrationStatus
import com.example.data.SubmissionConfirmation
import com.example.data.VoteEntity
import com.example.data.VoteOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest

enum class AppTab {
    REGISTER,
    VOTE,
    ADMIN
}

data class RegistrationFormState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val ideaTitle: String = "",
    val selectedCategory: IdeaCategory = IdeaCategory.TECH_DIGITAL,
    val ideaDescription: String = "",
    val projectFile: PickedFileInfo? = null,
    val selectedPaymentChannel: PaymentChannel = PaymentChannel.TELEBIRR,
    val paymentReference: String = "",
    val receiptFile: PickedFileInfo? = null,
    val fullNameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val ideaTitleError: String? = null,
    val ideaDescriptionError: String? = null,
    val projectFileError: String? = null,
    val paymentReferenceError: String? = null,
    val receiptFileError: String? = null,
    val generalError: String? = null,
    val isSubmitting: Boolean = false,
    val confirmation: SubmissionConfirmation? = null
)

data class VotingUiState(
    val voterIdentifier: String = "",
    val identifierError: String? = null,
    val pendingNomineeToConfirm: NomineeWithVotes? = null,
    val feedbackBanner: String? = null,
    val feedbackIsError: Boolean = false,
    val isSubmittingVote: Boolean = false
)

enum class AdminFilter {
    ALL,
    PENDING,
    APPROVED,
    REJECTED
}

data class AdminUiState(
    val isUnlocked: Boolean = false,
    val pinInput: String = "",
    val pinError: String? = null,
    val filter: AdminFilter = AdminFilter.ALL,
    val statusSnackbar: String? = null
)

class CompetitionViewModel(
    private val repository: CompetitionRepository
) : ViewModel() {

    private val _currentTab = MutableStateFlow(AppTab.REGISTER)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _formState = MutableStateFlow(RegistrationFormState())
    val formState: StateFlow<RegistrationFormState> = _formState.asStateFlow()

    private val _votingState = MutableStateFlow(VotingUiState())
    val votingState: StateFlow<VotingUiState> = _votingState.asStateFlow()

    private val _adminState = MutableStateFlow(AdminUiState())
    val adminState: StateFlow<AdminUiState> = _adminState.asStateFlow()

    val allRegistrations: StateFlow<List<RegistrationEntity>> = repository.allRegistrations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allVotes: StateFlow<List<VoteEntity>> = repository.allVotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val approvedNominees: StateFlow<List<NomineeWithVotes>> = repository.approvedNomineesWithVotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val totalVotesCast: StateFlow<Int> = repository.totalVotesCast
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0)

    val deviceVoteLock: StateFlow<DeviceVoteLock> = repository.deviceVoteLock
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DeviceVoteLock())

    val paymentAccountsConfig: StateFlow<PaymentAccountsConfig> = repository.paymentAccountsConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PaymentAccountsConfig())

    val stagesConfig: StateFlow<CompetitionStagesConfig> = repository.competitionStagesConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CompetitionStagesConfig())

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
    }

    // --- Registration Form Updates ---
    fun onFullNameChange(value: String) = _formState.update {
        it.copy(fullName = value, fullNameError = null, generalError = null)
    }

    fun onPhoneChange(value: String) = _formState.update {
        it.copy(phoneNumber = value, phoneError = null, generalError = null)
    }

    fun onEmailChange(value: String) = _formState.update {
        it.copy(email = value, emailError = null, generalError = null)
    }

    fun onIdeaTitleChange(value: String) = _formState.update {
        it.copy(ideaTitle = value, ideaTitleError = null, generalError = null)
    }

    fun onCategorySelect(category: IdeaCategory) = _formState.update {
        it.copy(selectedCategory = category)
    }

    fun onIdeaDescriptionChange(value: String) = _formState.update {
        it.copy(ideaDescription = value, ideaDescriptionError = null, generalError = null)
    }

    fun onProjectFileSelected(file: PickedFileInfo) = _formState.update {
        it.copy(projectFile = file, projectFileError = null, generalError = null)
    }

    fun clearProjectFile() = _formState.update {
        it.copy(projectFile = null)
    }

    fun onPaymentChannelSelect(channel: PaymentChannel) = _formState.update {
        it.copy(selectedPaymentChannel = channel)
    }

    fun onPaymentReferenceChange(value: String) = _formState.update {
        it.copy(paymentReference = value, paymentReferenceError = null, generalError = null)
    }

    fun onReceiptFileSelected(file: PickedFileInfo) = _formState.update {
        it.copy(receiptFile = file, receiptFileError = null, generalError = null)
    }

    fun clearReceiptFile() = _formState.update {
        it.copy(receiptFile = null)
    }

    fun dismissConfirmation() = _formState.update {
        it.copy(confirmation = null)
    }

    fun submitRegistration() {
        val state = _formState.value
        if (state.isSubmitting) return

        val nameErr = if (state.fullName.trim().length < 3) {
            "እባክዎ ሙሉ ስምዎን ያስገቡ (ቢያንስ 3 ፊደላት) / Please enter your full name."
        } else null

        val phoneErr = if (!CompetitionRepository.isValidPhone(state.phoneNumber)) {
            "እባክዎ ትክክለኛ የስልክ ቁጥር ያስገቡ (ምሳሌ፡ 0911223344) / Enter a valid phone number."
        } else null

        val emailErr = if (!CompetitionRepository.isValidEmail(state.email)) {
            "እባክዎ ትክክለኛ የኢሜል አድራሻ ያስገቡ / Please enter a valid email address."
        } else null

        val titleErr = if (state.ideaTitle.trim().length < 3) {
            "የቢዝነስ አይዲያ ርዕስ ያስገቡ / Please enter your business idea title."
        } else null

        val descErr = if (state.ideaDescription.trim().length < 20) {
            "እባክዎ ዝርዝር መግለጫ (ቢያንስ 20 ፊደላት) ያስገቡ / Please describe your idea (at least 20 characters)."
        } else null

        val projectErr = if (state.projectFile == null) {
            "የፕሮጀክት አታችመንት (PDF, DOC, DOCX, PPT, PNG, JPG) ያያይዙ / Please attach your project file."
        } else null

        val refErr = if (state.paymentReference.trim().length < 4) {
            "የ1,000 ብር ክፍያ ሪፈራንስ ነበር (Transaction ID) ያስገቡ / Payment reference ID is required."
        } else null

        val receiptErr = if (state.receiptFile == null) {
            "የክፍያ ሪሲት አታችመንት ያያይዙ / Please attach your payment receipt."
        } else null

        val hasErrors = listOf(
            nameErr, phoneErr, emailErr, titleErr, descErr, projectErr, refErr, receiptErr
        ).any { it != null }

        if (hasErrors) {
            _formState.update {
                it.copy(
                    fullNameError = nameErr,
                    phoneError = phoneErr,
                    emailError = emailErr,
                    ideaTitleError = titleErr,
                    ideaDescriptionError = descErr,
                    projectFileError = projectErr,
                    paymentReferenceError = refErr,
                    receiptFileError = receiptErr,
                    generalError = "እባክዎ ሁሉንም አስፈላጊ መረጃዎች በትክክል ይሙሉ። / Please complete all required fields."
                )
            }
            return
        }

        _formState.update { it.copy(isSubmitting = true, generalError = null) }
        viewModelScope.launch {
            val result = repository.submitRegistration(
                fullName = state.fullName,
                phoneNumber = state.phoneNumber,
                email = state.email,
                ideaTitle = state.ideaTitle,
                category = state.selectedCategory,
                ideaDescription = state.ideaDescription,
                projectFile = state.projectFile!!,
                paymentChannel = state.selectedPaymentChannel,
                paymentReference = state.paymentReference,
                receiptFile = state.receiptFile!!
            )
            result.onSuccess { confirmation ->
                _formState.value = RegistrationFormState(
                    confirmation = confirmation
                )
            }.onFailure { error ->
                _formState.update {
                    it.copy(
                        isSubmitting = false,
                        generalError = error.message ?: "ምዝገባው አልተሳካም። እባክዎ እንደገና ይሞክሩ።"
                    )
                }
            }
        }
    }

    // --- Voting Actions ---
    fun onVoterIdentifierChange(value: String) = _votingState.update {
        it.copy(voterIdentifier = value, identifierError = null, feedbackBanner = null)
    }

    fun requestVoteConfirmation(nominee: NomineeWithVotes) {
        val lock = deviceVoteLock.value
        if (lock.hasVotedOnDevice) {
            _votingState.update {
                it.copy(
                    feedbackBanner = "ድምጽዎ በተሳካ ሁኔታ ተመዝግቧል - አንድ ጊዜ ብቻ (${lock.votedIdentifierMasked})",
                    feedbackIsError = true
                )
            }
            return
        }

        val input = _votingState.value.voterIdentifier.trim()
        if (CompetitionRepository.parseVoterIdentifier(input) == null) {
            _votingState.update {
                it.copy(
                    identifierError = "ድምጽ ከመስጠትዎ በፊት ትክክለኛ ኢሜል ወይም ስልክ ቁጥር ያስገቡ / Enter a valid email or phone number first."
                )
            }
            return
        }

        _votingState.update {
            it.copy(pendingNomineeToConfirm = nominee, identifierError = null, feedbackBanner = null)
        }
    }

    fun dismissVoteConfirmation() = _votingState.update {
        it.copy(pendingNomineeToConfirm = null)
    }

    fun confirmAndCastVote() {
        val nominee = _votingState.value.pendingNomineeToConfirm ?: return
        val identifier = _votingState.value.voterIdentifier
        _votingState.update { it.copy(isSubmittingVote = true, pendingNomineeToConfirm = null) }

        viewModelScope.launch {
            when (val outcome = repository.castVote(nominee.registration.id, identifier)) {
                is VoteOutcome.Success -> {
                    _votingState.update {
                        it.copy(
                            isSubmittingVote = false,
                            feedbackBanner = "ድምጽዎ በተሳካ ሁኔታ ተመዝግቧል - አንድ ጊዜ ብቻ • ${outcome.nomineeTitle}",
                            feedbackIsError = false
                        )
                    }
                }
                is VoteOutcome.DeviceAlreadyLocked -> {
                    _votingState.update {
                        it.copy(
                            isSubmittingVote = false,
                            feedbackBanner = "ይህ ስልክ/መሣሪያ ቀድሞ ድምጽ ሰጥቷል (${outcome.maskedIdentifier})። በአንድ ሰው አንድ ድምጽ ብቻ ነው!",
                            feedbackIsError = true
                        )
                    }
                }
                is VoteOutcome.IdentifierAlreadyVoted -> {
                    _votingState.update {
                        it.copy(
                            isSubmittingVote = false,
                            identifierError = "ይህ ኢሜል ወይም ስልክ ቁጥር ቀድሞ ድምጽ ሰጥቷል / Duplicate vote blocked for this identifier.",
                            feedbackBanner = "በዚህ መለያ (${outcome.identifier}) ቀድሞ ድምጽ ተሰጥቷል። ድጋሚ ድምጽ መስጠት አይቻልም!",
                            feedbackIsError = true
                        )
                    }
                }
                is VoteOutcome.InvalidIdentifier -> {
                    _votingState.update {
                        it.copy(
                            isSubmittingVote = false,
                            identifierError = "${outcome.reasonAmharic} / ${outcome.reasonEnglish}"
                        )
                    }
                }
                VoteOutcome.NomineeNotApproved -> {
                    _votingState.update {
                        it.copy(
                            isSubmittingVote = false,
                            feedbackBanner = "ይህ እጩ ለህዝብ ድምጽ አልጸደቀም። / Nominee is not approved for voting.",
                            feedbackIsError = true
                        )
                    }
                }
                VoteOutcome.VotingClosed -> {
                    _votingState.update {
                        it.copy(
                            isSubmittingVote = false,
                            feedbackBanner = "የድምጽ መስጫ ጊዜ በአስተዳዳሪው ተዘግቷል። / Public voting is currently closed.",
                            feedbackIsError = true
                        )
                    }
                }
            }
        }
    }

    // --- Organizer / Admin Actions ---
    fun onAdminPinInputChange(value: String) = _adminState.update {
        it.copy(pinInput = value, pinError = null)
    }

    fun unlockAdmin() {
        val candidate = _adminState.value.pinInput.trim()
        if (candidate.isEmpty()) {
            _adminState.update {
                it.copy(pinError = "እባክዎ የአስተዳዳሪ የይለፍ ቃል (PIN) ያስገቡ / Please enter the Admin PIN.")
            }
            return
        }
        if (sha256Hex(candidate) == ADMIN_PIN_SHA256) {
            _adminState.update {
                it.copy(
                    isUnlocked = true,
                    pinInput = "",
                    pinError = null,
                    statusSnackbar = null
                )
            }
        } else {
            _adminState.update {
                it.copy(
                    isUnlocked = false,
                    pinError = "የተሳሳተ የይለፍ ቃል / Incorrect Admin Password/PIN."
                )
            }
        }
    }

    fun lockAdmin() {
        _adminState.update {
            it.copy(
                isUnlocked = false,
                pinInput = "",
                pinError = null,
                statusSnackbar = null
            )
        }
    }

    fun selectAdminFilter(filter: AdminFilter) = _adminState.update {
        it.copy(filter = filter)
    }

    fun updateStatus(registrationId: Long, newStatus: RegistrationStatus) {
        if (!_adminState.value.isUnlocked) return
        viewModelScope.launch {
            repository.updateRegistrationStatus(registrationId, newStatus)
            val msg = when (newStatus) {
                RegistrationStatus.APPROVED -> "እጩው ወደ የህዝብ ድምጽ መስጫ ዝርዝር ተዛውሯል። / Promoted to public voting!"
                RegistrationStatus.REJECTED -> "ምዝገባው ውድቅ ተደርጓል። / Registration marked as rejected."
                RegistrationStatus.PENDING -> "ወደ ግምገማ ተመልሷል። / Returned to pending review."
            }
            _adminState.update { it.copy(statusSnackbar = msg) }
        }
    }

    fun updatePaymentAccounts(config: PaymentAccountsConfig) {
        if (!_adminState.value.isUnlocked) return
        viewModelScope.launch {
            repository.savePaymentAccountsConfig(config)
            _adminState.update {
                it.copy(statusSnackbar = "የክፍያ አካውንት መረጃዎች ተዘምነዋል። / Payment account details updated.")
            }
        }
    }

    fun updateCompetitionStages(config: CompetitionStagesConfig) {
        if (!_adminState.value.isUnlocked) return
        viewModelScope.launch {
            repository.saveCompetitionStagesConfig(config)
            _adminState.update {
                it.copy(statusSnackbar = "የውድድር ደረጃዎች ተዘምነዋል። / Competition stages updated.")
            }
        }
    }

    fun showAdminNotice(message: String) {
        _adminState.update { it.copy(statusSnackbar = message) }
    }

    fun buildCsvReport(): String =
        CompetitionRepository.buildCsvReport(allRegistrations.value, allVotes.value)

    fun buildShareSummary(): String =
        CompetitionRepository.buildShareSummary(allRegistrations.value, allVotes.value)

    fun clearAdminMessage() = _adminState.update { it.copy(statusSnackbar = null) }

    companion object {
        // SHA-256 hash of Admin PIN
        private const val ADMIN_PIN_SHA256 =
            "c05b6066a03a2bcae8dd0d604ca5e47676511036c30d58fa284bed6e883bb5eb"

        private fun sha256Hex(input: String): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it) }
        }

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val appContext = context.applicationContext
                val db = AppDatabase.getInstance(appContext)
                val prefs = CompetitionPreferences(appContext)
                val repo = CompetitionRepository(db.competitionDao(), prefs)
                CompetitionViewModel(repo)
            }
        }
    }
}
