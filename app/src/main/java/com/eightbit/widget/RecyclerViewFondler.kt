package com.eightbit.widget

import android.graphics.Canvas
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

class RecyclerViewFondler(var recyclerView: RecyclerView) {

    var swipeCallback: SwipeCallback? = null
    var swipeDirections: Int? = null

    private val drawerTouchCallback: ItemTouchHelper.SimpleCallback =
        object : ItemTouchHelper.SimpleCallback(
            0, swipeDirections?: ItemTouchHelper.START or ItemTouchHelper.END
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

    public fun setSwipeCallback(directions: Int, callback: SwipeCallback) {
        swipeDirections = directions
        swipeCallback = callback
        ItemTouchHelper(drawerTouchCallback).attachToRecyclerView(recyclerView)
    }

    interface SwipeCallback {
        fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int)
    }
}