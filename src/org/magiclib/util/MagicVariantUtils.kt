package org.magiclib.util

import com.fs.starfarer.api.combat.ShipVariantAPI
import org.magiclib.util.MagicVariantUtils.getPrefixedTag

object MagicVariantUtils {
    private fun prefixed(prefix: String) = "${prefix}="

    /**
     * Adds a string to the ShipVariantAPI tags with the input prefix (`prefix=string`).
     * Replaces any existing tag with the same prefix.
     *
     * See [getPrefixedTag] for the inverse operation.
     */
    @JvmStatic
    fun setPrefixedTag(variant: ShipVariantAPI, prefix: String, string: String) {
        val p = prefixed(prefix)
        variant.tags.removeIf { it.startsWith(p) }
        variant.addTag(p + string)
    }

    /**
     * Gets a string from a prefixed ShipVariantAPI tag (`prefix=string`).
     */
    @JvmStatic
    fun getPrefixedTag(variant: ShipVariantAPI, prefix: String): String? {
        val p = prefixed(prefix)
        return variant.tags.firstOrNull { it.startsWith(p) }?.removePrefix(p)
    }

    /**
     * Removes the ShipVariantAPI tag with the input prefix (`prefix=*`).
     */
    @JvmStatic
    fun removePrefixedTag(variant: ShipVariantAPI, prefix: String): Boolean {
        val p = prefixed(prefix)
        return variant.tags.removeIf { it.startsWith(p) }
    }

    /**
     * Checks for existence of a ShipVariantAPI tag with the input prefix (`prefix=*`).
     */
    @JvmStatic
    fun hasPrefixedTag(variant: ShipVariantAPI, prefix: String): Boolean {
        val p = prefixed(prefix)
        return variant.tags.any { it.startsWith(p) }
    }

}