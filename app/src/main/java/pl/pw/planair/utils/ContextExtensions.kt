package pl.pw.planair.utils

import android.content.Context
import android.content.ContextWrapper
import androidx.fragment.app.FragmentActivity

/**
 * Rozszerzenie Context, które bezpiecznie znajduje najbliższą FragmentActivity w hierarchii ContextWrapper.
 * Jest to rekomendowany sposób uzyskania Activity w Compose, gdy LocalContext nie jest bezpośrednio Activity.
 */
fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}