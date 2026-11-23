package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CompetitionStagesConfig
import com.example.data.IdeaCategory
import com.example.data.PaymentAccountsConfig
import com.example.data.PaymentChannel
import com.example.data.PickedFileInfo
import com.example.data.SubmissionConfirmation
import com.example.ui.RegistrationFormState
import com.example.ui.components.AttachmentPreviewDialog
import com.example.ui.components.AttachmentUploadCard
import com.example.ui.components.FileMetadataResolver
import com.example.ui.components.HeroBannerHeader
import com.example.ui.components.RulesTimelineDialog
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldSoftContainer

private val PROJECT_MIME_TYPES = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "image/png",
    "image/jpeg"
)

private val RECEIPT_DOC_MIME_TYPES = arrayOf(
    "application/pdf",
    "image/png",
    "image/jpeg"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistrationScreen(
    formState: RegistrationFormState,
    paymentAccounts: PaymentAccountsConfig,
    stagesConfig: CompetitionStagesConfig = CompetitionStagesConfig(),
    onFullNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onIdeaTitleChange: (String) -> Unit,
    onCategorySelect: (IdeaCategory) -> Unit,
    onIdeaDescriptionChange: (String) -> Unit,
    onProjectFileSelected: (PickedFileInfo) -> Unit,
    onClearProjectFile: () -> Unit,
    onPaymentChannelSelect: (PaymentChannel) -> Unit,
    onPaymentReferenceChange: (String) -> Unit,
    onReceiptFileSelected: (PickedFileInfo) -> Unit,
    onClearReceiptFile: () -> Unit,
    onSubmit: () -> Unit,
    onDismissConfirmation: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var previewFile by remember { mutableStateOf<PickedFileInfo?>(null) }
    var showRulesDialog by remember { mutableStateOf(false) }

    val projectFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onProjectFileSelected(FileMetadataResolver.resolve(context, it)) }
    }

    val receiptImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onReceiptFileSelected(FileMetadataResolver.resolve(context, it)) }
    }

    val receiptDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { onReceiptFileSelected(FileMetadataResolver.resolve(context, it)) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HeroBannerHeader(
                    registrationOpen = stagesConfig.registrationOpen,
                    onOpenRules = { showRulesDialog = true }
                )

                if (!stagesConfig.registrationOpen) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("registration_closed_banner"),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "ምዝገባው በአሁኑ ጊዜ ተዘግቷል",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = "Registration is currently closed by the organizer. Public voting or results remain visible in the Voting tab.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                // Page Heading
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "ምዝገባ እና የቢዝነስ አይዲያ ማስገቢያ",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Registration & Direct Business Idea Submission",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 1. Contestant Info Section
                SectionCard(
                    icon = Icons.Filled.Person,
                    titleAmharic = "1. የተወዳዳሪ መረጃ",
                    subtitleEnglish = "Contestant Identity (Both Phone & Email Required)"
                ) {
                    Text(
                        text = "ተወዳዳሪዎችን በልዩ ሁኔታ ለመለየት እና ለመገናኘት ስልክ ቁጥር እና ኢሜል ሁለቱም ያስፈልጋሉ።",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = formState.fullName,
                        onValueChange = onFullNameChange,
                        label = { Text("ሙሉ ስም / Full Name *") },
                        placeholder = { Text("ምሳሌ፡ አበበ ከበደ / Abebe Kebede") },
                        leadingIcon = { Icon(Icons.Filled.Badge, contentDescription = null) },
                        isError = formState.fullNameError != null,
                        supportingText = formState.fullNameError?.let { err -> { Text(err) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("full_name_input")
                    )

                    OutlinedTextField(
                        value = formState.phoneNumber,
                        onValueChange = onPhoneChange,
                        label = { Text("ስልክ ቁጥር / Phone Number *") },
                        placeholder = { Text("0911223344 or +251911223344") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        isError = formState.phoneError != null,
                        supportingText = formState.phoneError?.let { err -> { Text(err) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("phone_input")
                    )

                    OutlinedTextField(
                        value = formState.email,
                        onValueChange = onEmailChange,
                        label = { Text("ኢሜል / Email Address *") },
                        placeholder = { Text("example@domain.com") },
                        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                        isError = formState.emailError != null,
                        supportingText = formState.emailError?.let { err -> { Text(err) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input")
                    )
                }

                // 2. Direct Business Idea Submission
                SectionCard(
                    icon = Icons.Filled.Lightbulb,
                    titleAmharic = "2. የቢዝነስ አይዲያ ማስገቢያ",
                    subtitleEnglish = "Direct Business Idea Submission"
                ) {
                    OutlinedTextField(
                        value = formState.ideaTitle,
                        onValueChange = onIdeaTitleChange,
                        label = { Text("የቢዝነስ አይዲያ ርዕስ / Business Idea Title *") },
                        placeholder = { Text("ምሳሌ፡ ዘመናዊ የግብርና ገበያ መተግበሪያ") },
                        isError = formState.ideaTitleError != null,
                        supportingText = formState.ideaTitleError?.let { err -> { Text(err) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("idea_title_input")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "ዘርፍ ይምረጡ / Category *",
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IdeaCategory.entries.forEach { category ->
                                val selected = formState.selectedCategory == category
                                FilterChip(
                                    selected = selected,
                                    onClick = { onCategorySelect(category) },
                                    label = {
                                        Text(
                                            text = "${category.amharicLabel}\n${category.englishLabel}",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    modifier = Modifier.testTag("category_${category.name}")
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formState.ideaDescription,
                        onValueChange = onIdeaDescriptionChange,
                        label = { Text("የቢዝነስ አይዲያ ዝርዝር መግለጫ / Detailed Idea Description *") },
                        placeholder = {
                            Text("ችግሩን፣ መፍትሄውን፣ የገበያ እድሉን እና የሚፈጥረውን ተጽዕኖ በዝርዝር ይጻፉ...")
                        },
                        isError = formState.ideaDescriptionError != null,
                        supportingText = formState.ideaDescriptionError?.let { err -> { Text(err) } },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("idea_description_input")
                    )

                    AttachmentUploadCard(
                        titleAmharic = "የፕሮጀክት አታችመንት / Project Attachment *",
                        subtitleEnglish = "Pitch deck, business proposal, or concept image",
                        allowedHint = "Supported: PDF, DOC, DOCX, PPT, PNG, JPG",
                        selectedFile = formState.projectFile,
                        errorMessage = formState.projectFileError,
                        onPickFile = { projectFileLauncher.launch(PROJECT_MIME_TYPES) },
                        onPreviewFile = { previewFile = it },
                        onClearFile = onClearProjectFile,
                        testTag = "project_attachment_button"
                    )
                }

                // 3. 1,000 ETB Payment & Verification Section
                SectionCard(
                    icon = Icons.Filled.Payments,
                    titleAmharic = "3. የ1,000 ብር ክፍያ እና ማረጋገጫ",
                    subtitleEnglish = "1,000 ETB Registration Fee & Verification"
                ) {
                    // Payment Instructions Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = GoldSoftContainer.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f))
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
                                    imageVector = Icons.Filled.AccountBalance,
                                    contentDescription = null,
                                    tint = GoldAccent
                                )
                                Text(
                                    text = "የምዝገባ ክፍያ መመሪያ • 1,000 ETB",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldDeep
                                )
                            }
                            Text(
                                text = "እባክዎ የ1,000 ብር (1,000 ETB) የውድድር መመዝገቢያ ክፍያ ከታች በተዘረዘሩት የቴሌብር ወይም የባንክ አካውንት ይላኩ፤ ከዚያም የክፍያ ሪፈራንስ ነበር እና የሪሲት ፎቶ/ፋይል ያያይዙ።",
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDeep
                            )
                            HorizontalDivider(color = GoldAccent.copy(alpha = 0.25f))
                            AccountCopyRow(
                                label = "የምዝገባ ክፍያ / Registration Fee",
                                value = paymentAccounts.registrationFeeLabel,
                                context = context,
                                showCopy = false
                            )
                            AccountCopyRow(
                                label = "የቴሌብር ቁጥር / Telebirr Number",
                                value = paymentAccounts.telebirrNumber,
                                context = context
                            )
                            AccountCopyRow(
                                label = "የቴሌብር አካውንት ስም / Telebirr Account Name",
                                value = paymentAccounts.telebirrAccountName,
                                context = context
                            )
                            AccountCopyRow(
                                label = "የባንክ አካውንት ቁጥር / Bank Account Number",
                                value = paymentAccounts.bankAccountNumber,
                                context = context
                            )
                            AccountCopyRow(
                                label = "የባንክ ስም / Bank Account Name",
                                value = paymentAccounts.bankAccountName,
                                context = context
                            )
                            if (paymentAccounts.otherBankNote.isNotBlank()) {
                                Text(
                                    text = "Other Info: ${paymentAccounts.otherBankNote}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldDeep
                                )
                            }
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "የክፍያ መንገድ / Payment Channel *",
                            style = MaterialTheme.typography.titleSmall
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaymentChannel.entries.forEach { channel ->
                                val selected = formState.selectedPaymentChannel == channel
                                FilterChip(
                                    selected = selected,
                                    onClick = { onPaymentChannelSelect(channel) },
                                    label = {
                                        Text("${channel.englishLabel} (${channel.amharicLabel})")
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.testTag("payment_${channel.name}")
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = formState.paymentReference,
                        onValueChange = onPaymentReferenceChange,
                        label = { Text("የክፍያ ሪፈራንስ ነበር / Payment Reference ID *") },
                        placeholder = { Text("ምሳሌ፡ FT26326X891B ወይም BGE94K21") },
                        leadingIcon = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                        isError = formState.paymentReferenceError != null,
                        supportingText = formState.paymentReferenceError?.let { err -> { Text(err) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("payment_reference_input")
                    )

                    AttachmentUploadCard(
                        titleAmharic = "የክፍያ ሪሲት አታችመንት / Payment Receipt *",
                        subtitleEnglish = "Screenshot or bank transfer receipt",
                        allowedHint = "Supported: Receipt Image (PNG, JPG) or PDF",
                        selectedFile = formState.receiptFile,
                        errorMessage = formState.receiptFileError,
                        onPickFile = {
                            receiptImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onSecondaryPick = { receiptDocLauncher.launch(RECEIPT_DOC_MIME_TYPES) },
                        secondaryPickLabel = "PDF",
                        onPreviewFile = { previewFile = it },
                        onClearFile = onClearReceiptFile,
                        testTag = "receipt_attachment_button"
                    )
                }

                // General Validation / Duplicate Error Banner
                AnimatedVisibility(visible = formState.generalError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = formState.generalError.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Primary Submit Button
                Button(
                    onClick = onSubmit,
                    enabled = !formState.isSubmitting && stagesConfig.registrationOpen,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .testTag("submit_registration_button")
                ) {
                    if (formState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.5.dp
                        )
                    } else if (!stagesConfig.registrationOpen) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "  ምዝገባው ተዘግቷል • Registration Closed",
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "  ምዝገባውን እና የ1,000 ብር ማረጋገጫውን ላክ • Submit",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    previewFile?.let { file ->
        AttachmentPreviewDialog(
            file = file,
            onDismiss = { previewFile = null }
        )
    }

    formState.confirmation?.let { confirmation ->
        SubmissionConfirmationDialog(
            confirmation = confirmation,
            onDismiss = onDismissConfirmation
        )
    }

    if (showRulesDialog) {
        RulesTimelineDialog(
            onDismiss = { showRulesDialog = false }
        )
    }
}

@Composable
private fun SectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    titleAmharic: String,
    subtitleEnglish: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = titleAmharic,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitleEnglish,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun AccountCopyRow(
    label: String,
    value: String,
    context: Context,
    showCopy: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = EmeraldDeep.copy(alpha = 0.78f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = EmeraldDeep
            )
        }
        if (showCopy) {
            IconButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText(label, value))
                    Toast.makeText(context, "Copied / ተቀድቷል", Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.ContentCopy,
                    contentDescription = "Copy account",
                    tint = EmeraldDeep,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun SubmissionConfirmationDialog(
    confirmation: SubmissionConfirmation,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(42.dp)
            )
        },
        title = {
            Text(
                text = "ምዝገባዎ በተሳካ ሁኔታ ተመዝግቧል!",
                style = MaterialTheme.typography.titleLarge,
                color = EmeraldPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ለ Call for Growth ውድድር ያስገቡት የቢዝነስ አይዲያ እና የ1,000 ብር ክፍያ ማረጋገጫ ደርሶናል። አዘጋጁ ካረጋገጠ በኋላ ለህዝብ ድምጽ መስጫ ይቀርባል።",
                    style = MaterialTheme.typography.bodySmall
                )
                HorizontalDivider()
                SummaryLine("የመመዝገቢያ ቁጥር / ID", "#CFG-${confirmation.registrationId}")
                SummaryLine("ሙሉ ስም / Name", confirmation.fullName)
                SummaryLine("ስልክ / Phone", confirmation.phoneNumber)
                SummaryLine("ኢሜል / Email", confirmation.email)
                SummaryLine("የቢዝነስ አይዲያ / Idea", confirmation.ideaTitle)
                SummaryLine(
                    "ዘርፍ / Category",
                    "${confirmation.category.amharicLabel} (${confirmation.category.englishLabel})"
                )
                SummaryLine("ፕሮጀክት ፋይል / Project", confirmation.projectFileName)
                SummaryLine(
                    "የክፍያ መንገድ / Payment",
                    "${confirmation.paymentChannel.englishLabel} • Ref: ${confirmation.paymentReference}"
                )
                SummaryLine("የክፍያ ሪሲት / Receipt", confirmation.receiptFileName)
                SummaryLine("ሁኔታ / Status", "በግምገማ ላይ (Pending Organizer Verification)")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("confirmation_done_button")
            ) {
                Text("እሺ / Done")
            }
        }
    )
}

@Composable
private fun SummaryLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
    }
}
