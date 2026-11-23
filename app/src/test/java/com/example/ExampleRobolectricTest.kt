package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.CompetitionPreferences
import com.example.data.CompetitionRepository
import com.example.data.CompetitionStagesConfig
import com.example.data.IdeaCategory
import com.example.data.PaymentChannel
import com.example.data.PickedFileInfo
import com.example.data.RegistrationStatus
import com.example.data.VoteOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: CompetitionRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val prefs = CompetitionPreferences(context)
        runBlocking { prefs.resetAllForTesting() }
        repository = CompetitionRepository(db.competitionDao(), prefs)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `app name matches Call for Growth`() {
        assertEquals("Call for Growth", context.getString(R.string.app_name))
    }

    @Test
    fun `ethiopian phone formats normalize identically`() {
        val local = CompetitionRepository.normalizePhone("0911223344")
        val intl = CompetitionRepository.normalizePhone("+251 911-223-344")
        assertEquals("251911223344", local)
        assertEquals(local, intl)
    }

    @Test
    fun `registration approval and strict one vote per person work end to end`() = runTest {
        val projectFile = PickedFileInfo("content://test/deck.pdf", "deck.pdf", "application/pdf", 12000L)
        val receiptFile = PickedFileInfo("content://test/receipt.jpg", "receipt.jpg", "image/jpeg", 4500L)

        val regResult = repository.submitRegistration(
            fullName = "Abebe Bikila",
            phoneNumber = "0911223344",
            email = "abebe@example.com",
            ideaTitle = "Smart Teff Milling",
            category = IdeaCategory.AGRI_FOOD,
            ideaDescription = "Solar powered community grain milling and packaging platform.",
            projectFile = projectFile,
            paymentChannel = PaymentChannel.TELEBIRR,
            paymentReference = "TXN998877",
            receiptFile = receiptFile
        )
        assertTrue(regResult.isSuccess)
        val regId = regResult.getOrThrow().registrationId

        // Duplicate phone/email registration should be rejected
        val dupResult = repository.submitRegistration(
            fullName = "Abebe Duplicate",
            phoneNumber = "+251911223344",
            email = "other@example.com",
            ideaTitle = "Another Idea",
            category = IdeaCategory.TECH_DIGITAL,
            ideaDescription = "Testing duplicate phone check across formats.",
            projectFile = projectFile,
            paymentChannel = PaymentChannel.CBE,
            paymentReference = "TXN000111",
            receiptFile = receiptFile
        )
        assertTrue(dupResult.isFailure)

        // Voting before organizer approval is blocked
        val unapprovedVote = repository.castVote(regId, "voter@example.com")
        assertTrue(unapprovedVote is VoteOutcome.NomineeNotApproved)

        // Organizer promotes entry to public voting
        repository.updateRegistrationStatus(regId, RegistrationStatus.APPROVED)
        val nominees = repository.approvedNomineesWithVotes.first()
        assertEquals(1, nominees.size)

        // First valid vote succeeds and locks device
        val firstVote = repository.castVote(regId, "0912345678")
        assertTrue(firstVote is VoteOutcome.Success)

        // Second vote attempt from same device (even with a different identifier) is blocked
        val secondVote = repository.castVote(regId, "another_person@example.com")
        assertTrue(secondVote is VoteOutcome.DeviceAlreadyLocked)

        val lock = repository.deviceVoteLock.first()
        assertTrue(lock.hasVotedOnDevice)
        assertEquals(regId, lock.votedRegistrationId)
    }

    @Test
    fun `payment config defaults match exact Telebirr and CBE details`() = runTest {
        val config = repository.paymentAccountsConfig.first()
        assertEquals("251934188825", config.telebirrNumber)
        assertEquals("amanuel", config.telebirrAccountName)
        assertEquals("1000294899883", config.bankAccountNumber)
        assertEquals("CBE", config.bankAccountName)
        assertEquals("1,000 ETB (1,000 ብር)", config.registrationFeeLabel)
    }

    @Test
    fun `admin panel stays locked until correct PIN is entered and locks again`() {
        val vm = com.example.ui.CompetitionViewModel(repository)
        assertEquals(false, vm.adminState.value.isUnlocked)

        // Wrong PIN fails
        vm.onAdminPinInputChange("000000")
        vm.unlockAdmin()
        assertEquals(false, vm.adminState.value.isUnlocked)
        assertTrue(vm.adminState.value.pinError != null)

        // Correct PIN unlocks
        vm.onAdminPinInputChange("198729")
        vm.unlockAdmin()
        assertTrue(vm.adminState.value.isUnlocked)
        assertEquals(null, vm.adminState.value.pinError)

        // Lock / logout re-locks immediately
        vm.lockAdmin()
        assertEquals(false, vm.adminState.value.isUnlocked)
    }

    @Test
    fun `only approved contestants appear in public voting list`() = runTest {
        val projectFile = PickedFileInfo("content://test/deck.pdf", "deck.pdf", "application/pdf", 8000L)
        val receiptFile = PickedFileInfo("content://test/receipt.jpg", "receipt.jpg", "image/jpeg", 3000L)

        val pendingId = repository.submitRegistration(
            fullName = "Pending User",
            phoneNumber = "0911000001",
            email = "pending@example.com",
            ideaTitle = "Pending Idea",
            category = IdeaCategory.TECH_DIGITAL,
            ideaDescription = "Pending idea waiting for admin approval.",
            projectFile = projectFile,
            paymentChannel = PaymentChannel.TELEBIRR,
            paymentReference = "REF001",
            receiptFile = receiptFile
        ).getOrThrow().registrationId

        val approvedId = repository.submitRegistration(
            fullName = "Approved User",
            phoneNumber = "0911000002",
            email = "approved@example.com",
            ideaTitle = "Approved Idea",
            category = IdeaCategory.TRADE_SERVICES,
            ideaDescription = "Approved idea eligible for public voting.",
            projectFile = projectFile,
            paymentChannel = PaymentChannel.CBE,
            paymentReference = "REF002",
            receiptFile = receiptFile
        ).getOrThrow().registrationId

        // Initially neither appears
        assertEquals(0, repository.approvedNomineesWithVotes.first().size)

        // Approve second, reject first
        repository.updateRegistrationStatus(approvedId, RegistrationStatus.APPROVED)
        repository.updateRegistrationStatus(pendingId, RegistrationStatus.REJECTED)

        val visibleNominees = repository.approvedNomineesWithVotes.first()
        assertEquals(1, visibleNominees.size)
        assertEquals(approvedId, visibleNominees.first().registration.id)

        // Rejecting removes from public voting list immediately
        repository.updateRegistrationStatus(approvedId, RegistrationStatus.REJECTED)
        assertEquals(0, repository.approvedNomineesWithVotes.first().size)
    }

    @Test
    fun `closing registration and voting stages enforces stage gating`() = runTest {
        val projectFile = PickedFileInfo("content://test/deck.pdf", "deck.pdf", "application/pdf", 8000L)
        val receiptFile = PickedFileInfo("content://test/receipt.jpg", "receipt.jpg", "image/jpeg", 3000L)

        // First register and approve while open
        val regId = repository.submitRegistration(
            fullName = "Stage Test Founder",
            phoneNumber = "0911555666",
            email = "stage@example.com",
            ideaTitle = "Clean Water Sensors",
            category = IdeaCategory.TECH_DIGITAL,
            ideaDescription = "Solar IoT water quality sensors for rural communities.",
            projectFile = projectFile,
            paymentChannel = PaymentChannel.TELEBIRR,
            paymentReference = "REF_STAGE_1",
            receiptFile = receiptFile
        ).getOrThrow().registrationId
        repository.updateRegistrationStatus(regId, RegistrationStatus.APPROVED)

        // Close registration & voting
        repository.saveCompetitionStagesConfig(
            CompetitionStagesConfig(
                registrationOpen = false,
                votingOpen = false,
                winnersPublished = true
            )
        )

        val closedRegResult = repository.submitRegistration(
            fullName = "Late Entry",
            phoneNumber = "0911777888",
            email = "late@example.com",
            ideaTitle = "Late Business Idea",
            category = IdeaCategory.TRADE_SERVICES,
            ideaDescription = "Submitted after organizer closed registration window.",
            projectFile = projectFile,
            paymentChannel = PaymentChannel.CBE,
            paymentReference = "REF_LATE",
            receiptFile = receiptFile
        )
        assertTrue(closedRegResult.isFailure)

        val closedVoteOutcome = repository.castVote(regId, "voter1@example.com")
        assertTrue(closedVoteOutcome is VoteOutcome.VotingClosed)
    }

    @Test
    fun `csv report and share summary include BOM headers payment ref and vote count`() = runTest {
        val projectFile = PickedFileInfo("content://test/pitch.pdf", "pitch.pdf", "application/pdf", 9500L)
        val receiptFile = PickedFileInfo("content://test/receipt.png", "receipt.png", "image/png", 4200L)

        val regId = repository.submitRegistration(
            fullName = "Selamawit \"Selam\" Tadesse",
            phoneNumber = "0922334455",
            email = "selam@example.com",
            ideaTitle = "EthioCraft Export Hub",
            category = IdeaCategory.CREATIVE_CULTURE,
            ideaDescription = "Connecting artisan weavers directly to international buyers.",
            projectFile = projectFile,
            paymentChannel = PaymentChannel.CBE,
            paymentReference = "FT_CSV_991",
            receiptFile = receiptFile
        ).getOrThrow().registrationId
        repository.updateRegistrationStatus(regId, RegistrationStatus.APPROVED)
        assertTrue(repository.castVote(regId, "fan@example.com") is VoteOutcome.Success)

        val registrations = repository.allRegistrations.first()
        val votes = repository.allVotes.first()

        val csv = CompetitionRepository.buildCsvReport(registrations, votes)
        assertTrue(csv.startsWith("\uFEFFID,Full Name,Phone,Email"))
        assertTrue(csv.contains("\"Selamawit \"\"Selam\"\" Tadesse\""))
        assertTrue(csv.contains("\"FT_CSV_991\""))
        assertTrue(csv.contains("\"1\""))

        val summary = CompetitionRepository.buildShareSummary(registrations, votes)
        assertTrue(summary.contains("Call for Growth"))
        assertTrue(summary.contains("EthioCraft Export Hub"))
        assertTrue(summary.contains("1 votes"))
    }
}
