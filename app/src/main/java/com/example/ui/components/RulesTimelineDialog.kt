package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldSoftContainer

@Composable
fun RulesTimelineDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Gavel,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "የውድድር ደንቦች እና መርሃ ግብር",
                    style = MaterialTheme.typography.titleLarge,
                    color = EmeraldPrimary
                )
                Text(
                    text = "Call for Growth • Rules, Timeline & FAQ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoBlock(
                    icon = Icons.Filled.CalendarMonth,
                    title = "1. የውድድር ደረጃዎች • Competition Stages",
                    bullets = listOf(
                        "ደረጃ 1 (Registration): ተወዳዳሪዎች ስም፣ ስልክ፣ ኢሜል፣ የቢዝነስ አይዲያ፣ የፕሮጀክት ፋይል እና የ1,000 ብር ክፍያ ማረጋገጫ (Telebirr 251934188825 / CBE 1000294899883) ያስገባሉ።",
                        "ደረጃ 2 (Organizer Review): አስተዳዳሪዎች ክፍያውን እና የቢዝነስ ሃሳቡን አረጋግጠው ለህዝብ ድምጽ ያጸድቃሉ።",
                        "ደረጃ 3 (Public Voting): የጸደቁ እጩዎች ለህዝብ ድምጽ ይቀርባሉ (በአንድ ሰው አንድ ድምጽ ብቻ)።",
                        "ደረጃ 4 (Winners Podium): 1ኛ 500,000 ETB • 2ኛ 200,000 ETB • 3ኛ 100,000 ETB ሽልማት ይሰጣል።"
                    )
                )

                HorizontalDivider()

                InfoBlock(
                    icon = Icons.Filled.EmojiEvents,
                    title = "2. የመመዘኛ መስፈርቶች • Evaluation Criteria",
                    bullets = listOf(
                        "ፈጠራ እና መፍትሄ (Innovation & Problem Fit) — 30%",
                        "የገበያ እድል እና ዘላቂነት (Market & Scalability) — 30%",
                        "ማህበራዊ እና ኢኮኖሚያዊ ተጽዕኖ (Job Creation & Impact) — 20%",
                        "የህዝብ ድጋፍ እና ተአማኒነት (Public Voting & Pitch Quality) — 20%"
                    )
                )

                HorizontalDivider()

                InfoBlock(
                    icon = Icons.Filled.Shield,
                    title = "3. የ1 ድምጽ ደህንነት • Fair Voting Policy",
                    bullets = listOf(
                        "እያንዳንዱ መራጭ ከሁሉም እጩዎች ለአንዱ ብቻ ድምጽ መስጠት ይችላል።",
                        "ተመሳሳይ ኢሜል ወይም ስልክ ቁጥር (+251/09) እና ተመሳሳይ መሣሪያ ድጋሚ ድምጽ እንዳይሰጥ በራስ-ሰር ይቆለፋል።"
                    )
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GoldSoftContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "ለተጨማሪ መረጃ እና የክፍያ ማረጋገጫ ጥያቄ፡ Telebirr 251934188825 (amanuel) • CBE 1000294899883",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = EmeraldDeep,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("ተረድቻለሁ / Got It")
            }
        }
    )
}

@Composable
private fun InfoBlock(
    icon: ImageVector,
    title: String,
    bullets: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
        }
        bullets.forEach { item ->
            Text(
                text = "• $item",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
