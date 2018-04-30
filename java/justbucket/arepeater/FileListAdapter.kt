package justbucket.arepeater

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.holder.view.*
import java.io.File

internal class FileListAdapter(context: Context, private val listener: (file: File) -> Unit,
                               private val items: ArrayList<File>) : ArrayAdapter<File>(context, R.layout.holder, items) {

    private var inflater = LayoutInflater.from(context)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        return (convertView ?: inflater.inflate(R.layout.holder, parent, false)).also {
            val holder = ViewHolder().apply {
                layout = it.holderLay
                image = it.holderImage
                text = it.holderText
            }

            with(items[position]) {

                holder.layout.setOnClickListener { listener(this) }

                if (isDirectory) holder.image.setImageResource(R.drawable.folder)
                else /*if (extension == "mp3")*/ holder.image.setImageResource(R.drawable.music_box)

                holder.text.text = nameWithoutExtension
            }

            it.tag = holder
        }
    }

    inner class ViewHolder {
        lateinit var layout: LinearLayout
        lateinit var image: ImageView
        lateinit var text: TextView
    }

}