package org.magiclib.graphics;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.graphics.SpriteAPI;
import org.lazywizard.lazylib.MathUtils;

/**
 * Samples a single sprite from a tilesheet of multiple symbols.
 * <p>
 * Example of using it to get hyperspace terrain sprites:
 * <pre>
 *     MagicTiledSpriteSampler mySampler = new MagicTiledSpriteSampler("hyperspace", "deep_hyperspace", 4, 4);
 *     SpriteAPI sprite = mySampler.getSprite();
 * </pre>
 *
 * @author Toaster
 */
@SuppressWarnings("unused")
public class MagicTiledSpriteSampler {
    protected final String category;
    /// The category in the graphics section.
    protected final String key;
    /// The key for the sprite in that category.
    protected final int tilesX;
    /// Number of horizontal tiles on the sprite. Adjusted to 0-index internally.
    protected final int tilesY;
    /// Number of vertical tiles on the sprite. Adjusted to 0-index internally.
    protected final float texWidth;
    /// The width fraction of 1 tile.
    protected final float texHeight;  /// The height fraction of 1 tile.

    /**
     * Creates a new sprite sampler for a sprite sheet.
     *
     * @param category The graphics subcategory in settings.json.
     * @param key      The key for the sprite.
     * @param tilesX   How many tiles in the X direction.
     * @param tilesY   How many tiles in the Y direction.
     */
    public MagicTiledSpriteSampler(
            String category, String key, int tilesX, int tilesY
    ) {
        this.category = category;
        this.key = key;
        this.tilesX = tilesX - 1;
        this.tilesY = tilesY - 1;
        this.texWidth = 1f / tilesX;
        this.texHeight = 1f / tilesY;
    }

    /**
     * Returns a random sprite from the tiles.
     *
     * @return A sample from a tiled sprite.
     */
    public SpriteAPI getSprite() {
        SpriteAPI sprite = Global.getSettings().getSprite(this.category, this.key);
        sprite.setTexWidth(this.texWidth);
        sprite.setTexHeight(this.texHeight);
        sprite.setTexX(this.texWidth * MathUtils.getRandomNumberInRange(0, this.tilesX));
        sprite.setTexY(this.texHeight * MathUtils.getRandomNumberInRange(0, this.tilesY));
        return sprite;
    }
}
