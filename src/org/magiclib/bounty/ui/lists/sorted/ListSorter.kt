package org.magiclib.bounty.ui.lists.sorted

import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI

interface ListSorter<T: Sortable<T>, V> {
    fun createPanel(tooltip: TooltipMakerAPI, width: Float, lastItems: List<Sortable<T>>): CustomPanelAPI

    fun saveToPersistentData()
    fun loadFromPersistentData(members: List<T>)

    fun isActive(): Boolean
}

abstract class SortableParam<T, V>(val item: T) {
    abstract fun getData(): V?
}

interface Sortable<T> {
    fun getSorterData(): List<SortableParam<T, *>>

    fun getSortIndex(): Int = 1
    fun getSortIndexOffset(): Int
    fun setSortIndexOffset(value: Int)
}