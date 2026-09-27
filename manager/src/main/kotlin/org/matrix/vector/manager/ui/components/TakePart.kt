package org.matrix.vector.manager.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.matrix.vector.manager.R
import org.matrix.vector.manager.data.github.GitHubRepository

/**
 * Two doors off the Home page: this repository on GitHub, and a soft reboot.
 *
 * The fork is a personal build rather than a community project, so the old contributor doors -
 * pull requests to review, discussions, a canary to test, an issue to report - are gone. The
 * GitHub door opens the repository itself; the soft-reboot door restarts the framework in place,
 * which is the one action a flashed change regularly asks for. It confirms before it acts, and
 * the confirmation lives with the caller: everything on screen dies with the zygote, so the
 * wording is the caller's business.
 */
@Composable
fun TakePartSection(
    modifier: Modifier = Modifier,
    onOpen: (String) -> Unit,
    onSoftReboot: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.home_actions),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(10.dp))
        // IntrinsicSize.Min, so the two doors in a row settle on the height of the taller one and
        // each can then fill it. Without it a card is only as tall as its own label, and a language
        // where one label wraps and its neighbour does not leaves a short card floating in a tall
        // row.
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Door(
                Icons.Rounded.Code,
                stringResource(R.string.home_github),
                Modifier.weight(1f).fillMaxHeight(),
            ) {
                onOpen(GitHubRepository.LOCAL_REPO_URL)
            }
            Door(
                Icons.Rounded.RestartAlt,
                stringResource(R.string.action_soft_reboot),
                Modifier.weight(1f).fillMaxHeight(),
                onClick = onSoftReboot,
            )
        }
    }
}

@Composable
private fun Door(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        // fillMaxHeight so the content is centred in whatever height the row settled on, rather
        // than sitting at the top of a card that was stretched to match its neighbour.
        Row(
            modifier = Modifier.fillMaxHeight().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}
