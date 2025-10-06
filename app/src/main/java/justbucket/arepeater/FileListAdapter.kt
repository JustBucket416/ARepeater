package justbucket.arepeater

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import justbucket.arepeater.databinding.HolderBinding

class FileListAdapter(private val listener: (file: File) -> Unit) : ListAdapter<File, FileListAdapter.FileHolder>(FileDiffCallback()) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileHolder {
        return FileHolder(HolderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: FileHolder, position: Int) {
        holder.bind(getItem(position))
        holder.itemView.setOnClickListener { listener.invoke(getItem(position)) }
    }

    class FileDiffCallback : DiffUtil.ItemCallback<File>() {
        override fun areItemsTheSame(oldItem: File, newItem: File) = oldItem === newItem

        override fun areContentsTheSame(oldItem: File, newItem: File) = oldItem == newItem
    }

    class FileHolder(private val binding: HolderBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(file: File) {
            val img = if (file.isDirectory) R.drawable.folder else R.drawable.music_box
            val drawable = AppCompatResources.getDrawable(itemView.context, img)!!
            val size = (56 * itemView.resources.displayMetrics.density).toInt()
            drawable.setBounds(0, 0, size, size)
            binding.holderText.setCompoundDrawables(drawable, null, null, null)

            binding.holderText.text = file.nameWithoutExtension
        }
    }

}