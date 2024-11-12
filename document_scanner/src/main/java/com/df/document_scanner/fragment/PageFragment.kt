package com.df.document_scanner.fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.df.document_scanner.R

class PageFragment : Fragment() {

    private var imageView: ImageView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_page, container, false)

        // Initialize ImageView
        imageView = view.findViewById(R.id.pageImage)

        // Get the image path from arguments (if passed) and load the image
        val imagePath = arguments?.getString("imagePath")
        imagePath?.let { loadImage(it) }

        return view
    }

    private fun loadImage(imagePath: String) {
        // Load the image using Glide
        Glide.with(this)
            .load(imagePath)
            .into(imageView!!)
    }

    companion object {
        // Factory method to create a new instance of the fragment with arguments
        fun newInstance(imagePath: String) = PageFragment().apply {
            arguments = Bundle().apply {
                putString("imagePath", imagePath)
            }
        }
    }
}

