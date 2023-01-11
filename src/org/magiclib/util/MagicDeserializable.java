package org.magiclib.util;

/**
 * An interface that implements the "magic" method used by Starsector's xml serializer (XStream) whenever the object is
 * deserialized (created from xml).
 * <p>
 * Note: implementing this interface does nothing. It is merely a convenient way to remember and implement the magic method
 * (`readResolve`) that is used by Starsector.
 * Implementing `readResolve` without using `MagicDeserializable` will work exactly the same.
 */
public interface MagicDeserializable {

    /**
     * A method that is automatically called by XStream when a class is instantiated and populated via deserialization.
     *
     * @return This should return your class; `return this;`.
     */
    Object readResolve();
}
