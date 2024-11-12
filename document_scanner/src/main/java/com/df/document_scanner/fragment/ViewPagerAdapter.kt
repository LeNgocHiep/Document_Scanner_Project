package com.df.document_scanner.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity, private val pages: MutableList<String>) :
    FragmentStateAdapter(fragmentActivity) {
    private var listFragment = mutableListOf<PageFragment>()

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment {
        return listFragment[position]
    }

    fun onCreated() {
        listFragment = pages.map { PageFragment.newInstance(it) }.toMutableList()
    }

    fun update(position: Int, path: String) {
        listFragment.removeAt(position)
        listFragment.add(position, PageFragment.newInstance(path))
        notifyItemChanged(position)
    }

    fun remove(position: Int) {
        listFragment.removeAt(position)
        notifyItemRemoved(position)
    }

    override fun getItemId(position: Int): Long {
        return pages[position].hashCode().toLong()
    }

    override fun containsItem(itemId: Long): Boolean {
        return pages.find { it.hashCode().toLong() == itemId } != null
    }
}
