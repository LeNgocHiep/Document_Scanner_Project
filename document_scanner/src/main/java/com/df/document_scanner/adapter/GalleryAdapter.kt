package com.df.document_scanner.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.df.document_scanner.R
import com.google.android.material.imageview.ShapeableImageView

class GalleryAdapter(
    private val images: List<String>,
    private val onImageClick: (String, Int) -> Unit,
) : RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {

    private var currentSelectedPosition = 0

    class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ShapeableImageView = itemView.findViewById(R.id.galleryImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_image, parent, false)
        return GalleryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: GalleryViewHolder,
        @SuppressLint("RecyclerView") position: Int
    ) {
        if (currentSelectedPosition == this.itemCount) {
            currentSelectedPosition = this.itemCount - 1
        }
        // Load the image from the path using ImageUtil
        Glide.with(holder.itemView.context)
            .load(images[position]) // Load image from path
            .transform(RoundedCorners(10)) // Apply a rounded corner transformation
//            .placeholder(R.drawable.placeholder_image) // Optional: Placeholder while loading
//            .error(R.drawable.error_image) // Optional: Error image if failed to load
            .into(holder.imageView) // Set into ImageView

        // Set strokeWidth based on whether the item is selected
        holder.imageView.strokeWidth = if (position == currentSelectedPosition) 8f else 0f
        holder.imageView.cropToPadding = true

        // Set click listener
        holder.imageView.setOnClickListener {
            // Update selected position and notify adapter
            val previousPosition = currentSelectedPosition
            currentSelectedPosition = position
            notifyItemChanged(previousPosition) // Refresh previously selected item
            notifyItemChanged(currentSelectedPosition) // Refresh newly selected item
            if (images.size > position) {
                onImageClick(images[position], position) // Pass the image path to the click handler
            }
        }
    }

    override fun getItemCount(): Int = images.size

    fun updateSelectedPosition(position: Int) {
        val previousPosition = currentSelectedPosition
        currentSelectedPosition = position
        notifyItemChanged(previousPosition) // Refresh previously selected item
        notifyItemChanged(currentSelectedPosition)
    }
}
