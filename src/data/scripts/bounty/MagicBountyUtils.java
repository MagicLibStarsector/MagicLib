package data.scripts.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial;
import com.fs.starfarer.api.util.Misc;
import data.scripts.util.MagicTxt;
import data.scripts.util.StringCreator;

class MagicBountyUtils {


    /**
     * Replaces variables in the given string with data from the bounty and splits it into paragraphs using `\n`.
     */
    static String replaceStringVariables(final ActiveBounty bounty, String text) {
        String replaced = text;

        replaced = MagicTxt.replaceAllIfPresent(replaced, "$sonOrDaughter|$sonOrDaughterOrChild", new StringCreator() {
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
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$fatherOrMother|$fatherOrMotherOrParent", new StringCreator() {
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
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$manOrWomanOrPerson", new StringCreator() {
            @Override
            public String create() {
                switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE:
                        return MagicTxt.getString("mb_man");
                    case FEMALE:
                        return MagicTxt.getString("mb_woman");
                    default:
                        return MagicTxt.getString("mb_person");
                }
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$hisOrHerOrTheir", new StringCreator() {
            @Override
            public String create() {
                switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE:
                        return MagicTxt.getString("mb_his");
                    case FEMALE:
                        return MagicTxt.getString("mb_her");
                    default:
                        return MagicTxt.getString("mb_their");
                }
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$heOrSheOrThey", new StringCreator() {
            @Override
            public String create() {
                switch (bounty.getFleet().getCommander().getGender()) {
                    case MALE:
                        return MagicTxt.getString("mb_he");
                    case FEMALE:
                        return MagicTxt.getString("mb_she");
                    default:
                        return MagicTxt.getString("mb_they");
                }
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$system_name", new StringCreator() {
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
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$fleetAssignmentTarget", new StringCreator() {
            @Override
            public String create() {
                return bounty.getFleetSpawnLocation().getName();
            }
        });
        replaced = MagicTxt.replaceAllIfPresent(replaced, "$target", new StringCreator() {
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

        return replaced;
    }

    static String createLocationEstimateText(final ActiveBounty bounty) {
        SectorEntityToken hideoutLocation = bounty.getFleetSpawnLocation();
        SectorEntityToken fake = hideoutLocation.getContainingLocation().createToken(0, 0);
        fake.setOrbit(Global.getFactory().createCircularOrbit(hideoutLocation, 0, 1000, 100));

        String loc = BreadcrumbSpecial.getLocatedString(fake);
        loc = loc.replaceAll("orbiting", "hiding out near");
        loc = loc.replaceAll("located in", "hiding out in");
        String sheIs = "She is";
        if (bounty.getCaptain().getGender() == FullName.Gender.MALE) sheIs = "He is";
        if (bounty.getCaptain().getGender() == FullName.Gender.ANY) sheIs = "They are";
        loc = sheIs + " rumored to be " + loc + ".";

        return loc;
    }
}
