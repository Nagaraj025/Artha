package com.subramanya.artha.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.subramanya.artha.R

/** The two account kinds the user can add for an unmatched SMS hint. */
enum class AddAccountKind { ACCOUNT, CARD }

/** Small chooser asking whether an unmatched SMS account belongs to a bank account or a card,
 *  since the SMS wording doesn't reliably disambiguate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAccountKindChooser(onChosen: (AddAccountKind) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            Text(
                text = stringResource(R.string.review_add_kind_title),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.review_add_kind_account)) },
                modifier = Modifier.clickable { onChosen(AddAccountKind.ACCOUNT) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.review_add_kind_card)) },
                modifier = Modifier.clickable { onChosen(AddAccountKind.CARD) },
            )
        }
    }
}
