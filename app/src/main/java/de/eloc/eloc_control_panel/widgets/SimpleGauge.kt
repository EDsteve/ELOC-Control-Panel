package de.eloc.eloc_control_panel.widgets

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import de.eloc.eloc_control_panel.R
import kotlin.math.ceil
import kotlin.math.min
import androidx.core.content.withStyledAttributes

const val CRITICAL_COLOR_THRESHOLD_DEFAULT = 15

const val LOW_COLOR_THRESHOLD_MIN = 15
const val LOW_COLOR_THRESHOLD_MAX = 95
const val LOW_COLOR_THRESHOLD_DEFAULT = 45

class SimpleGauge : View {

    private val pen = Paint()
    private var centerX = 1.0f
    private var centerY = 1.0f
    private var radius = 1.0f
    private var gaugeValue = 0.0
    private var showDiscreteColors = false
    private var canInvalidate = false
    private var normalColor = Color.GREEN
    private var lowColor = Color.YELLOW
    private var lowColorThreshold: Int = LOW_COLOR_THRESHOLD_DEFAULT
    private var criticalColorThreshold: Int = CRITICAL_COLOR_THRESHOLD_DEFAULT
    private var criticalColor = Color.RED
    private var lowRange = 30.0f
    private var alwaysFilled = true
    private val arc = RectF(0f, 0f, 0f, 0f)
    var errorMode = false
    private val exclamationMarkBody = Path()

    constructor(context: Context) : super(context) {
        initialize(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        initialize(attrs)
    }

    private fun initialize(attrs: AttributeSet?) {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.SimpleGauge) {
                val show =
                    getBoolean(R.styleable.SimpleGauge_showDiscreteColors, false)
                setShowDiscreteColors(show)
                errorMode = getBoolean(R.styleable.SimpleGauge_errorMode, false)
                alwaysFilled = getBoolean(R.styleable.SimpleGauge_alwaysFilled, true)
                normalColor = getColor(R.styleable.SimpleGauge_normalColor, Color.GREEN)
                lowColor = getColor(R.styleable.SimpleGauge_lowColor, Color.YELLOW)
                criticalColor = getColor(R.styleable.SimpleGauge_criticalColor, Color.RED)
                lowColorThreshold =
                    getInteger(
                        R.styleable.SimpleGauge_lowColorThreshold,
                        LOW_COLOR_THRESHOLD_DEFAULT
                    )
                criticalColorThreshold = getInteger(
                    R.styleable.SimpleGauge_criticalColorThreshold,
                    CRITICAL_COLOR_THRESHOLD_DEFAULT
                )
                lowRange = if (lowColorThreshold <= LOW_COLOR_THRESHOLD_MIN) {
                    LOW_COLOR_THRESHOLD_MIN.toFloat()
                } else if (lowColorThreshold >= LOW_COLOR_THRESHOLD_MAX) {
                    LOW_COLOR_THRESHOLD_MAX.toFloat()
                } else {
                    lowColorThreshold.toFloat()
                }
            }
        }
        canInvalidate = true
    }

    private fun getValueColor(): Int {
        if (errorMode) {
            criticalColor = Color.RED;
            val r = Color.red(criticalColor)
            val b = Color.blue(criticalColor)
            val g = Color.green(criticalColor)
            return Color.rgb(r, g, b)
        }

        normalizeGaugeValue()

        if (showDiscreteColors) {
            return if (gaugeValue <= criticalColorThreshold)
                criticalColor
            else if (gaugeValue <= lowColorThreshold)
                lowColor
            else
                normalColor
        }

        // Set appropriate shade
        val highRange = 100 - lowRange
        val firstColor: Int
        val secondColor: Int
        val range: Float
        val rangeValue: Double
        if (gaugeValue <= lowRange) {
            firstColor = criticalColor
            secondColor = lowColor
            range = lowRange
            rangeValue = gaugeValue
        } else {
            firstColor = lowColor
            secondColor = normalColor
            range = highRange
            rangeValue = gaugeValue - lowRange
        }

        val redStep = (Color.red(firstColor) - Color.red(secondColor)) / range
        val blueStep = (Color.blue(firstColor) - Color.blue(secondColor)) / range
        val greenStep = (Color.green(firstColor) - Color.green(secondColor)) / range
        val redDelta = rangeValue * redStep
        val blueDelta = rangeValue * blueStep
        val greenDelta = rangeValue * greenStep
        val red = Color.red(firstColor) - redDelta
        val blue = Color.blue(firstColor) - blueDelta
        val green = Color.green(firstColor) - greenDelta
        val r = ceil(red).toInt()
        val g = ceil(green).toInt()
        val b = ceil(blue).toInt()
        return Color.rgb(r, g, b)
    }

    private fun updateData() {
        val dips = 0.065f * min(width, height) // 6.5% of smallest side
        val penWidth = dips * context.resources.displayMetrics.density
        pen.color = getValueColor()
        pen.style = Paint.Style.STROKE
        pen.strokeWidth = penWidth

        centerX = width / 2.0f
        centerY = height / 2.0f
        val smallSide = if (width < height) width else height
        radius = 0.5f * (smallSide - penWidth)
        if (!alwaysFilled) {
            val halfPenWidth = penWidth / 2f
            if (width < height) {
                val diameter = width - penWidth
                arc.top = (height - diameter) / 2f
                arc.left = halfPenWidth
                arc.right = diameter + halfPenWidth
                arc.bottom = arc.top + diameter
            } else {
                val diameter = height - penWidth
                arc.top = halfPenWidth
                arc.left = (width - diameter) / 2f
                arc.right = arc.left + diameter
                arc.bottom = diameter + halfPenWidth
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Clear background
        canvas.drawColor(Color.TRANSPARENT)

        updateData()
        if (errorMode) {
            pen.color = criticalColor
            canvas.drawCircle(centerX, centerY, radius, pen)

            pen.style = Paint.Style.FILL
            pen.strokeWidth = 1f
            val dotRadius = 0.1f * radius
            val dotDiameter = dotRadius * 2
            val dotY = centerY + (dotRadius * 5)
            canvas.drawCircle(centerX, dotY, dotRadius, pen)

            val botLeftX = centerX - (dotDiameter * 0.2f)
            val botY = dotY - (dotRadius * 1.5f)
            exclamationMarkBody.moveTo(botLeftX, botY)

            val topLeftX = centerX - dotRadius
            val topLeftY = botY - radius
            exclamationMarkBody.lineTo(topLeftX, topLeftY)

            exclamationMarkBody.rLineTo(dotDiameter, 0f)

            val botRightX = centerX + (dotDiameter * 0.2f)
            exclamationMarkBody.lineTo(botRightX, botY)
            exclamationMarkBody.close()
            canvas.drawPath(exclamationMarkBody, pen)
        } else {
            // Ring
            if (alwaysFilled) {
                canvas.drawCircle(centerX, centerY, radius, pen)
            } else {
                val darkColor = pen.color
                val lightColor = Color.argb(
                    60,
                    Color.red(darkColor),
                    Color.green(darkColor),
                    Color.blue(darkColor)
                )
                pen.color = lightColor
                canvas.drawCircle(centerX, centerY, radius, pen)
                pen.color = darkColor
                normalizeGaugeValue()
                canvas.drawArc(arc, 90f, 360f * gaugeValue.toFloat() / 100f, false, pen)
            }
        }
    }

    private fun setShowDiscreteColors(show: Boolean) {
        showDiscreteColors = show
        if (canInvalidate) {
            invalidate()
        }
    }

    fun updateValue(percent: Double) {
        gaugeValue = percent
        errorMode = (gaugeValue < 0)
        normalizeGaugeValue()
        invalidate()
    }

    private fun normalizeGaugeValue() {
        gaugeValue = if (gaugeValue < 0.0) {
            0.0
        } else if (gaugeValue > 100.0) {
            100.0
        } else {
            gaugeValue
        }
    }

    fun getValue() = gaugeValue
}