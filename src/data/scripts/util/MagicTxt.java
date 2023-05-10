/*
By Tartiflette
 */
package data.scripts.util;

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

import static data.scripts.util.MagicVariables.MAGICLIB_ID;

@Deprecated
public class MagicTxt {

    public static String getString(String id) {
        return Global.getSettings().getString(MAGICLIB_ID, id);
    }

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

        MagicDisplayableText magicText = new MagicDisplayableText(str);

        text.addPara(
                magicText.format,
                textColor,
                highlightColor,
                magicText.highlights
        );
    }

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

        MagicDisplayableText magicText = new MagicDisplayableText(str);

        text.addPara(
                magicText.format,
                padding,
                textColor,
                highlightColor,
                magicText.highlights
        );
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