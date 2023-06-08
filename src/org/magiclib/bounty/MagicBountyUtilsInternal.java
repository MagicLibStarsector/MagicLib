package org.magiclib.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.FleetAssignment;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial;
import com.fs.starfarer.api.util.Misc;
import org.jetbrains.annotations.NotNull;
import org.magiclib.util.MagicTxt;
import org.magiclib.util.StringCreator;

/**
 * Do not use this.
 * It is only public because it is used in the org.magiclib.bounty.rulecmd package.
 */
public class MagicBountyUtilsInternal {


    /**
     * Replaces variables in the given string with data from the bounty and splits it into paragraphs using `\n`.
     */
    public static String replaceStringVariables(final ActiveBounty bounty, String text) {
        String replaced = text;

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$sonDaughterChild", new StringCreator() {
            @Override
            public String create() {
                switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE:
                        return MagicTxt.getString("mb_son");
                    case FEMALE:
                        return MagicTxt.getString("mb_daughter");
                    default:
                        return MagicTxt.getString("mb_child");
                }
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$fatherMotherParent", new StringCreator() {
            @Override
            public String create() {
                switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE:
                        return MagicTxt.getString("mb_father");
                    case FEMALE:
                        return MagicTxt.getString("mb_mother");
                    default:
                        return MagicTxt.getString("mb_parent");
                }
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$manWomanPerson", new StringCreator() {
            @Override
            public String create() {
                if (bounty.getFleet().getCommander().isAICore()) {
                    return MagicTxt.getString("mb_ai");
                } else {
                    switch (bounty.getFleet().getCommander().getGender()) {
                        case MALE:
                            return MagicTxt.getString("mb_man");
                        case FEMALE:
                            return MagicTxt.getString("mb_woman");
                        default:
                            return MagicTxt.getString("mb_person");
                    }
                }
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$hisHerTheir", new StringCreator() {
            @Override
            public String create() {
                if (bounty.getFleet().getCommander().isAICore()) {
                    return MagicTxt.getString("mb_its");
                } else {
                    switch (bounty.getFleet().getCommander().getGender()) {
                        case MALE:
                            return MagicTxt.getString("mb_his");
                        case FEMALE:
                            return MagicTxt.getString("mb_her");
                        default:
                            return MagicTxt.getString("mb_their");
                    }
                }
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$heSheThey", new StringCreator() {
            @Override
            public String create() {
                if (bounty.getFleet().getCommander().isAICore()) {
                    return MagicTxt.getString("mb_it");
                } else {
                    switch (bounty.getFleet().getCommander().getGender()) {
                        case MALE:
                            return MagicTxt.getString("mb_he");
                        case FEMALE:
                            return MagicTxt.getString("mb_she");
                        default:
                            return MagicTxt.getString("mb_they");
                    }
                }
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$heIsSheIsTheyAre", new StringCreator() {
            @Override
            public String create() {
                if (bounty.getFleet().getCommander().isAICore()) {
                    return MagicTxt.getString("mb_itIs");
                } else {
                    switch (bounty.getFleet().getCommander().getGender()) {
                        case MALE:
                            return MagicTxt.getString("mb_heIs");
                        case FEMALE:
                            return MagicTxt.getString("mb_sheIs");
                        default:
                            return MagicTxt.getString("mb_theyAre");
                    }
                }
            }
        });

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$himHerThem", new StringCreator() {
            @Override
            public String create() {
                if (bounty.getFleet().getCommander().isAICore()) {
                    return MagicTxt.getString("mb_it");
                } else {
                    switch (bounty.getFleet().getCommander().getGender()) {
                        case MALE:
                            return MagicTxt.getString("mb_him");
                        case FEMALE:
                            return MagicTxt.getString("mb_her");
                        default:
                            return MagicTxt.getString("mb_them");
                    }
                }
            }
        });

        StringCreator reflexivePronounStringCreator = new StringCreator() {
            @Override
            public String create() {
                if (bounty.getFleet().getCommander().isAICore()) {
                    return MagicTxt.getString("mb_itself");
                } else {
                    switch (bounty.getFleet().getCommander().getGender()) {
                        case MALE:
                            return MagicTxt.getString("mb_himself");
                        case FEMALE:
                            return MagicTxt.getString("mb_herself");
                        default:
                            return MagicTxt.getString("mb_themselves");
                    }
                }
            }
        };
        // Typo fixed in 0.46.0
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$himslefHerselfThemselves", reflexivePronounStringCreator);
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$himselfHerselfThemselves", reflexivePronounStringCreator);


        replaced = MagicTxt.replaceAllIfPresent(replaced, "$system", new StringCreator() {
            @Override
            public String create() {
                return bounty.getFleetSpawnLocation().getContainingLocation().getNameWithNoType();
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$shipName", new StringCreator() {
            @Override
            public String create() {
                return bounty.getFleet().getFlagship().getShipName();
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$location", new StringCreator() {
            @Override
            public String create() {
                if (bounty.getFleetSpawnLocation().getMarket() != null) {
                    return bounty.getFleetSpawnLocation().getMarket().getName();
                }
                return bounty.getFleetSpawnLocation().getFullName();
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$givingFaction", new StringCreator() {
            @Override
            public String create() {
                return bounty.getGivingFaction() != null
                        ? bounty.getGivingFaction().getDisplayNameWithArticle()
                        : "a faction";
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$rewardFaction", new StringCreator() {
            @Override
            public String create() {
                FactionAPI rewardFaction = Global.getSector().getFaction(bounty.getRewardFactionId());
                return rewardFaction != null
                        ? rewardFaction.getDisplayNameWithArticle()
                        : "a faction";
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$targetFaction", new StringCreator() {
            @Override
            public String create() {
                return bounty.getFleet().getFaction().getDisplayNameWithArticle();
            }
        });
        // Deprecated in 1.1.2
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$faction", new StringCreator() {
            @Override
            public String create() {
                return bounty.getFleet().getFaction().getDisplayNameWithArticle();
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$reward", new StringCreator() {
            @Override
            public String create() {
                return Misc.getDGSCredits(bounty.getSpec().job_credit_reward);
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$name", new StringCreator() {
            @Override
            public String create() {
                return bounty.getFleet().getCommander().getNameString();
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$firstName", new StringCreator() {
            @Override
            public String create() {
                return bounty.getFleet().getCommander().getName().getFirst();
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$lastName", new StringCreator() {
            @Override
            public String create() {
                return bounty.getFleet().getCommander().getName().getLast();
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$constellation", new StringCreator() {
            @Override
            public String create() {
                return bounty.getFleetSpawnLocation().getContainingLocation().getConstellation().getName();
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$shipFleet", new StringCreator() {
            @Override
            public String create() {
                if (bounty.getFleet().getFleetData().getMembersListCopy().size() == 1) {
                    return MagicTxt.getString("mb_var_ship");
                } else {
                    return MagicTxt.getString("mb_var_fleet");
                }
            }
        });

        //FAILSAFE
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$", new StringCreator() {
            @Override
            public String create() {
                return "==error==";
            }
        });

        return replaced;
    }

    static String createLocationEstimateText(final ActiveBounty bounty) {
//        SectorEntityToken hideoutLocation = bounty.getFleetSpawnLocation();
//        SectorEntityToken fake = hideoutLocation.getContainingLocation().createToken(0, 0);
//        fake.setOrbit(Global.getFactory().createCircularOrbit(hideoutLocation, 0, 1000, 100));
//
//        String loc = BreadcrumbSpecial.getLocatedString(fake);

        String loc = BreadcrumbSpecial.getLocatedString(bounty.getFleetSpawnLocation());
//        loc = loc.replaceAll(getString("mb_distance_orbit"), getString("mb_distance_hidingNear"));
//        loc = loc.replaceAll(getString("mb_distance_located"), getString("mb_distance_hidingIn"));
        String sheIs = getPronoun(bounty.getCaptain());
        if (bounty.getCaptain().isAICore()) sheIs = MagicTxt.getString("mb_distance_it");
        loc = sheIs + MagicTxt.getString("mb_distance_rumor") + loc + MagicTxt.getString(".");

        return loc;
    }

    static String getPronoun(@NotNull PersonAPI personAPI) {
        switch (personAPI.getGender()) {
            case FEMALE:
                return MagicTxt.getString("mb_distance_she");
            case MALE:
                return MagicTxt.getString("mb_distance_he");
            default:
                return MagicTxt.getString("mb_distance_they");
        }
    }

    static String createLocationPreciseText(final ActiveBounty bounty) {

        String loc = MagicTxt.getString("mb_distance_last");

        if (bounty.getSpec().fleet_behavior == FleetAssignment.PATROL_SYSTEM) {
            loc = loc + MagicTxt.getString("mb_distance_roaming") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
        } else {
            if (bounty.getFleetSpawnLocation().getMarket() != null) {
                loc = loc + MagicTxt.getString("mb_distance_near") + bounty.getFleetSpawnLocation().getMarket().getName() + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.PLANET)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + bounty.getFleetSpawnLocation().getName() + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.STATION)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_station") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.JUMP_POINT)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_jump") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.GATE)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_gate") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.DEBRIS_FIELD)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_debris") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.WRECK)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_wreck") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.COMM_RELAY)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_comm") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.SENSOR_ARRAY)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_sensor") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.NAV_BUOY)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_nav") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else if (bounty.getFleetSpawnLocation().hasTag(Tags.STABLE_LOCATION)) {
                loc = loc + MagicTxt.getString("mb_distance_near") + MagicTxt.getString("mb_distance_stable") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            } else {
                loc = loc + MagicTxt.getString("mb_distance_somewhere") + MagicTxt.getString("mb_distance_in") + bounty.getFleetSpawnLocation().getStarSystem().getNameWithLowercaseType();
            }
        }
        loc = loc + MagicTxt.getString(".");
        return loc;
    }
}
