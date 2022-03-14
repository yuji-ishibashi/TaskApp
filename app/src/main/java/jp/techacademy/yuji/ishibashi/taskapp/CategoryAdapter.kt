package jp.techacademy.yuji.ishibashi.taskapp

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class CategoryAdapter(context: Context): BaseAdapter() {
    private val TAG: String = "CategoryAdapter"
    private val mLayoutInflater: LayoutInflater
    var mCategoryList= mutableListOf<Category>()

    init {
        this.mLayoutInflater = LayoutInflater.from(context)
    }

    override fun getCount(): Int {
        return mCategoryList.size
    }

    override fun getItem(position: Int): Any {
        return mCategoryList[position]
    }

    override fun getItemId(position: Int): Long {
        return mCategoryList[position].id.toLong()
    }

    /**
     * 指定したカテゴリーの位置を返却する関数
     */
    fun getPosition(element: Category): Int {
        return mCategoryList.indexOfFirst { it.id == element.id }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View = convertView ?: mLayoutInflater.inflate(android.R.layout.simple_list_item_1, null)

        val textView1 = view.findViewById<TextView>(android.R.id.text1)

        textView1.text = mCategoryList[position].categoryName

        return view
    }
}