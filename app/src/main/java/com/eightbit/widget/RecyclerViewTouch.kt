/*
 * ====================================================================
 * Copyright (c) 2021-2023 AbandonedCart.  All rights reserved.
 *
 * See https://github.com/SamSprung/.github/blob/main/LICENSE#L5
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

package com.eightbit.widget

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewTouch(var recyclerView: RecyclerView) {

    var swipeCallback: SwipeCallback? = null
    var swipeDirections: Int? = null

    private val drawerTouchCallback: ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(
            0, swipeDirections ?: (ItemTouchHelper.START or ItemTouchHelper.END)
        ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) { }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            swipeCallback?.onSwiped(viewHolder, direction)
        }
    }

    fun setSwipeCallback(directions: Int, callback: SwipeCallback) {
        swipeDirections = directions
        swipeCallback = callback
        ItemTouchHelper(drawerTouchCallback).attachToRecyclerView(recyclerView)
    }

    interface SwipeCallback {
        fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
    }
}
