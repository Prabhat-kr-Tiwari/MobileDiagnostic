package com.example.mobilediagnostic

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.example.mobilediagnostic.databinding.ItemBinding

class DiagnosticAdapter(
    val context:
    Context, private var list: List<model>, private val viewPager2: ViewPager2
) : RecyclerView.Adapter<DiagnosticAdapter.ViewHolder>() {
    private var showBlackCircle = false
    private var showGreenCircle = false
    private val visibilityStates = mutableListOf<Triple<Boolean, Boolean, Boolean>>()

    init {
        for (i in list.indices) {
            visibilityStates.add(Triple(true, false, false))
        }
    }

    inner class ViewHolder(binding: ItemBinding) : RecyclerView.ViewHolder(binding.root) {

        val image = binding.image
        val title = binding.title

        val outerBlackCircle: ImageView = itemView.findViewById(R.id.outer_black_circle)
        val outerGreenCircle: ImageView = itemView.findViewById(R.id.outer_green_circle)
        val greenTick: ImageView = itemView.findViewById(R.id.checked_green_circle)
    }


    fun setImageViewVisibility(
        position: Int,
        showBlackCircle: Boolean,
        showGreenCircle: Boolean,
        greenTick: Boolean
    ) {
        visibilityStates[position] = Triple(showBlackCircle, showGreenCircle, greenTick)
        notifyItemChanged(position)
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            ItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val model = list[position]
        holder.title.text = model.title
//        holder.image.drawable=model.image
        Glide.with(context).load(model.image).into(holder.image)

        val (showBlackCircle, showGreenCircle, greenTick) = visibilityStates[position]
        holder.outerBlackCircle.visibility = if (showBlackCircle) View.VISIBLE else View.INVISIBLE
        holder.outerGreenCircle.visibility = if (showGreenCircle) View.VISIBLE else View.INVISIBLE
        holder.greenTick.visibility = if (greenTick) View.VISIBLE else View.INVISIBLE


    }


}