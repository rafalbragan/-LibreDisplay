package com.libredisplay.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree

/**
 * Registers a text field with the platform autofill service.
 *
 * This is what makes Google Password Manager, Samsung Pass, Bitwarden, 1Password and every other
 * autofill provider recognise the LibreCare login form, offer stored credentials and propose to
 * save new ones.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.autofillField(
    autofillTypes: List<AutofillType>,
    onFill: (String) -> Unit
): Modifier = composed {
    val autofill = LocalAutofill.current
    val autofillNode = remember(autofillTypes) {
        AutofillNode(autofillTypes = autofillTypes, onFill = onFill)
    }
    LocalAutofillTree.current += autofillNode

    this
        .onGloballyPositioned { coordinates ->
            autofillNode.boundingBox = coordinates.boundsInWindow()
        }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) {
                    requestAutofillForNode(autofillNode)
                } else {
                    cancelAutofillForNode(autofillNode)
                }
            }
        }
}

/**
 * Asks the autofill service to store the credentials that were just used.
 *
 * Must be called after a successful login, otherwise the password manager never offers to save.
 */
@Composable
fun rememberAutofillCommit(): () -> Unit {
    val context = androidx.compose.ui.platform.LocalContext.current
    return remember(context) {
        {
            runCatching {
                context.getSystemService(android.view.autofill.AutofillManager::class.java)?.commit()
            }
            Unit
        }
    }
}

