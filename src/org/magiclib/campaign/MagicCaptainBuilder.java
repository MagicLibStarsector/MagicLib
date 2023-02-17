package org.magiclib.campaign;

import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import data.scripts.util.MagicCampaign;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Creates a captain PersonAPI.
 * Usage:
 * <pre>new MagicCaptainBuilder("factionId").setFirstName("John").setLastName("Smith").create();</pre>
 *
 * @since 0.46.1
 */
public class MagicCaptainBuilder {
    private @Nullable Boolean isAI = false;
    private @Nullable String aiCoreType;
    private @Nullable String firstName;
    private @Nullable String lastName;
    private @Nullable String portraitId;
    private @Nullable FullName.Gender gender;
    private @NotNull String factionId;
    private @Nullable String rankId;
    private @Nullable String postId;
    private @Nullable String personality;
    private @Nullable Integer level = 0;
    private @Nullable Integer eliteSkillsOverride = 0;
    private @Nullable OfficerManagerEvent.SkillPickPreference skillPreference;
    private @Nullable Map<String, Integer> skillLevels;

    public MagicCaptainBuilder(@NotNull String factionId) {
        this.factionId = factionId;
    }

    public PersonAPI create() {
        return MagicCampaign.createCaptain(
                isAI,
                aiCoreType,
                firstName,
                lastName,
                portraitId,
                gender,
                factionId,
                rankId,
                postId,
                personality,
                level,
                eliteSkillsOverride,
                skillPreference,
                skillLevels);
    }

    /**
     * Default: false.
     */
    public MagicCaptainBuilder setIsAI(boolean isAI) {
        this.isAI = isAI;
        return this;
    }

    /**
     * AI core from campaign.ids.Commodities.
     * Default: none.
     */
    public MagicCaptainBuilder setAICoreType(@Nullable String aiCoreType) {
        this.aiCoreType = aiCoreType;
        return this;
    }

    /**
     * Default: randomly chosen from faction names.
     */
    public MagicCaptainBuilder setFirstName(@Nullable String firstName) {
        this.firstName = firstName;
        return this;
    }

    /**
     * Default: randomly chosen from faction names.
     */
    public MagicCaptainBuilder setLastName(@Nullable String lastName) {
        this.lastName = lastName;
        return this;
    }

    /**
     * Id of the sprite in settings.json/graphics/characters.
     * Default: randomly chosen from faction names.
     */
    public MagicCaptainBuilder setPortraitId(@Nullable String portraitId) {
        this.portraitId = portraitId;
        return this;
    }

    /**
     * Default: Randomly chosen between male and female if non-AI. Gender.ANY if AI.
     */
    public MagicCaptainBuilder setGender(@Nullable FullName.Gender gender) {
        this.gender = gender;
        return this;
    }

    public MagicCaptainBuilder setFactionId(@NotNull String factionId) {
        this.factionId = factionId;
        return this;
    }

    /**
     * Default: {@code Ranks.SPACE_COMMANDER}.
     */
    public MagicCaptainBuilder setRankId(@Nullable String rankId) {
        this.rankId = rankId;
        return this;
    }

    /**
     * Default: {@code Ranks.POST_FLEET_COMMANDER}.
     */
    public MagicCaptainBuilder setPostId(@Nullable String postId) {
        this.postId = postId;
        return this;
    }

    /**
     * Personality from {@code campaign.ids.Personalities}.
     * Default: randomly chosen.
     */
    public MagicCaptainBuilder setPersonality(@Nullable String personality) {
        this.personality = personality;
        return this;
    }

    /**
     * Captain level, picks random skills according to the faction's doctrine.
     * Default: Set from {@code skillLevels} if present. If {@code skillLevels} is not set, 1 is used.
     */
    public MagicCaptainBuilder setLevel(@Nullable Integer level) {
        this.level = level;
        return this;
    }

    /**
     * Overrides the regular number of elite skills, set to -1 to ignore.
     * Default: none.
     */
    public MagicCaptainBuilder setEliteSkillsOverride(@Nullable Integer eliteSkillsOverride) {
        this.eliteSkillsOverride = eliteSkillsOverride;
        return this;
    }

    /**
     * GENERIC, PHASE, CARRIER, ANY from {@code OfficerManagerEvent.SkillPickPreference}.
     * Default: {@code OfficerManagerEvent.SkillPickPreference.ANY}.
     */
    public MagicCaptainBuilder setSkillPreference(@Nullable OfficerManagerEvent.SkillPickPreference skillPreference) {
        this.skillPreference = skillPreference;
        return this;
    }

    /**
     * Map <skill, level> Optional skills from campaign.ids.Skills and their appropriate levels,
     * OVERRIDES ALL RANDOM SKILLS PREVIOUSLY PICKED.
     * Default: none.
     */
    public MagicCaptainBuilder setSkillLevels(@Nullable Map<String, Integer> skillLevels) {
        this.skillLevels = skillLevels;
        return this;
    }
}
