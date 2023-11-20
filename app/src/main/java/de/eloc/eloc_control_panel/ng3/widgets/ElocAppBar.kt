package de.eloc.eloc_control_panel.ng3.widgets

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.AppBarLayout
import de.eloc.eloc_control_panel.R
import de.eloc.eloc_control_panel.databinding.LayoutAppBarBinding
import de.eloc.eloc_control_panel.ng3.interfaces.VoidCallback

class ElocAppBar : AppBarLayout {
    private lateinit var binding: LayoutAppBarBinding
    private var onMenuButtonClickListener: VoidCallback? = null
    private var onSettingsButtonClickListener: VoidCallback? = null
    private var onBackButtonClickListener: VoidCallback? = null

    private var titleOpacity = 1.0f
        set(value) {
            field = getSanitizedOpacity(value)
            binding.titleTextView.alpha = field
        }

    private var nameOpacity = 1.0f
        set(value) {
            field = getSanitizedOpacity(value)
            binding.userLabel.alpha = field
        }

    private var backButtonOpacity = 1.0f
        set(value) {
            field = getSanitizedOpacity(value)
            binding.backButton.alpha = field
        }

    private var showBackButton = false
        set(value) {
            field = value
            binding.backButton.visibility = if (field) VISIBLE else GONE
            if (menuButtonPosition == MenuButtonPosition.Left) {
                menuButtonPosition = MenuButtonPosition.None
            }
        }

    var menuButtonPosition = MenuButtonPosition.None
        set(value) {
            field = value
            binding.leftMenuButton.visibility = GONE
            binding.rightMenuButton.visibility = GONE
            binding.wideTitlePadder.visibility = VISIBLE
            binding.narrowTitlePadder.visibility = GONE
            when (field) {
                MenuButtonPosition.Left -> {
                    binding.wideTitlePadder.visibility = GONE
                    binding.narrowTitlePadder.visibility = VISIBLE
                    binding.leftMenuButton.visibility = VISIBLE
                }

                MenuButtonPosition.Right -> binding.rightMenuButton.visibility = VISIBLE
                else -> {}
            }
        }

    var title = ""
        set(value) {
            field = value.trim()
            binding.titleTextView.text = field
        }

    var userName = ""
        set(value) {
            field = value.trim()
            binding.userLabel.text = context.getString(R.string.eloc_user, field)
        }

    private var showUserName = false
        set(value) {
            field = value
            binding.userLabel.visibility = if (field) {
                VISIBLE
            } else {
                GONE
            }
        }

    var showSettingsButton = false
        set(value) {
            field = value
            binding.settingsButton.visibility = if (field) {
                VISIBLE
            } else {
                GONE
            }
        }

    constructor(context: Context) : super(context) {
        initView()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initView(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        initView(attrs)
    }

    private fun initView(attrs: AttributeSet? = null) {
        val view = inflate(context, R.layout.layout_app_bar, this)
        binding = LayoutAppBarBinding.bind(view)
        elevation = resources.displayMetrics.density * 16.0f // 16dp

        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ElocAppBar)
        title = typedArray.getString(R.styleable.ElocAppBar_title) ?: ""
        titleOpacity = typedArray.getFloat(R.styleable.ElocAppBar_titleOpacity, 1.0f)
        nameOpacity = typedArray.getFloat(R.styleable.ElocAppBar_nameOpacity, 1.0f)
        backButtonOpacity = typedArray.getFloat(R.styleable.ElocAppBar_backButtonOpacity, 1.0f)
        showBackButton = typedArray.getBoolean(R.styleable.ElocAppBar_show_back_button, false)
        showSettingsButton =
            typedArray.getBoolean(R.styleable.ElocAppBar_show_settings_button, false)
        showUserName = typedArray.getBoolean(R.styleable.ElocAppBar_show_user_name, false)
        val menuPositionValue = typedArray.getInt(
            R.styleable.ElocAppBar_menu_button_position,
            MenuButtonPosition.None.value
        )
        menuButtonPosition = MenuButtonPosition.parse(menuPositionValue)
        typedArray.recycle()

        setListeners()
    }

    private fun setListeners() {
        binding.settingsButton.setOnClickListener { onSettingsButtonClickListener?.handler() }
        binding.leftMenuButton.setOnClickListener { onMenuButtonClickListener?.handler() }
        binding.rightMenuButton.setOnClickListener { onMenuButtonClickListener?.handler() }
        binding.backButton.setOnClickListener { onBackButtonClickListener?.handler() }
    }

    fun setOnMenuButtonClickedListener(callback: VoidCallback?) {
        onMenuButtonClickListener = callback
    }

    fun setOnBackButtonClickedListener(callback: VoidCallback?) {
        onBackButtonClickListener = callback
    }

    fun setOnSettingsButtonClickedListener(callback: VoidCallback?) {
        onSettingsButtonClickListener = callback
    }

    private fun getSanitizedOpacity(opacity: Float) = if (opacity >= 1.0) {
        1.0f
    } else if (opacity <= 0.0) {
        0.0f
    } else {
        opacity
    }

    enum class MenuButtonPosition(val value: Int) {
        // Match values in R.styleable.ElocAppBar_menu_button_position
        None(0), Left(1), Right(2);

        companion object {
            fun parse(i: Int): MenuButtonPosition {
                return when (i) {
                    1 -> Left
                    2 -> Right
                    else -> None
                }
            }
        }
    }
}