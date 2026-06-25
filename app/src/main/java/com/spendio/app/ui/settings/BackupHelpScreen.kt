package com.spendio.app.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupHelpScreen(onClose: () -> Unit) {
    BackHandler { onClose() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("How backup works") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Spendio itself has no internet permission and never uploads anything on its " +
                    "own. These three options exist purely so you can get your data back if something " +
                    "happens to your phone — and every one of them is locked with a passphrase only you know.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HelpCard(
                emoji = "🔄",
                title = "Automatic backup",
                what = "A backup that quietly stays up to date on its own and lives in your Google account — nothing to remember.",
                how = "Turn on \"Automatic backup\" in Settings → Backup and set a passphrase once. From then on, Spendio refreshes the encrypted backup whenever your data changes, and Android syncs it in the background, roughly once a day.",
                why = "If you lose your phone, factory-reset it, or get a new one, reinstalling Spendio and signing into the same Google account offers to restore everything automatically.",
            )

            HelpCard(
                emoji = "📄",
                title = "Back up to a file",
                what = "A single encrypted file you save yourself, wherever you like — phone storage, Drive, email, a USB drive.",
                how = "Settings → Backup → \"Back up to a file\", set a passphrase, pick where to save it. To bring it back later, use \"Restore from a file\" and select that same file.",
                why = "A good one-off safety copy — before clearing app data, switching phones manually, or just wanting a copy outside Google's ecosystem.",
            )

            HelpCard(
                emoji = "☁",
                title = "Google Drive backup",
                what = "Similar to Automatic backup, but the file is uploaded straight to a private, hidden folder in your Drive that only Spendio can see — not your regular Drive files.",
                how = "Settings → Backup → \"Google Drive backup\" → sign in with Google. Spendio uploads automatically after that; \"Back up now\" and \"Restore from Drive\" trigger it on demand.",
                why = "A second, on-the-spot copy alongside Automatic backup — handy if you want to restore on a new phone immediately, without waiting for Android's own backup schedule.",
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "About your passphrase",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    HelpLine("It's the only key. Every backup is encrypted with the passphrase you choose — nobody else, not even Spendio's developer, can open it without that exact passphrase.")
                    HelpLine("It can't be reset. There's no \"forgot password\" option, because that would mean someone other than you could unlock your data. Forget it, and that backup is unrecoverable.")
                    HelpLine("Write it down somewhere safe the moment you set it — a password manager or a notebook works fine.")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Questions",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FaqItem(
                        q = "Does this use my mobile data?",
                        a = "Spendio itself never connects to the internet. Automatic backup is handled entirely by Android's own system sync; Drive backup and signing in with Google are the only parts that need a connection.",
                    )
                    FaqItem(
                        q = "Is my data still on my phone after I back it up?",
                        a = "Yes. Backing up only ever copies data out — it never removes anything from your phone.",
                    )
                    FaqItem(
                        q = "What's actually inside a backup?",
                        a = "Your transactions — amount, date, category, merchant/payer name, and any note you added. Not the original bank notification text; only a one-way fingerprint of it is kept, just enough to avoid duplicate entries, and the original text can't be recovered from it.",
                    )
                    FaqItem(
                        q = "Why did it say \"backup from your old phone\" when I didn't change phones?",
                        a = "That screen shows up whenever this install has zero transactions and a backup file is sitting on disk — which can also happen right after you turn Automatic backup on. It now only asks once per install.",
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpCard(emoji: String, title: String, what: String, how: String, why: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "$emoji  $title",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            HelpLine("What it is — $what")
            HelpLine("How to use it — $how")
            HelpLine("Why it matters — $why")
        }
    }
}

@Composable
private fun HelpLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun FaqItem(q: String, a: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(q, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
