package com.wjploop.radarviewdemo

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import kotlin.math.*

class RadarView @JvmOverloads constructor(
    context: Context? = null,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) :
    View(context, attrs, defStyle) {

    private val sides = 6
    val titles = arrayListOf<String>("a", "b", "cerw", "eqwer", "fwer", "gerewr")
    val dataSet = arrayListOf<Double>(0.1, 0.3, 0.5, 0.7, 0.9, 1.0)
    var centerX = 0f
    var centerY = 0f
    var radius = 0f
    private val linePaint = Paint().apply {
        strokeWidth = 5f
        color = Color.BLUE
        style = Paint.Style.STROKE
    }
    private val textPaint = Paint().apply {
        textSize = 40f
        strokeWidth = 5f
    }

    private val dataPointPaint = Paint().apply {

    }

    private val dataCoverPaint = Paint().apply {
        color = Color.parseColor("#804D5AA3")
        style = Paint.Style.FILL_AND_STROKE
    }
    private val range = 2 * PI / sides
    private val angles: List<Double>
        get() {
            return (0 until sides).map {
                range * it
            }
        }


    private fun log(str: String) {
        Log.d("RadarView", str)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        radius = minOf(h, w) / 2 * 0.9f
        centerX = (w / 2).toFloat()
        centerY = (h / 2).toFloat()
        log("w $w, radius $radius")
    }

    override fun onDraw(canvas: Canvas) {
        drawPolygon(canvas)
        drawLines(canvas)
        drawTitle(canvas)
        drawData(canvas)
    }

    private fun drawData(canvas: Canvas) {

        val path = Path()
        for (i in 0 until sides) {
            val angle = i * (2 * PI / sides)
            val score = dataSet[i]
            var x = (centerX + radius * score * cos(angle)).toFloat()
            val y = (centerY + radius * score * sin(angle)).toFloat()


            canvas.drawCircle(x, y, 10f, dataPointPaint)
            if (i == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        path.close()
        canvas.drawPath(path, dataCoverPaint)
    }

    private fun drawLines(canvas: Canvas) {
        for (i in 0 until sides) {
            val angle = i * (2 * PI / sides)
            val x = (centerX + radius * cos(angle)).toFloat()
            val y = (centerY + radius * sin(angle)).toFloat()
            canvas.drawLine(centerX, centerY, x, y, linePaint)
        }
    }

    private fun drawTitle(canvas: Canvas) {
        val fontHeight: Float = with(textPaint.fontMetrics) {
            descent - ascent
        }

        for (i in 0 until sides) {
            val angle = i * (2 * PI / sides)
            var x = (centerX + (radius + fontHeight / 2) * cos(angle)).toFloat()

            val y = (centerY + (radius + fontHeight / 2) * sin(angle)).toFloat()
            // ??????????????????????????????????????????????????????
            // ?????????4???????????????
            val regions = (0..4).map { 2 * PI * it / 4 }

            when {
                angle >= regions[0] && angle <= regions[1] -> {
                }
                angle >= regions[1] && angle <= regions[2] -> {
                    x -= textPaint.measureText(titles[i])
                }
                angle >= regions[2] && angle <= regions[3] -> {
                    x -= textPaint.measureText(titles[i])
                }
                angle >= regions[3] && angle <= regions[4] -> {

                }
            }
            canvas.drawText(titles[i], x, y, textPaint)
        }
    }

    private fun drawPolygon(canvas: Canvas) {
        val path = Path()
        val r = radius / (sides - 1)    // ??????????????????
        for (i in 1 until sides) {  // ??? side -1 ??????
            val curR = i * r
            path.reset()
            for (angel in (0..sides).map { it * (2 * PI / sides) }) {
                // ?????????side???????????????????????????????????????curR???????????????side??????
                // ???????????????????????????y = 0, x = r, ???????????????, y = r*sin(q), x = r*cos(q)???q = 0
                // ????????????????????????????????????????????????q????????????q???[0,2pi)?????????????????????
                // ?????????????????????side?????????????????????side???????????????
                // ??????side??????????????????????????????
                val x = centerX + (curR * cos(angel)).toFloat()
                val y = centerY + (curR * sin(angel)).toFloat()

                if (angel == 0.0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }
            canvas.drawPath(path, linePaint)
        }
    }


    // ??????????????????????????????
    var select = -1

    var lastX = -1f
    var lastY = -1f

    // ???????????????????????????
    // ????????? 0~2PI
    private fun angel(x: Float, y: Float): Double {
        val dx = x - centerX
//        val dy = centerY - y  // ???????????????y?????????~
        val dy = y - centerY  // ???????????????y?????????~

        val len = sqrt(dx * dx + dy * dy)
//        if (len > radius) {
//            select = -1
//        }
        // -pi/2 ~ pi/2
        // ????????????????????????????????????????????????
        var angel = atan((dy / dx)).toDouble()
        when {
            dx >= 0 && dy >= 0 -> {

            }
            dx <= 0 && dy >= 0 -> {
                // tan30 = tan(180-30)
                angel += PI
            }
            dx <= 0 && dy <= 0 -> {
                angel += PI
            }
            dx >= 0 && dy <= 0 -> {
                angel += 2 * PI
            }
        }
        return angel
    }

    /**
     * ????????????????????? ??????????????????
     * */
    private fun lengthOnLine(x: Float, y: Float, line: Int): Double {
        val dx = x - centerX
        val dy = y - centerY
        val r = sqrt(dx * dx + dy * dy)
        val angelLine = angles[line]
        val angelPoint = angel(x, y)
        val dAngel = abs(angelLine - angelPoint) % (2 * PI)
        val len = r * cos(dAngel)
        return abs(len)

    }

    fun lenToCenter2(x: Float, y: Float): Float {
        return (x - centerX) * x - centerX + (y - centerY) * (y - centerY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action and MotionEvent.ACTION_MASK) {
            // ????????????????????????
            MotionEvent.ACTION_DOWN -> {
                val x = event.x
                val y = event.y
                val angel = angel(x, y)

                if (angel <= angles[0] + range / 2 || angel >= angles[angles.size - 1] + range / 2) {
                    select = 0
                } else {
                    for (i in 1 until angles.size) {
                        if (angel >= angles[i - 1] + range / 2 && angel <= angles[i] + range / 2) {
                            select = i
                            break;
                        }
                    }
                }
                if (select != -1) {
                    lastX = x
                    lastY = y
                } else {
                    return true
                }
                log("action down select data index $select and angel ${angel * 360 / 2 / PI}")
            }
            MotionEvent.ACTION_MOVE -> {
                // ?????????????????????????????????
                val x = event.x
                val y = event.y

                val dx = x - lastX
                val dy = y - lastY   // ???????????????????????????
                // ??????????????????????????? ???????????????????????????????????????

                // ????????????
//                if (dx * dx + dy * dy < 4) {
//                    return false
//                }

                // ?????????????????????????????????
                if (select == -1) {
                    return false
                }
                val sign = if (lenToCenter2(x, y) > lenToCenter2(lastX, lastY)) { 1 } else -1


                var offset = abs(lengthOnLine(x, y, select) - lengthOnLine(lastX, lastY, select))
                offset *= sign

                log("select $select offset $offset")
                dataSet[select] =
                    (dataSet[select] + offset / radius).coerceIn(0.0, 1.0)
                invalidate()
                lastX = x
                lastY = y
                return true
            }
            MotionEvent.ACTION_UP -> {
                select = -1

            }
        }
        return true
    }

}