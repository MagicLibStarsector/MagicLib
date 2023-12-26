package org.magiclib.bounty.ui.lists.filtered

import com.fs.starfarer.api.ui.CustomPanelAPI
import com.fs.starfarer.api.ui.TooltipMakerAPI

interface ListFilter<T: Filterable<T>, V> {
    fun matches(filterableParam: FilterableParam<T, V>): Boolean
    fun matches(filterData: List<FilterableParam<T, *>>): Boolean
    fun createPanel(tooltip: TooltipMakerAPI, width: Float, lastItems: List<Filterable<T>>): CustomPanelAPI

    fun saveToPersistentData()
    fun loadFromPersistentData()

    fun isActive(): Boolean
}

abstract class FilterableParam<T, V>(val item: T) {
    abstract fun getData(): V?
}

interface Filterable<T> {
    fun getFilterData(): List<FilterableParam<T, *>>
}