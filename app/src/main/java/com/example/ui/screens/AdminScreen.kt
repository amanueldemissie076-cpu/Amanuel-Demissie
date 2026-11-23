package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CompetitionStagesConfig
import com.example.data.IdeaCategory
import com.example.data.PaymentAccountsConfig
import com.example.data.PaymentChannel
import com.example.data.PickedFileInfo
import com.example.data.RegistrationEntity
import com.example.data.RegistrationStatus
import com.example.ui.AdminFilter
import com.example.ui.AdminUiState
import com.example.ui.components.AttachmentPreviewDialog
import com.example.ui.components.AttachmentSummaryChip
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.ErrorSoftContainer
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldSoftContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoftContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminScreen(
    registrations: List<RegistrationEntity>,
    adminState: AdminUiState,
    paymentAccountsConfig: PaymentAccountsConfig,
    stagesConfig: CompetitionStagesConfig = CompetitionStagesConfig(),
    onPinInputChange: (String) -> Unit,
    onUnlockAdmin: () -> Unit,
    onLockAdmin: () -> Unit,
    onSelectFilter: (AdminFilter) -> Unit,
    onUpdateStatus: (Long, RegistrationStatus) -> Unit,
    onSavePaymentAccounts: (PaymentAccountsConfig) -> Unit,
    onUpdateStages: (CompetitionStagesConfig) -> Unit = {},
    onBuildCsvReport: () -> String = { "" },
    onBuildShareSummary: () -> String = { "" },
    onShowNotice: (String) -> Unit = {},
    onClearMessage: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!adminState.isUnlocked) {
        AdminPinGateScreen(
            pinInput = adminState.pinInput,
            pinError = adminState.pinError,
            onPinInputChange = onPinInputChange,
            onUnlockAdmin = onUnlockAdmin,
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    var showPaymentEditor by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<PickedFileInfo?>(null) }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(onBuildCsvReport().toByteArray(Charsets.UTF_8))
                }
                onShowNotice("የCSV ሪፖርት በተሳካ ሁኔታ ተቀምጧል። / CSV report saved successfully.")
            } catch (_: Exception) {
                onShowNotice("CSV ፋይል ማስቀመጥ አልተሳካም። / Failed to save CSV report.")
            }
        }
    }

    var telebirrNumberEdit by remember(paymentAccountsConfig) { mutableStateOf(paymentAccountsConfig.telebirrNumber) }
    var telebirrNameEdit by remember(paymentAccountsConfig) { mutableStateOf(paymentAccountsConfig.telebirrAccountName) }
    var bankNumberEdit by remember(paymentAccountsConfig) { mutableStateOf(paymentAccountsConfig.bankAccountNumber) }
    var bankNameEdit by remember(paymentAccountsConfig) { mutableStateOf(paymentAccountsConfig.bankAccountName) }
    var otherNoteEdit by remember(paymentAccountsConfig) { mutableStateOf(paymentAccountsConfig.otherBankNote) }

    LaunchedEffect(adminState.statusSnackbar) {
        if (adminState.statusSnackbar != null) {
            kotlinx.coroutines.delay(3500)
            onClearMessage()
        }
    }

    val filteredRegistrations = remember(registrations, adminState.filter) {
        when (adminState.filter) {
            AdminFilter.ALL -> registrations
            AdminFilter.PENDING -> registrations.filter { it.statusKey == RegistrationStatus.PENDING.name }
            AdminFilter.APPROVED -> registrations.filter { it.statusKey == RegistrationStatus.APPROVED.name }
            AdminFilter.REJECTED -> registrations.filter { it.statusKey == RegistrationStatus.REJECTED.name }
        }
    }

    val pendingCount = registrations.count { it.statusKey == RegistrationStatus.PENDING.name }
    val approvedCount = registrations.count { it.statusKey == RegistrationStatus.APPROVED.name }
    val rejectedCount = registrations.count { it.statusKey == RegistrationStatus.REJECTED.name }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 720.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Lock / Logout action
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldDeep
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoldAccent.copy(alpha = 0.24f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = GoldSoftContainer,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "የአስተዳዳሪ መቆጣጠሪያ ክፍል",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Organizer / Admin Verification & Promotion Panel",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldSoftContainer
                                )
                            }
                            OutlinedButton(
                                onClick = onLockAdmin,
                                border = BorderStroke(1.dp, GoldSoftContainer.copy(alpha = 0.7f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldSoftContainer),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("admin_lock_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(" ቆልፍ / Lock")
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AdminCountChip("Total", registrations.size, Modifier.weight(1f))
                            AdminCountChip("Pending", pendingCount, Modifier.weight(1f))
                            AdminCountChip("Approved", approvedCount, Modifier.weight(1f))
                            AdminCountChip("Rejected", rejectedCount, Modifier.weight(1f))
                        }
                    }
                }

                // Competition Stage Controls
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                            Column {
                                Text(
                                    text = "የውድድር ደረጃዎች • Competition Stages",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Control live registration, voting window, and winner announcement",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider()
                        StageToggleRow(
                            title = "ምዝገባ ክፍት ነው / Registration Open",
                            subtitle = "Allow new contestants to submit ideas and 1,000 ETB receipts",
                            checked = stagesConfig.registrationOpen,
                            onCheckedChange = { open ->
                                onUpdateStages(stagesConfig.copy(registrationOpen = open))
                            },
                            testTag = "stage_registration_switch"
                        )
                        StageToggleRow(
                            title = "የህዝብ ድምጽ ክፍት ነው / Public Voting Open",
                            subtitle = "Allow verified visitors to cast their single vote",
                            checked = stagesConfig.votingOpen,
                            onCheckedChange = { open ->
                                onUpdateStages(stagesConfig.copy(votingOpen = open))
                            },
                            testTag = "stage_voting_switch"
                        )
                        StageToggleRow(
                            title = "አሸናፊዎችን ይፋ አድርግ / Publish Winners Podium",
                            subtitle = "Display Top 3 winners (500K / 200K / 100K ETB) on the Voting tab",
                            checked = stagesConfig.winnersPublished,
                            onCheckedChange = { published ->
                                onUpdateStages(stagesConfig.copy(winnersPublished = published))
                            },
                            testTag = "stage_winners_switch"
                        )
                    }
                }

                // Export & Share Report
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "ሪፖርት አውርድ እና አጋራ • Export & Share Report",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    csvExportLauncher.launch("call_for_growth_report.csv")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("export_csv_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Download,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = " Save CSV File",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = {
                                    val shareText = onBuildShareSummary()
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Call for Growth — Competition Report")
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }
                                    try {
                                        context.startActivity(
                                            Intent.createChooser(sendIntent, "Share Competition Report")
                                        )
                                    } catch (_: Exception) {
                                        onShowNotice("No sharing app available on this device.")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("share_summary_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Share,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = " Share Summary",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Editable Payment Account Settings
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = GoldAccent
                                )
                                Column {
                                    Text(
                                        text = "የ1,000 ብር ክፍያ አካውንቶችን አስተካክል",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Edit Telebirr & Bank Account Details",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            TextButton(onClick = { showPaymentEditor = !showPaymentEditor }) {
                                Text(if (showPaymentEditor) "ዝጋ" else "አስተካክል")
                                Icon(
                                    imageVector = if (showPaymentEditor) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        AnimatedVisibility(visible = showPaymentEditor) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = telebirrNumberEdit,
                                    onValueChange = { telebirrNumberEdit = it },
                                    label = { Text("Telebirr Number") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = telebirrNameEdit,
                                    onValueChange = { telebirrNameEdit = it },
                                    label = { Text("Telebirr Account Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = bankNumberEdit,
                                    onValueChange = { bankNumberEdit = it },
                                    label = { Text("Bank Account Number") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = bankNameEdit,
                                    onValueChange = { bankNameEdit = it },
                                    label = { Text("Bank Account Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = otherNoteEdit,
                                    onValueChange = { otherNoteEdit = it },
                                    label = { Text("Additional Payment Note (Optional)") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        onSavePaymentAccounts(
                                            PaymentAccountsConfig(
                                                registrationFeeLabel = paymentAccountsConfig.registrationFeeLabel,
                                                telebirrNumber = telebirrNumberEdit,
                                                telebirrAccountName = telebirrNameEdit,
                                                bankAccountNumber = bankNumberEdit,
                                                bankAccountName = bankNameEdit,
                                                otherBankNote = otherNoteEdit
                                            )
                                        )
                                        showPaymentEditor = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                ) {
                                    Icon(Icons.Filled.Save, contentDescription = null)
                                    Text("  አካውንቶችን አስቀምጥ / Save Accounts")
                                }
                            }
                        }
                    }
                }

                // Status Update Feedback
                AnimatedVisibility(visible = adminState.statusSnackbar != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = SuccessSoftContainer,
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = adminState.statusSnackbar.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SuccessGreen,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                // Filter Chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = adminState.filter == AdminFilter.ALL,
                        onClick = { onSelectFilter(AdminFilter.ALL) },
                        label = { Text("ሁሉም / All (${registrations.size})") }
                    )
                    FilterChip(
                        selected = adminState.filter == AdminFilter.PENDING,
                        onClick = { onSelectFilter(AdminFilter.PENDING) },
                        label = { Text("በግምገማ ላይ / Pending ($pendingCount)") }
                    )
                    FilterChip(
                        selected = adminState.filter == AdminFilter.APPROVED,
                        onClick = { onSelectFilter(AdminFilter.APPROVED) },
                        label = { Text("የጸደቁ / Approved ($approvedCount)") }
                    )
                    FilterChip(
                        selected = adminState.filter == AdminFilter.REJECTED,
                        onClick = { onSelectFilter(AdminFilter.REJECTED) },
                        label = { Text("ውድቅ / Rejected ($rejectedCount)") }
                    )
                }
            }
        }

        if (filteredRegistrations.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inbox,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "በዚህ ምድብ የተመዘገበ የለም",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Submitted competition entries will appear here with full payment receipts, reference numbers, and project attachments for review.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredRegistrations, key = { it.id }) { registration ->
                AdminRegistrationCard(
                    registration = registration,
                    onPreviewFile = { previewFile = it },
                    onApprove = { onUpdateStatus(registration.id, RegistrationStatus.APPROVED) },
                    onReject = { onUpdateStatus(registration.id, RegistrationStatus.REJECTED) },
                    onResetPending = { onUpdateStatus(registration.id, RegistrationStatus.PENDING) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 720.dp)
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }

    previewFile?.let { file ->
        AttachmentPreviewDialog(
            file = file,
            onDismiss = { previewFile = null }
        )
    }
}

@Composable
private fun AdminPinGateScreen(
    pinInput: String,
    pinError: String?,
    onPinInputChange: (String) -> Unit,
    onUnlockAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = EmeraldDeep,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = GoldSoftContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "የአስተዳዳሪ መግቢያ • Admin Lock",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "ይህ ክፍል ለውድድሩ አስተዳዳሪዎች ብቻ የተዘጋጀ ነው። ምዝገባዎችን ለማየት እና እጩዎችን ለማጽደቅ የአስተዳዳሪ ፒን/የይለፍ ቃል ያስገቡ።",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                OutlinedTextField(
                    value = pinInput,
                    onValueChange = onPinInputChange,
                    label = { Text("የአስተዳዳሪ የይለፍ ቃል / Admin PIN *") },
                    placeholder = { Text("Enter Admin PIN") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (passwordVisible) "Hide PIN" else "Show PIN"
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    isError = pinError != null,
                    supportingText = pinError?.let { err -> { Text(err) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { onUnlockAdmin() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_pin_input")
                )

                Button(
                    onClick = onUnlockAdmin,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("admin_unlock_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.LockOpen,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "  ክፈት • Unlock Admin Panel",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminCountChip(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AdminRegistrationCard(
    registration: RegistrationEntity,
    onPreviewFile: (PickedFileInfo) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onResetPending: () -> Unit,
    modifier: Modifier = Modifier
) {
    val category = IdeaCategory.fromKey(registration.categoryKey)
    val channel = PaymentChannel.fromKey(registration.paymentChannelKey)
    val status = RegistrationStatus.fromKey(registration.statusKey)

    val statusColor = when (status) {
        RegistrationStatus.PENDING -> GoldAccent
        RegistrationStatus.APPROVED -> SuccessGreen
        RegistrationStatus.REJECTED -> ErrorRed
    }
    val statusBg = when (status) {
        RegistrationStatus.PENDING -> GoldSoftContainer
        RegistrationStatus.APPROVED -> SuccessSoftContainer
        RegistrationStatus.REJECTED -> ErrorSoftContainer
    }

    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    val projectFile = PickedFileInfo(
        uriString = registration.projectFileUri,
        displayName = registration.projectFileName,
        mimeType = registration.projectFileMime,
        sizeBytes = registration.projectFileSizeBytes
    )
    val receiptFile = PickedFileInfo(
        uriString = registration.receiptFileUri,
        displayName = registration.receiptFileName,
        mimeType = registration.receiptFileMime,
        sizeBytes = registration.receiptFileSizeBytes
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = statusBg
                ) {
                    Text(
                        text = "${status.amharicLabel} • ${status.englishLabel}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "#CFG-${registration.id} • ${dateFormatter.format(Date(registration.submittedAtMillis))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "የቢዝነስ አይዲያ / Business Idea",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = registration.ideaTitle,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "${category.amharicLabel} (${category.englishLabel})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "ስም / Name: ${registration.fullName}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ስልክ / Phone: ${registration.phoneNumber}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "ኢሜል / Email: ${registration.email}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Text(
                text = registration.ideaDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "የፕሮጀክት አታችመንት / Project Attachment",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                AttachmentSummaryChip(
                    file = projectFile,
                    onPreview = { onPreviewFile(projectFile) }
                )
            }

            HorizontalDivider()

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Payments,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = GoldAccent
                    )
                    Text(
                        text = "የ1,000 ብር ክፍያ መንገድ / Payment Method: ${channel.englishLabel} (${channel.amharicLabel})",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "የክፍያ ሪፈራንስ ነበር / Payment Reference: ${registration.paymentReference}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
                Text(
                    text = "የተጫነ የክፍያ ሪሲት / Uploaded Receipt",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                AttachmentSummaryChip(
                    file = receiptFile,
                    onPreview = { onPreviewFile(receiptFile) }
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (status != RegistrationStatus.APPROVED) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("approve_button_${registration.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = " አጽድቅ (Approve)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (status != RegistrationStatus.REJECTED) {
                        OutlinedButton(
                            onClick = onReject,
                            border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.65f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("reject_button_${registration.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Cancel,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = " ውድቅ አድርግ (Reject)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (status != RegistrationStatus.PENDING) {
                    OutlinedButton(
                        onClick = onResetPending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(" ወደ ግምገማ መልስ (Reset to Pending)")
                    }
                }
            }
        }
    }
}

@Composable
private fun StageToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag)
        )
    }
}
