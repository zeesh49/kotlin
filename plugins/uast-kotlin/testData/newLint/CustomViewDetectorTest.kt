package test.pkg

import android.content.Context
import android.util.AttributeSet
import android.widget.Button
import android.widget.LinearLayout

class CustomView1(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : Button(context, attrs, defStyleAttr) {
    init {
        // OK
        context.obtainStyledAttributes(R.styleable.CustomView1)
        context.obtainStyledAttributes(defStyleRes, R.styleable.CustomView1)
        context.obtainStyledAttributes(attrs, R.styleable.CustomView1)
        context.obtainStyledAttributes(attrs, R.styleable.CustomView1, defStyleAttr, defStyleRes)

        // Wrong:
        context.obtainStyledAttributes(R.styleable.MyDeclareStyleable)
        context.obtainStyledAttributes(defStyleRes, R.styleable.MyDeclareStyleable)
        context.obtainStyledAttributes(attrs, R.styleable.MyDeclareStyleable)
        context.obtainStyledAttributes(attrs, R.styleable.MyDeclareStyleable, defStyleAttr,
                                       defStyleRes)

        // Unknown: Not flagged
        val dynamic = styleable
        context.obtainStyledAttributes(dynamic)
        context.obtainStyledAttributes(defStyleRes, dynamic)
        context.obtainStyledAttributes(attrs, dynamic)
        context.obtainStyledAttributes(attrs, dynamic, defStyleAttr, defStyleRes)
    }

    private val styleable: IntArray
        get() = IntArray(0)

    class MyLayout(context: Context, attrs: AttributeSet, defStyle: Int) : LinearLayout(context, attrs, defStyle) {
        init {
            context.obtainStyledAttributes(R.styleable.MyLayout)
        }

        class MyLayoutParams(context: Context, attrs: AttributeSet) : LinearLayout.LayoutParams(context, attrs) {
            init {
                context.obtainStyledAttributes(R.styleable.MyLayout_Layout) // OK
                context.obtainStyledAttributes(R.styleable.MyLayout) // Wrong
                context.obtainStyledAttributes(R.styleable.MyDeclareStyleable) // Wrong
            }
        }
    }

    class R {
        object attr {
            val layout_myWeight = 0x7f010001
            val myParam = 0x7f010000
        }

        object dimen {
            val activity_horizontal_margin = 0x7f040000
            val activity_vertical_margin = 0x7f040001
        }

        object drawable {
            val ic_launcher = 0x7f020000
        }

        object id {
            val action_settings = 0x7f080000
        }

        object layout {
            val activity_my = 0x7f030000
        }

        object menu {
            val my = 0x7f070000
        }

        object string {
            val action_settings = 0x7f050000
            val app_name = 0x7f050001
            val hello_world = 0x7f050002
        }

        object style {
            val AppTheme = 0x7f060000
        }

        object styleable {
            val CustomView1 = intArrayOf()
            val MyDeclareStyleable = intArrayOf()
            val MyLayout = intArrayOf(0x010100c4, 0x7f010000)
            val MyLayout_android_orientation = 0
            val MyLayout_myParam = 1
            val MyLayout_Layout = intArrayOf(0x010100f4, 0x010100f5, 0x7f010001)
            val MyLayout_Layout_android_layout_height = 1
            val MyLayout_Layout_android_layout_width = 0
            val MyLayout_Layout_layout_myWeight = 2
        }
    }
}
