package data.scripts.util;

/**
 * An interface that implements the "magic" method used by Starsector's xml serializer (XStream) whenever the object is
 * deserialized (created from xml).
 */
public interface MagicDeserializable {

    /**
     * A method that is automatically called by XStream when a class is instantiated and populated via deserialization.
     *
     * @return This should return your class; `return this;`.
     */
    Object readResolve();
}
