package com.zdmgold.autoclickernega.service.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.zdmgold.autoclickernega.R
import com.zdmgold.autoclickernega.core.model.EngineState
import com.zdmgold.autoclickernega.ui.theme.Tokens

/** What the panel's buttons do. The panel itself holds no logic. */
class PanelActions(
    val onToggleRun: () -> Unit,
    val onStop: () -> Unit,
    val onCancelWait: () -> Unit,
    val onAddTap: () -> Unit,
    val onAddSwipe: () -> Unit,
    val onAddCurve: () -> Unit,
    val onAddImage: () -> Unit,
    val onClose: () -> Unit,
    val onDrag: (dx: Int, dy: Int) -> Unit,
    val onDragEnd: () -> Unit,
)

/** Everything the panel shows at one moment. */
class PanelModel(
    val state: EngineState,
    val status: String,
    val counters: String,
    val hud: HudModel?,
)

/**
 * The floating panel: start or pause, stop, add tap, swipe, curve and image, collapse and close, with the run
 * timer, the counters and the detection HUD. It carries the only shadow in the product, drawn inside its own
 * padding so the window needs no extra room.
 */
@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class PanelView(
    context: Context,
    private val theme: OverlayTheme,
    private val actions: PanelActions,
) : LinearLayout(context) {
    private val shadowPad = theme.dpf(Tokens.PanelShadow.blur + Tokens.PanelShadow.offsetY)
    private val radius = theme.dpf(Tokens.Radius.card)
    private val cardPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = theme.background
            setShadowLayer(
                theme.dpf(Tokens.PanelShadow.blur),
                0f,
                theme.dpf(Tokens.PanelShadow.offsetY),
                theme.shadow,
            )
        }
    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = theme.dpf(Tokens.Stroke.hairline)
            color = theme.border
        }

    private val handle = ImageView(context)
    private val status = label()
    private val counters = label()
    private val hud = HudView(context, theme)
    private val toggle =
        button(R.drawable.ic_play, R.string.panel_start, theme.onAccent, theme.accent) { actions.onToggleRun() }
    private val stop = button(R.drawable.ic_stop, R.string.panel_stop, theme.error, theme.surface) { actions.onStop() }
    private val cancelWait =
        button(R.drawable.ic_close, R.string.panel_cancel_wait, theme.text, theme.surface) { actions.onCancelWait() }
    private val addTap =
        button(R.drawable.ic_tap, R.string.panel_add_tap, theme.markerTap, theme.surface) { actions.onAddTap() }
    private val addSwipe =
        button(R.drawable.ic_swipe, R.string.panel_add_swipe, theme.markerSwipe, theme.surface) { actions.onAddSwipe() }
    private val addCurve =
        button(R.drawable.ic_curve, R.string.panel_add_curve, theme.markerSwipe, theme.surface) { actions.onAddCurve() }
    private val addImage =
        button(
            R.drawable.ic_image_target,
            R.string.panel_add_image,
            theme.markerImage,
            theme.surface,
        ) { actions.onAddImage() }
    private val collapse =
        button(R.drawable.ic_chevron_left, R.string.panel_collapse, theme.text, theme.surface) { toggleCollapsed() }
    private val close =
        button(R.drawable.ic_close, R.string.panel_close, theme.textSecondary, theme.surface) { actions.onClose() }

    private var collapsed = false
    private var lastModel: PanelModel? = null
    private var lastX = 0f
    private var lastY = 0f

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
        val pad = shadowPad.toInt() + theme.dp(INNER_PADDING_DP)
        setPadding(pad, pad, pad, pad)
        setupHandle()
        listOf(
            handle,
            status,
            counters,
            hud,
            toggle,
            stop,
            cancelWait,
            addTap,
            addSwipe,
            addCurve,
            addImage,
            collapse,
            close,
        ).forEach { addView(it) }
    }

    private fun label(): TextView =
        TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, LABEL_SP)
            setTextColor(theme.textSecondary)
            typeface = theme.regular
            gravity = Gravity.CENTER
            maxWidth = theme.dp(LABEL_MAX_WIDTH_DP)
            visibility = View.GONE
        }

    private fun button(
        @DrawableRes icon: Int,
        @StringRes description: Int,
        tint: Int,
        fill: Int,
        onClick: () -> Unit,
    ): ImageView =
        ImageView(context).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(tint)
            scaleType = ImageView.ScaleType.FIT_CENTER
            val inset = theme.dp((BUTTON_DP - ICON_DP) / 2)
            setPadding(inset, inset, inset, inset)
            background = roundedFill(fill, theme.dpf(Tokens.Radius.chip))
            contentDescription = context.getString(description)
            setOnClickListener { onClick() }
            pressFeedback()
            layoutParams = LayoutParams(theme.dp(BUTTON_DP), theme.dp(BUTTON_DP)).apply { topMargin = theme.dp(GAP_DP) }
        }

    private fun setupHandle() {
        handle.setImageResource(R.drawable.ic_drag)
        handle.imageTintList = ColorStateList.valueOf(theme.textTertiary)
        handle.scaleType = ImageView.ScaleType.FIT_CENTER
        handle.contentDescription = context.getString(R.string.panel_drag)
        handle.layoutParams = LayoutParams(theme.dp(BUTTON_DP), theme.dp(HANDLE_DP))
        handle.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    actions.onDrag((event.rawX - lastX).toInt(), (event.rawY - lastY).toInt())
                    lastX = event.rawX
                    lastY = event.rawY
                    true
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    actions.onDragEnd()
                    true
                }

                else -> {
                    false
                }
            }
        }
    }

    private fun toggleCollapsed() {
        collapsed = !collapsed
        collapse.setImageResource(if (collapsed) R.drawable.ic_chevron_right else R.drawable.ic_chevron_left)
        collapse.contentDescription =
            context.getString(if (collapsed) R.string.panel_expand else R.string.panel_collapse)
        lastModel?.let(::render)
    }

    fun render(model: PanelModel) {
        lastModel = model
        val state = model.state
        val editing = state is EngineState.Idle || state is EngineState.Blocked
        val running = state is EngineState.Running || state is EngineState.Paused
        setText(status, model.status)
        setText(counters, model.counters)
        hud.show(model.hud.takeIf { running })
        toggle.visibility = visible(editing || running)
        toggle.setImageResource(if (state is EngineState.Running) R.drawable.ic_pause else R.drawable.ic_play)
        toggle.contentDescription =
            context.getString(
                when (state) {
                    is EngineState.Running -> R.string.panel_pause
                    is EngineState.Paused -> R.string.panel_resume
                    else -> R.string.panel_start
                },
            )
        stop.visibility = visible(running || state is EngineState.Countdown)
        cancelWait.visibility = visible(state is EngineState.Waiting)
        listOf(addTap, addSwipe, addCurve, addImage).forEach { it.visibility = visible(editing && !collapsed) }
        collapse.visibility = visible(editing)
    }

    private fun setText(
        view: TextView,
        text: String,
    ) {
        view.text = text
        view.visibility = visible(text.isNotBlank())
    }

    private fun visible(show: Boolean): Int = if (show) View.VISIBLE else View.GONE

    override fun onDraw(canvas: Canvas) {
        val card = RectF(shadowPad, shadowPad, width - shadowPad, height - shadowPad)
        canvas.drawRoundRect(card, radius, radius, cardPaint)
        canvas.drawRoundRect(card, radius, radius, borderPaint)
        super.onDraw(canvas)
    }

    private companion object {
        const val BUTTON_DP = 40
        const val ICON_DP = 20
        const val HANDLE_DP = 24
        const val GAP_DP = 4
        const val INNER_PADDING_DP = 6
        const val LABEL_SP = 12f
        const val LABEL_MAX_WIDTH_DP = 150
    }
}
