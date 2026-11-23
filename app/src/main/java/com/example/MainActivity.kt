package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AppRegistration
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AppRegistration
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.RegistrationStatus
import com.example.ui.AppTab
import com.example.ui.CompetitionViewModel
import com.example.ui.screens.AdminScreen
import com.example.ui.screens.RegistrationScreen
import com.example.ui.screens.VotingScreen
import com.example.ui.theme.CallForGrowthTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CallForGrowthTheme {
                CallForGrowthApp()
            }
        }
    }
}

@Composable
fun CallForGrowthApp() {
    val context = LocalContext.current
    val viewModel: CompetitionViewModel = viewModel(
        factory = CompetitionViewModel.factory(context)
    )

    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val formState by viewModel.formState.collectAsStateWithLifecycle()
    val votingState by viewModel.votingState.collectAsStateWithLifecycle()
    val adminState by viewModel.adminState.collectAsStateWithLifecycle()
    val allRegistrations by viewModel.allRegistrations.collectAsStateWithLifecycle()
    val approvedNominees by viewModel.approvedNominees.collectAsStateWithLifecycle()
    val totalVotesCast by viewModel.totalVotesCast.collectAsStateWithLifecycle()
    val deviceVoteLock by viewModel.deviceVoteLock.collectAsStateWithLifecycle()
    val paymentAccounts by viewModel.paymentAccountsConfig.collectAsStateWithLifecycle()
    val stagesConfig by viewModel.stagesConfig.collectAsStateWithLifecycle()

    val pendingCount = allRegistrations.count { it.statusKey == RegistrationStatus.PENDING.name }

    BackHandler(enabled = currentTab != AppTab.REGISTER) {
        viewModel.selectTab(AppTab.REGISTER)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavigationRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!useNavigationRail) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 4.dp
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val badgeCount = when (tab) {
                                AppTab.REGISTER -> 0
                                AppTab.VOTE -> approvedNominees.size
                                AppTab.ADMIN -> if (adminState.isUnlocked) pendingCount else 0
                            }
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        badgeCount = badgeCount
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel(tab),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavigationRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        AppTab.entries.forEach { tab ->
                            val selected = currentTab == tab
                            val badgeCount = when (tab) {
                                AppTab.REGISTER -> 0
                                AppTab.VOTE -> approvedNominees.size
                                AppTab.ADMIN -> if (adminState.isUnlocked) pendingCount else 0
                            }
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    TabIconWithBadge(
                                        tab = tab,
                                        selected = selected,
                                        badgeCount = badgeCount
                                    )
                                },
                                label = {
                                    Text(
                                        text = tabLabel(tab),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentTab) {
                        AppTab.REGISTER -> RegistrationScreen(
                            formState = formState,
                            paymentAccounts = paymentAccounts,
                            stagesConfig = stagesConfig,
                            onFullNameChange = viewModel::onFullNameChange,
                            onPhoneChange = viewModel::onPhoneChange,
                            onEmailChange = viewModel::onEmailChange,
                            onIdeaTitleChange = viewModel::onIdeaTitleChange,
                            onCategorySelect = viewModel::onCategorySelect,
                            onIdeaDescriptionChange = viewModel::onIdeaDescriptionChange,
                            onProjectFileSelected = viewModel::onProjectFileSelected,
                            onClearProjectFile = viewModel::clearProjectFile,
                            onPaymentChannelSelect = viewModel::onPaymentChannelSelect,
                            onPaymentReferenceChange = viewModel::onPaymentReferenceChange,
                            onReceiptFileSelected = viewModel::onReceiptFileSelected,
                            onClearReceiptFile = viewModel::clearReceiptFile,
                            onSubmit = viewModel::submitRegistration,
                            onDismissConfirmation = viewModel::dismissConfirmation
                        )
                        AppTab.VOTE -> VotingScreen(
                            nominees = approvedNominees,
                            totalVotesCast = totalVotesCast,
                            deviceVoteLock = deviceVoteLock,
                            votingState = votingState,
                            stagesConfig = stagesConfig,
                            onVoterIdentifierChange = viewModel::onVoterIdentifierChange,
                            onRequestVote = viewModel::requestVoteConfirmation,
                            onConfirmVote = viewModel::confirmAndCastVote,
                            onDismissConfirm = viewModel::dismissVoteConfirmation,
                            onNavigateToRegister = { viewModel.selectTab(AppTab.REGISTER) }
                        )
                        AppTab.ADMIN -> AdminScreen(
                            registrations = allRegistrations,
                            adminState = adminState,
                            paymentAccountsConfig = paymentAccounts,
                            stagesConfig = stagesConfig,
                            onPinInputChange = viewModel::onAdminPinInputChange,
                            onUnlockAdmin = viewModel::unlockAdmin,
                            onLockAdmin = viewModel::lockAdmin,
                            onSelectFilter = viewModel::selectAdminFilter,
                            onUpdateStatus = viewModel::updateStatus,
                            onSavePaymentAccounts = viewModel::updatePaymentAccounts,
                            onUpdateStages = viewModel::updateCompetitionStages,
                            onBuildCsvReport = viewModel::buildCsvReport,
                            onBuildShareSummary = viewModel::buildShareSummary,
                            onShowNotice = viewModel::showAdminNotice,
                            onClearMessage = viewModel::clearAdminMessage
                        )
                    }
                }
            }
        }
    }
}

private fun tabLabel(tab: AppTab): String = when (tab) {
    AppTab.REGISTER -> "ምዝገባ • Register"
    AppTab.VOTE -> "ድምጽ • Vote"
    AppTab.ADMIN -> "አስተዳዳሪ • Admin"
}

@Composable
private fun TabIconWithBadge(
    tab: AppTab,
    selected: Boolean,
    badgeCount: Int
) {
    val icon = when (tab) {
        AppTab.REGISTER -> if (selected) Icons.Filled.AppRegistration else Icons.Outlined.AppRegistration
        AppTab.VOTE -> if (selected) Icons.Filled.HowToVote else Icons.Outlined.HowToVote
        AppTab.ADMIN -> if (selected) Icons.Filled.AdminPanelSettings else Icons.Outlined.AdminPanelSettings
    }
    if (badgeCount > 0) {
        BadgedBox(
            badge = {
                Badge(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ) {
                    Text(badgeCount.toString())
                }
            }
        ) {
            Icon(imageVector = icon, contentDescription = tabLabel(tab))
        }
    } else {
        Icon(imageVector = icon, contentDescription = tabLabel(tab))
    }
}
