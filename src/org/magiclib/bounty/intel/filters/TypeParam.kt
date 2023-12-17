package org.magiclib.bounty.intel.filters

import org.magiclib.bounty.intel.BountyInfo
import org.magiclib.bounty.ui.lists.filtered.FilterableParam

class TypeParam(item: BountyInfo): FilterableParam<BountyInfo, String>(item) {
    override fun getData(): String {
        return item.getBountyType()
    }
}