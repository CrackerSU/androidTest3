package com.example.androidtest3

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class MyAdapter(
    private val context: Context,
    private val dataList: List<ListItem>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)

    override fun getCount(): Int = dataList.size

    override fun getItem(position: Int): Any = dataList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item, parent, false)
            viewHolder = ViewHolder()
            viewHolder.imageView = view.findViewById(R.id.iv_icon)
            viewHolder.titleView = view.findViewById(R.id.tv_title)
            viewHolder.descView = view.findViewById(R.id.tv_description)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = convertView.tag as ViewHolder
        }

        val item = dataList[position]
        viewHolder.imageView.setImageResource(item.imageResId)
        viewHolder.titleView.text = item.title
        viewHolder.descView.text = item.description

        return view
    }

    private class ViewHolder {
        lateinit var imageView: ImageView
        lateinit var titleView: TextView
        lateinit var descView: TextView
    }
}