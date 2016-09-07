package test.pkg

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.TextView

@SuppressWarnings("ClassNameDiffersFromFileName", "unused")
class RecyclerViewTest {
    abstract class Test1(private val mDataset: Array<String>) : RecyclerView.Adapter<Test1.ViewHolder>() {
        class ViewHolder(var mTextView: TextView) : RecyclerView.ViewHolder(mTextView)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.mTextView.text = mDataset[position] // OK
        }
    }

    abstract class Test2 : RecyclerView.Adapter<Test2.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // OK
        }
    }

    abstract class Test3 : RecyclerView.Adapter<Test3.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            // OK - final, but not referenced

        }
    }

    abstract class Test4 : RecyclerView.Adapter<Test4.ViewHolder>() {
        private var myCachedPosition: Int = 0

        class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            myCachedPosition = position // ERROR: escapes
        }
    }

    abstract class Test5 : RecyclerView.Adapter<Test5.ViewHolder>() {
        class ViewHolder(v: View) : RecyclerView.ViewHolder(v)

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            object : Runnable {
                override fun run() {
                    println(position) // ERROR: escapes
                }
            }.run()
        }

        // https://code.google.com/p/android/issues/detail?id=172335
        abstract class Test6 : RecyclerView.Adapter<Test6.ViewHolder>() {
            internal var myData: List<String>? = null

            class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
                private val itemView: View? = null
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                holder.itemView!!.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(view: View) {
                        myData!![position] // ERROR
                    }
                })
            }

            override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
                holder.itemView.setOnClickListener(object : View.OnClickListener {
                    public override fun onClick(view: View) {
                        myData!![position] // ERROR
                    }
                })
            }
        }

        abstract class Test7 : RecyclerView.Adapter<Test7.ViewHolder>() {
            internal var myData: List<String>? = null

            class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
                private val itemView: View? = null
            }

            // ERRROR, but SAM constructors are not supported now
            override fun onBindViewHolder(holder: ViewHolder, position: Int) {
                holder.itemView!!.setOnClickListener { myData!![position] }
            }
        }
    }
}