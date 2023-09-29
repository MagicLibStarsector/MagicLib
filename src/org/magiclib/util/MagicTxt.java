package org.magiclib.util;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains some String-related utility functions.
 *
 * @author Tartiflette, Wisp
 */
public class MagicTxt {

    public static String getString(String id) {
        return Global.getSettings().getString(MagicVariables.MAGICLIB_ID, id);
    }

    public static String getString(String id, String... args) {
        return String.format(Global.getSettings().getString(MagicVariables.MAGICLIB_ID, id), args);
    }

    /**
     * If the input is an empty string, returns null. Otherwise, returns the input.
     */
    public static String nullStringIfEmpty(@Nullable String input) {
        return input != null && !input.isEmpty() ? input : null;
    }

    /**
     * Replaces all instances of the given regex with the string returned from stringCreator.
     * The difference from normal String.replaceAll is that stringCreator is only run if a match is found.
     */
    public static String replaceAllIfPresent(String stringToReplace, String regex, StringCreator stringCreator) {
        if (stringToReplace.toLowerCase().contains(regex.toLowerCase())) {
            try {
                String replacement = stringCreator.create();
                return Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE).matcher(stringToReplace).replaceAll(replacement);
            } catch (Exception e) {
                Global.getLogger(MagicTxt.class).error("Error thrown while replacing " + stringToReplace, e);
                return Pattern.compile(Pattern.quote(regex), Pattern.CASE_INSENSITIVE).matcher(stringToReplace).replaceAll("null");
            }
        } else {
            return stringToReplace;
        }
    }

    /**
     * If the string is longer than the given length, returns the string truncated to the given length with "..." appended.
     * <p>
     * Note that "..." is 3 characters, so the returned string will be a max of length + 3.
     *
     * @since 1.3.0
     */
    public static String ellipsizeStringAfterLength(String str, int length) {
        if (str.length() <= length) {
            return str;
        } else {
            return str.substring(0, length) + "...";
        }
    }

    private static final Pattern highlightPattern = Pattern.compile("==(.*?)==", Pattern.DOTALL);
    private static final Pattern uppercaseFirstPattern = Pattern.compile(".*(?<!\\\\)\\^(.).*");

    /**
     * Takes a string with the format "This is a ==highlighted== sentence with ==words==."
     * <p>
     * Usage:
     * <pre>
     *         MagicDisplayableText magicText = new MagicDisplayableText(str);
     *
     *         text.addPara(
     *                 magicText.format,
     *                 textColor,
     *                 highlightColor,
     *                 magicText.highlights
     *         );
     *        </pre>
     */
    public static class MagicDisplayableText {
        /**
         * The text that was passed in as `str`.
         */
        public String originalText;
        /**
         * The text with the highlights replaced by '%s'.
         */
        public String format;
        /**
         * An array of the highlighted parts of the string.
         */
        public String[] highlights;

        public MagicDisplayableText(@NotNull String str) {
            this.originalText = str;
            this.format = replaceStringHighlightsWithSymbol(str);
            this.highlights = getTextMatches(str, highlightPattern);
        }
    }

    /**
     * Takes a string with the format "This is a ==highlighted word== string." and returns {@link MagicDisplayableText}.
     */
    public static MagicDisplayableText createMagicDisplayableText(@NotNull String str) {
        return new MagicDisplayableText(str);
    }

    /**
     * Uses {@link MagicDisplayableText} to add a paragraph to the given {@link TextPanelAPI}.
     * \n may be used to add multiple paragraphs.
     * You can use Misc.getTextColor() and Misc.getHighlightColor() to get default colors.
     */
    public static void addPara(
            @NotNull TextPanelAPI text,
            @Nullable String str,
            @NotNull Color textColor,
            @NotNull Color highlightColor
    ) {
        if (str == null || str.isEmpty()) {
            text.addPara("");
            return;
        }

        String[] paras = str.split("\\n");

        for (String para : paras) {
            MagicDisplayableText magicText = new MagicDisplayableText(para);

            text.addPara(
                    magicText.format,
                    textColor,
                    highlightColor,
                    magicText.highlights
            );
        }
    }

    /**
     * Uses {@link MagicDisplayableText} to add a paragraph to the given {@link TooltipMakerAPI}.
     * \n may be used to add multiple paragraphs.
     * You can use Misc.getTextColor() and Misc.getHighlightColor() to get default colors.
     */
    public static void addPara(
            @NotNull TooltipMakerAPI text,
            @Nullable String str,
            float padding,
            @NotNull Color textColor,
            @NotNull Color highlightColor
    ) {
        if (str == null || str.isEmpty()) {
            text.addPara("", padding);
            return;
        }

        String[] paras = str.split("\\n");

        for (String para : paras) {
            MagicDisplayableText magicText = new MagicDisplayableText(para);

            text.addPara(
                    magicText.format,
                    padding,
                    textColor,
                    highlightColor,
                    magicText.highlights
            );
        }
    }

    private static String replaceStringHighlightsWithSymbol(@NotNull String str) {
        String format = highlightPattern.matcher(str).replaceAll("%s");
        Matcher uppercaseMatcher = uppercaseFirstPattern.matcher(format);

        // Have to match the whole pattern because using find() ignores the negative lookbehind since it matches substrings.
        // Or something like that.
        while (uppercaseMatcher.matches()) {
            format = format.substring(0, uppercaseMatcher.start(1) - 1) + uppercaseMatcher.group(1).toUpperCase() + format.substring(uppercaseMatcher.end(1));
            uppercaseMatcher = uppercaseFirstPattern.matcher(format);
        }

        return format;
    }

    @NotNull
    private static String[] getTextMatches(@NotNull String str, Pattern pattern) {
        List<String> allMatches = new ArrayList<>();

        Matcher m = pattern.matcher(str);
        while (m.find()) {
            allMatches.add(m.group(1));
        }
        return allMatches.toArray(new String[0]);
    }
}