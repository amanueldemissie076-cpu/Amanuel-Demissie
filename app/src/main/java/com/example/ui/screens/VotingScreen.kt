package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.CompetitionStagesConfig
import com.example.data.DeviceVoteLock
import com.example.data.IdeaCategory
import com.example.data.NomineeWithVotes
import com.example.data.PickedFileInfo
import com.example.ui.VotingUiState
import com.example.ui.components.AttachmentPreviewDialog
import com.example.ui.components.FileMetadataResolver
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldSoftContainer
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessSoftContainer

@Composable
fun VotingScreen(
    nominees: List<NomineeWithVotes>,
    totalVotesCast: Int,
    deviceVoteLock: DeviceVoteLock,
    votingState: VotingUiState,
    stagesConfig: CompetitionStagesConfig = CompetitionStagesConfig(),
    onVoterIdentifierChange: (String) -> Unit,
    onRequestVote: (NomineeWithVotes) -> Unit,
    onConfirmVote: () -> Unit,
    onDismissConfirm: () -> Unit,
    onNavigateToRegister: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSecurityDetails by remember { mutableStateOf(false) }
    var previewFile by remember { mutableStateOf<PickedFileInfo?>(null) }

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
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = EmeraldDeep
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = GoldAccent.copy(alpha = 0.24f),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.HowToVote,
                                    contentDescription = null,
                                    tint = GoldSoftContainer,
                                    modifier = Modifier.padding(9.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "ደህንነቱ የተጠበቀ የህዝብ ድምጽ መስጫ",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = "Secure Public Voting • Strictly 1 Vote Per Person",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = GoldSoftContainer
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatPill(
                                label = "Approved Nominees",
                                value = "${nominees.size}",
                                modifier = Modifier.weight(1f)
                            )
                            StatPill(
                                label = "Total Verified Votes",
                                value = "$totalVotesCast",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Official Winners Podium when published
                if (stagesConfig.winnersPublished && nominees.isNotEmpty()) {
                    WinnersPodiumCard(
                        topNominees = nominees.sortedByDescending { it.voteCount }.take(3)
                    )
                }

                // How Duplicate Voting is Prevented
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
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
                                    imageVector = Icons.Filled.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "የድምጽ ደህንነት እንዴት ይጠበቃል? / Duplicate Vote Prevention",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            TextButton(onClick = { showSecurityDetails = !showSecurityDetails }) {
                                Text(if (showSecurityDetails) "ደብቅ" else "ዝርዝር")
                                Icon(
                                    imageVector = if (showSecurityDetails) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        Text(
                            text = "እያንዳንዱ መራጭ ከሁሉም እጩዎች መካከል አንድ ድምጽ ብቻ መስጠት ይችላል። / Each person may cast strictly ONE vote total across all nominees.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        AnimatedVisibility(visible = showSecurityDetails) {
                            Column(
                                modifier = Modifier.padding(top = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                HorizontalDivider()
                                SecurityBullet(
                                    "1. ልዩ መለያ ማረጋገጫ (Unique Identifier):",
                                    "ድምጽ ከመስጠትዎ በፊት የኢሜል አድራሻዎን ወይም የስልክ ቁጥርዎን ማስገባት አለብዎት። (Email or Phone Number is normalized and checked.)"
                                )
                                SecurityBullet(
                                    "2. የዳታቤዝ ድግግሞሽ እገዳ (Database Unique Lock):",
                                    "አንድ ኢሜል ወይም ስልክ ቁጥር (+251/09 ተመሳሳይ ፎርማትን ጨምሮ) ሁለተኛ ጊዜ ድምጽ እንዳይሰጥ በዳታቤዝ ደረጃ ይታገዳል።"
                                )
                                SecurityBullet(
                                    "3. የመሣሪያ/ሴሽን ቁልፍ (Device & Session Lock):",
                                    "አንድ ድምጽ እንደተሰጠ ይህ ስልክ/መሣሪያ ሌላ መለያ በመቀየር ድጋሚ ድምጽ እንዳይሰጥ ይቆለፋል።"
                                )
                            }
                        }
                    }
                }

                // Voting Closed Banner OR Locked Success Banner OR Voter Identity Box
                if (!stagesConfig.votingOpen) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("voting_closed_banner"),
                        shape = RoundedCornerShape(18.dp),
                        color = GoldSoftContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = EmeraldDeep,
                                modifier = Modifier.size(28.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "የድምጽ መስጫ ጊዜ ተጠናቋል • Public Voting Closed",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = EmeraldDeep
                                )
                                Text(
                                    text = "Public voting has been closed by the organizer. Verified vote counts and standings remain visible below.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldDeep.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                } else if (deviceVoteLock.hasVotedOnDevice) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("vote_locked_banner"),
                        shape = RoundedCornerShape(18.dp),
                        color = SuccessSoftContainer,
                        border = BorderStroke(1.5.dp, SuccessGreen.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(30.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(id = R.string.vote_locked_banner),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = SuccessGreen
                                )
                                Text(
                                    text = "Your vote has been securely recorded and locked for this device (${deviceVoteLock.votedIdentifierMasked}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = EmeraldDeep
                                )
                            }
                        }
                    }
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        shadowElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "የመራጭ መለያ ያረጋግጡ / Voter Verification",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            Text(
                                text = "ከታች ካሉት እጩዎች ለአንዱ ድምጽ ለመስጠት ኢሜልዎን ወይም ስልክ ቁጥርዎን ያስገቡ (አንድ ጊዜ ብቻ)፡",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedTextField(
                                value = votingState.voterIdentifier,
                                onValueChange = onVoterIdentifierChange,
                                label = { Text("ኢሜል ወይም ስልክ ቁጥር / Email or Phone *") },
                                placeholder = { Text("0911223344 or name@example.com") },
                                leadingIcon = { Icon(Icons.Filled.Security, contentDescription = null) },
                                isError = votingState.identifierError != null,
                                supportingText = votingState.identifierError?.let { err -> { Text(err) } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("voter_identifier_input")
                            )
                        }
                    }
                }

                // Transient error/feedback banner
                AnimatedVisibility(visible = votingState.feedbackBanner != null && !deviceVoteLock.hasVotedOnDevice) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (votingState.feedbackIsError) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            SuccessSoftContainer
                        }
                    ) {
                        Text(
                            text = votingState.feedbackBanner.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (votingState.feedbackIsError) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                SuccessGreen
                            },
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }

        if (nominees.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.HowToVote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "እስካሁን ለህዝብ ድምጽ የጸደቀ እጩ የለም",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "No approved nominees yet. Submit a business idea in the Registration tab, then review and promote it in the Organizer Panel to open public voting.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onNavigateToRegister,
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text("ወደ ምዝገባ ሂድ / Go to Registration")
                        }
                    }
                }
            }
        } else {
            items(nominees, key = { it.registration.id }) { nominee ->
                NomineeVoteCard(
                    nominee = nominee,
                    votingOpen = stagesConfig.votingOpen,
                    isDeviceLocked = deviceVoteLock.hasVotedOnDevice,
                    isVotedByThisDevice = deviceVoteLock.hasVotedOnDevice &&
                        deviceVoteLock.votedRegistrationId == nominee.registration.id,
                    onPreviewAttachment = {
                        previewFile = PickedFileInfo(
                            uriString = nominee.registration.projectFileUri,
                            displayName = nominee.registration.projectFileName,
                            mimeType = nominee.registration.projectFileMime,
                            sizeBytes = nominee.registration.projectFileSizeBytes
                        )
                    },
                    onVoteClick = { onRequestVote(nominee) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
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

    votingState.pendingNomineeToConfirm?.let { nominee ->
        AlertDialog(
            onDismissRequest = onDismissConfirm,
            icon = {
                Icon(
                    imageVector = Icons.Filled.HowToVote,
                    contentDescription = null,
                    tint = EmeraldPrimary,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("ድምጽዎን ያረጋግጡ / Confirm Your Single Vote")
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ለ «${nominee.registration.ideaTitle}» (በ${nominee.registration.fullName}) ድምጽ ለመስጠት ተዘጋጅተዋል።",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "ማሳሰቢያ፡ አንድ ጊዜ ብቻ ነው ድምጽ መስጠት የሚቻለው፤ ከሰጡ በኋላ መቀየር ወይም ድጋሚ መምረጥ አይቻልም።",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = onConfirmVote,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("confirm_vote_button")
                ) {
                    Text("ድምጽ አጽድቅ / Confirm Vote")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissConfirm) {
                    Text("ተመለስ / Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.80f)
            )
        }
    }
}

@Composable
private fun SecurityBullet(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun WinnersPodiumCard(
    topNominees: List<NomineeWithVotes>
) {
    val prizeLabels = listOf(
        "1ኛ ደረጃ • 500,000 ETB",
        "2ኛ ደረጃ • 200,000 ETB",
        "3ኛ ደረጃ • 100,000 ETB"
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("winners_podium_card"),
        shape = RoundedCornerShape(20.dp),
        color = GoldSoftContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.5.dp, GoldAccent),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = EmeraldDeep,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = GoldBright,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = "የአሸናፊዎች መድረክ • Official Winners",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = EmeraldDeep
                    )
                    Text(
                        text = "Call for Growth Top Ranked Entrepreneurs",
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldDeep.copy(alpha = 0.8f)
                    )
                }
            }

            HorizontalDivider(color = GoldAccent.copy(alpha = 0.35f))

            topNominees.forEachIndexed { index, nominee ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = prizeLabels.getOrElse(index) { "#${index + 1}" },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = GoldAccent
                            )
                            Text(
                                text = nominee.registration.ideaTitle,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldDeep,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = nominee.registration.fullName,
                                style = MaterialTheme.typography.bodySmall,
                                color = EmeraldDeep.copy(alpha = 0.78f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = EmeraldPrimary.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = "${nominee.voteCount} ድምጽ",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = EmeraldDeep,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NomineeVoteCard(
    nominee: NomineeWithVotes,
    votingOpen: Boolean,
    isDeviceLocked: Boolean,
    isVotedByThisDevice: Boolean,
    onPreviewAttachment: () -> Unit,
    onVoteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reg = nominee.registration
    val category = IdeaCategory.fromKey(reg.categoryKey)
    val fileInfo = PickedFileInfo(
        uriString = reg.projectFileUri,
        displayName = reg.projectFileName,
        mimeType = reg.projectFileMime,
        sizeBytes = reg.projectFileSizeBytes
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 3.dp,
        border = BorderStroke(
            width = if (isVotedByThisDevice) 2.dp else 1.dp,
            color = if (isVotedByThisDevice) SuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "${category.amharicLabel} • ${category.englishLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isVotedByThisDevice) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SuccessSoftContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "የመረጡት / Your Vote",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreen
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = reg.ideaTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "መስራች / Founder: ${reg.fullName}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                text = reg.ideaDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Project Attachment Info Row
            Surface(
                onClick = onPreviewAttachment,
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = reg.projectFileName,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${fileInfo.extensionBadge} • ${FileMetadataResolver.formatBytes(reg.projectFileSizeBytes)} • Tap to view",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Live Vote Tally Progress
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "የተገኘ ድምጽ / Votes: ${nominee.voteCount}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${(nominee.voteSharePercent * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    LinearProgressIndicator(
                        progress = { nominee.voteSharePercent.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxSize(),
                        color = if (isVotedByThisDevice) SuccessGreen else EmeraldPrimary,
                        trackColor = Color.Transparent
                    )
                }
            }

            // Single Vote Action Button
            val voteBlocked = isDeviceLocked || !votingOpen
            Button(
                onClick = onVoteClick,
                enabled = !voteBlocked,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isVotedByThisDevice) SuccessGreen else EmeraldPrimary,
                    disabledContainerColor = if (isVotedByThisDevice) SuccessSoftContainer else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag("vote_button_${reg.id}")
            ) {
                Icon(
                    imageVector = if (voteBlocked) Icons.Filled.Lock else Icons.Filled.HowToVote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (voteBlocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
                Text(
                    text = when {
                        isVotedByThisDevice -> "  ድምጽዎ ተመዝግቧል • Voted"
                        !votingOpen -> "  ድምጽ ተዘግቷል • Voting Closed"
                        isDeviceLocked -> "  ድምጽ መስጫ ተቆልፏል • Locked (1 Vote Used)"
                        else -> "  ለዚህ እጩ ድምጽ ስጥ • Vote Once"
                    },
                    color = if (voteBlocked) MaterialTheme.colorScheme.onSurfaceVariant else Color.White
                )
            }
        }
    }
}
