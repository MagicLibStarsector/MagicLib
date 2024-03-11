package org.magiclib.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.util.Misc;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.magiclib.util.MagicVariables;

import java.util.Map;

import static org.magiclib.util.MagicTxt.nullStringIfEmpty;

/**
 * Creates a captain PersonAPI.
 * <p>
 * Not all fields are required. Each `set` method has a comment showing the default value for if it is not used.
 * <p>
 * Usage:
 * <pre>
 * MagicCampaign.createCaptainBuilder(Factions.LUDDIC_CHURCH)
 *             .setFirstName("David")
 *             .setLastName("Rengel")
 *             .setGender(FullName.Gender.MALE)
 *             .create();
 * </pre>
 *
 * @since 0.46.1
 */
public class MagicCaptainBuilder {
    protected static Logger log = Global.getLogger(MagicCaptainBuilder.class);

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
        return createCaptain(
                Boolean.TRUE.equals(isAI),
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
     * Default: randomly chosen from faction.
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

    /**
     * Creates a captain PersonAPI
     *
     * @param isAI
     * @param AICoreType          AI core from campaign.ids.Commodities
     * @param firstName
     * @param lastName
     * @param portraitId          id of the sprite in settings.json/graphics/characters
     * @param gender,             any is gender-neutral, null is random male/female to avoid oddities and issues with dialogs and random portraits
     * @param factionId
     * @param rankId              rank from campaign.ids.Ranks
     * @param postId              post from campaign.ids.Ranks
     * @param personality         personality from campaign.ids.Personalities
     * @param level               Captain level, pick random skills according to the faction's doctrine
     * @param eliteSkillsOverride Overrides the regular number of elite skills, set to -1 to ignore.
     * @param skillPreference     GENERIC, PHASE, CARRIER, ANY from OfficerManagerEvent.SkillPickPreference
     * @param skillLevels         Map <skill, level> Optional skills from campaign.ids.Skills and their appropriate levels, OVERRIDES ALL RANDOM SKILLS PREVIOUSLY PICKED
     */
    private static PersonAPI createCaptain(
            boolean isAI,
            @Nullable String AICoreType,
            @Nullable String firstName,
            @Nullable String lastName,
            @Nullable String portraitId,
            @Nullable FullName.Gender gender,
            @NotNull String factionId,
            @Nullable String rankId,
            @Nullable String postId,
            @Nullable String personality,
            @Nullable Integer level,
            @Nullable Integer eliteSkillsOverride,
            @Nullable OfficerManagerEvent.SkillPickPreference skillPreference,
            @Nullable Map<String, Integer> skillLevels
    ) {

        if (eliteSkillsOverride == null)
            eliteSkillsOverride = 0;

        if (skillLevels != null && !skillLevels.isEmpty() && (level == null || level < 1)) {
            level = skillLevels.size();
            eliteSkillsOverride = 0;
            for (String s : skillLevels.keySet()) {
                if (skillLevels.get(s) == 2) eliteSkillsOverride++;
            }
        }

        if (level == null)
            level = 1;

        if (skillPreference == null) {
            skillPreference = OfficerManagerEvent.SkillPickPreference.ANY;
        }

        PersonAPI person = OfficerManagerEvent.createOfficer(
                Global.getSector().getFaction(factionId),
                level,
                skillPreference,
                false,
                null,
                true,
                eliteSkillsOverride != 0,
                eliteSkillsOverride,
                Misc.random
        );

        //try to create a default character of the proper gender if needed
        if (gender != null && gender != FullName.Gender.ANY && person.getGender() != gender) {
            for (int i = 0; i < 10; i++) {
                person = OfficerManagerEvent.createOfficer(
                        Global.getSector().getFaction(factionId),
                        level,
                        skillPreference,
                        false,
                        null,
                        true,
                        eliteSkillsOverride != 0,
                        eliteSkillsOverride,
                        Misc.random
                );
                if (person.getGender() == gender) break;
            }
        }

        if (gender != null && gender == FullName.Gender.ANY) {
            person.setGender(FullName.Gender.ANY);
        }

        if (isAI) {
            person.setAICoreId(AICoreType);
            person.setGender(FullName.Gender.ANY);
        }

        if (firstName != null) {
            person.getName().setFirst(firstName);
        }

        if (lastName != null) {
            person.getName().setLast(lastName);
        }

        if (MagicVariables.verbose) {
            log.info(" ");
            log.info(" Creating captain " + person.getNameString());
            log.info(" ");
        }

        if (nullStringIfEmpty(portraitId) != null) {
            if (portraitId.startsWith("graphics")) {
                if (Global.getSettings().getSprite(portraitId) != null) {
                    person.setPortraitSprite(portraitId);
                } else {
                    log.warn("Missing portrait at " + portraitId);
                }
            } else {
                if (Global.getSettings().getSprite("characters", portraitId) != null) {
                    person.setPortraitSprite(Global.getSettings().getSpriteName("characters", portraitId));
                } else {
                    log.warn("Missing portrait id " + portraitId);
                }
            }
        }

        if (nullStringIfEmpty(personality) != null) {
            person.setPersonality(personality);
        }
        if (MagicVariables.verbose) {
            log.info("     They are " + person.getPersonalityAPI().getDisplayName());
        }

        if (nullStringIfEmpty(rankId) != null) {
            person.setRankId(rankId);
        } else {
            person.setRankId(Ranks.SPACE_COMMANDER);
        }

        if (nullStringIfEmpty(postId) != null) {
            person.setPostId(postId);
        } else {
            person.setPostId(Ranks.POST_FLEET_COMMANDER);
        }

        //reset and reatribute skills if needed
        if (skillLevels != null && !skillLevels.isEmpty()) {
            if (MagicVariables.verbose) {

                //reset
                for (MutableCharacterStatsAPI.SkillLevelAPI skill : person.getStats().getSkillsCopy()) {
                    if (!skillLevels.keySet().contains(skill.getSkill().getId())) {
                        person.getStats().setSkillLevel(skill.getSkill().getId(), 0);
                    }
                }
                //reassign
                for (String skill : skillLevels.keySet()) {
                    person.getStats().setSkillLevel(skill, skillLevels.get(skill));
                }
                //log

                log.info("     " + "level effective: " + person.getStats().getLevel());
                log.info("     " + "level requested: " + level);
                for (MutableCharacterStatsAPI.SkillLevelAPI skill : person.getStats().getSkillsCopy()) {
                    if (skill.getSkill().isAptitudeEffect()) continue;
                    if (skill.getLevel() > 0) {
                        log.info("     " + skill.getSkill().getName() + " (" + skill.getSkill().getId() + ") : " + skill.getLevel());
                    }
                }

                /*
                for (SkillLevelAPI skill : person.getStats().getSkillsCopy()){
                    if(skillLevels.keySet().contains(skill.getSkill().getId())){
                        person.getStats().setSkillLevel(skill.getSkill().getId(),skillLevels.get(skill.getSkill().getId()));
                        log.info("     "+ skill.getSkill().getName() +" : "+ skillLevels.get(skill.getSkill().getId()));
                    } else {
                        person.getStats().setSkillLevel(skill.getSkill().getId(),0);
                        log.info("     "+ skill.getSkill().getName() +" : 0");
                    }
                }
                */
            } else {
                //reset
                for (MutableCharacterStatsAPI.SkillLevelAPI skill : person.getStats().getSkillsCopy()) {
                    person.getStats().setSkillLevel(skill.getSkill().getId(), 0);
                }
                //reassign
                for (String skill : skillLevels.keySet()) {
                    person.getStats().setSkillLevel(skill, skillLevels.get(skill));
                }
                /*
                for (SkillLevelAPI skill : person.getStats().getSkillsCopy()){
                    if(skillLevels.keySet().contains(skill.getSkill().getId())){
                        person.getStats().setSkillLevel(skill.getSkill().getId(),skillLevels.get(skill.getSkill().getId()));
                    } else {
                        person.getStats().setSkillLevel(skill.getSkill().getId(),0);
                    }
                }
                */
            }
            person.getStats().refreshCharacterStatsEffects();
        } else if (MagicVariables.verbose) {
            // list assigned random skills
            log.info("     " + "level: " + person.getStats().getLevel());
            for (MutableCharacterStatsAPI.SkillLevelAPI skill : person.getStats().getSkillsCopy()) {
                if (skill.getSkill().isAptitudeEffect()) continue;
                if (skill.getLevel() > 0) {
                    log.info("     " + skill.getSkill().getName() + " (" + skill.getSkill().getId() + ") : " + skill.getLevel());
                }
            }
        }

        return person;
    }
}
