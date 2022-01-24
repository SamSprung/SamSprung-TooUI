package com.eightbit.samsprung


/* ====================================================================
 * Copyright (c) 2012-2022 AbandonedCart.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. All advertising materials mentioning features or use of this
 *    software and redistributions of any form whatsoever
 *    must display the following acknowledgment:
 *    "This product includes software developed by AbandonedCart" unless
 *    otherwise displayed by tagged, public repository entries.
 *
 * 4. The names "8-Bit Dream", "TwistedUmbrella" and "AbandonedCart"
 *    must not be used in any form to endorse or promote products
 *    derived from this software without prior written permission. For
 *    written permission, please contact enderinexiledc@gmail.com
 *
 * 5. Products derived from this software may not be called "8-Bit Dream",
 *    "TwistedUmbrella" or "AbandonedCart" nor may these labels appear
 *    in their names without prior written permission of AbandonedCart.
 *
 * THIS SOFTWARE IS PROVIDED BY AbandonedCart ``AS IS'' AND ANY
 * EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE OpenSSL PROJECT OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * The license and distribution terms for any publicly available version or
 * derivative of this code cannot be changed.  i.e. this code cannot simply be
 * copied and put under another distribution license
 * [including the GNU Public License.] Content not subject to these terms is
 * subject to to the terms and conditions of the Apache License, Version 2.0.
 */

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import kotlin.math.min

class VerticalStrokeTextView : AppCompatTextView {
    private var textBounds = Rect()
    private var direction = 0
    private var strokeWidth = 1f
    private var strokeColor = -0x1000000

    constructor(context: Context) : super(context) {}
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val a = context.obtainStyledAttributes(
            attrs, R.styleable.VerticalStrokeTextView
        )
        direction = a.getInt(R.styleable.VerticalStrokeTextView_direction, 0)
        strokeWidth = a.getDimensionPixelSize(R.styleable.VerticalStrokeTextView_stroke_width, 1).toFloat()
        strokeColor = a.getColor(R.styleable.VerticalStrokeTextView_stroke_color, -0x1000000)
        a.recycle()
        requestLayout()
        invalidate()
    }

    @Suppress("UNUSED")
    fun setDirection(direction: Int) {
        this.direction = direction
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        paint.getTextBounds(
            text.toString(), 0, text.length,
            textBounds
        )
        if (direction == ORIENTATION_LEFT_TO_RIGHT
            || direction == ORIENTATION_RIGHT_TO_LEFT
        ) {
            setMeasuredDimension(
                measureHeight(widthMeasureSpec),
                measureWidth(heightMeasureSpec)
            )
        } else if (direction == ORIENTATION_UP_TO_DOWN
            || direction == ORIENTATION_DOWN_TO_UP
        ) {
            setMeasuredDimension(
                measureWidth(widthMeasureSpec),
                measureHeight(heightMeasureSpec)
            )
        }
    }

    private fun measureWidth(measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = (textBounds.height() + paddingTop
                    + paddingBottom)
            // result = text_bounds.height();
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        return result
    }

    private fun measureHeight(measureSpec: Int): Int {
        var result: Int
        val specMode = MeasureSpec.getMode(measureSpec)
        val specSize = MeasureSpec.getSize(measureSpec)
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize
        } else {
            result = textBounds.width() + paddingLeft + paddingRight
            // result = text_bounds.width();
            if (specMode == MeasureSpec.AT_MOST) {
                result = min(result, specSize)
            }
        }
        return result
    }

    val path = Path()

    override fun onDraw(canvas: Canvas) {
        canvas.save()
        val startX: Int
        val startY: Int
        val stopX: Int
        val stopY: Int
        when (direction) {
            ORIENTATION_UP_TO_DOWN -> {
                startX = width - textBounds.height() shr 1
                startY = height - textBounds.width() shr 1
                stopX = width - textBounds.height() shr 1
                stopY = height + textBounds.width() shr 1
                path.moveTo(startX.toFloat(), startY.toFloat())
                path.lineTo(stopX.toFloat(), stopY.toFloat())
            }
            ORIENTATION_DOWN_TO_UP -> {
                startX = width + textBounds.height() shr 1
                startY = height + textBounds.width() shr 1
                stopX = width + textBounds.height() shr 1
                stopY = height - textBounds.width() shr 1
                path.moveTo(startX.toFloat(), startY.toFloat())
                path.lineTo(stopX.toFloat(), stopY.toFloat())
            }
            ORIENTATION_LEFT_TO_RIGHT -> {
                startX = width - textBounds.width() shr 1
                startY = height + textBounds.height() shr 1
                stopX = width + textBounds.width() shr 1
                stopY = height + textBounds.height() shr 1
                path.moveTo(startX.toFloat(), startY.toFloat())
                path.lineTo(stopX.toFloat(), stopY.toFloat())
            }
            ORIENTATION_RIGHT_TO_LEFT -> {
                startX = width + textBounds.width() shr 1
                startY = height - textBounds.height() shr 1
                stopX = width - textBounds.width() shr 1
                stopY = height - textBounds.height() shr 1
                path.moveTo(startX.toFloat(), startY.toFloat())
                path.lineTo(stopX.toFloat(), stopY.toFloat())
            }
        }
        val paint = this.paint
        paint.style = Paint.Style.STROKE
        paint.color = strokeColor
        paint.strokeWidth = strokeWidth
        canvas.drawTextOnPath(text.toString(), path, 0f, 0f, paint)
        paint.style = Paint.Style.FILL
        paint.color = this.currentTextColor
        canvas.drawTextOnPath(text.toString(), path, 0f, 0f, paint)
        canvas.restore()

    }

    companion object {
        const val ORIENTATION_UP_TO_DOWN = 0
        const val ORIENTATION_DOWN_TO_UP = 1
        const val ORIENTATION_LEFT_TO_RIGHT = 2
        const val ORIENTATION_RIGHT_TO_LEFT = 3
    }
}
