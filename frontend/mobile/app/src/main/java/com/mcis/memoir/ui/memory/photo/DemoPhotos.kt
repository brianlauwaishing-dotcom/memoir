package com.mcis.memoir.ui.memory.photo

import android.content.Context
import android.net.Uri
import com.mcis.memoir.R

/**
 * Built-in sample photos shown on the import screen so the memory creation flow
 * can be demoed on devices/emulators without any photos in the gallery.
 *
 * Each entry reuses an existing drawable. Selecting one builds an
 * `android.resource://` Uri that the regular [PhotoSelectionIntent.PhotosPicked]
 * path imports through `addPhoto`, identical to picking from the system gallery.
 */
object DemoPhotos {
    val resIds: List<Int> = listOf(
        R.drawable.eg1,
        R.drawable.eg2,
        R.drawable.eg3,
        R.drawable.grand_mazu_temple,
        R.drawable.sea_protection
    )

    fun uriFor(context: Context, resId: Int): Uri =
        Uri.parse("android.resource://${context.packageName}/$resId")
}
