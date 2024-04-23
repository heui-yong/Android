package com.example.pytorch_yolo.custom

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.pytorch_yolo.processor.PrePostProcessor
import com.example.pytorch_yolo.processor.Result

class ResultView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val textX = 40
    private val textY = 35
    private val textWidth = 260
    private val textHeight = 50

    private val paintRectangle = Paint().apply {
        color = Color.YELLOW
    }
    private val paintText = Paint()
    private var results: ArrayList<Result>? = null
    private lateinit var prePostProcessor: PrePostProcessor

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        results?.forEach { result ->
            paintRectangle.apply {
                strokeWidth = 5f
                style = Paint.Style.STROKE
            }
            canvas.drawRect(result.rect, paintRectangle)

            val path = Path()
            val rectF = RectF(
                result.rect.left.toFloat(), result.rect.top.toFloat(),
                (result.rect.left + textWidth).toFloat(), (result.rect.top + textHeight).toFloat()
            )
            path.addRect(rectF, Path.Direction.CW)
            paintText.color = Color.MAGENTA
            canvas.drawPath(path, paintText)

            paintText.apply {
                color = Color.WHITE
                strokeWidth = 0f
                style = Paint.Style.FILL
                textSize = 32f
            }

            canvas.drawText(
                prePostProcessor.test(result),
                (result.rect.left + textX).toFloat(), (result.rect.top + textY).toFloat(), paintText
            )
        }
    }

    fun setResults(results: ArrayList<Result>, prePostProcessor: PrePostProcessor) {
        this.results = results
        this.prePostProcessor = prePostProcessor
    }
}