package data.scripts.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.InteractionDialogImageVisual;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
//import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.TextPanelAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
//import com.fs.starfarer.api.characters.FullName;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.graphics.SpriteAPI;
//import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.BreadcrumbSpecial;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.util.MagicPaginatedBarEvent;
import data.scripts.util.MagicSettings;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import java.util.*;

import static data.scripts.util.MagicTxt.getString;
import static data.scripts.util.MagicTxt.nullStringIfEmpty;
import data.scripts.util.MagicVariables;

public final class MagicBountyBarEvent extends MagicPaginatedBarEvent {
    private List<String> keysOfBountiesToShow;
    private MarketAPI market;

    /**
     * This method is not called, as the Bar Event is triggered directly in ShowMagicBountyBoardCmd.
     */
    @Override
    public boolean shouldShowAtMarket(MarketAPI market) {
        return MagicBountyCoordinator.getInstance().shouldShowBountyBoardAt(market);
    }

    @Override
    public void addPromptAndOption(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        // Display the text that will appear when the player first enters the bar and looks around
        //"A subroutine from your implant informs you that this establishment is broadcasting an informal job board."
        dialog.getTextPanel().addPara(getString("mb_greeting"));

        // Display the option that lets the player choose to investigate our bar event
        //"Connect to the local unsanctioned bounty board."
        dialog.getOptionPanel().addOption(getString("mb_connect"), this);
    }

    /**
     * Called when the player chooses this event from the list of options shown when they enter the bar.
     */
    @Override
    public void init(InteractionDialogAPI dialog, Map<String, MemoryAPI> memoryMap) {
        super.init(dialog, memoryMap);
        this.market = dialog.getInteractionTarget().getMarket();

        // If player starts our event, then backs out of it, `done` will be set to true.
        // If they then start the event again without leaving the bar, we should reset `done` to false.        
        done = false;
        // Clear on init in case it's being reopened
        options.clear();
        optionsAllPages.clear();

        dialog.getVisualPanel().saveCurrentVisual();

        // The boolean is for whether to show only minimal person information. True == minimal
//        dialog.getVisualPanel().showPersonInfo(person, true);

        // Launch into our event by triggering the "INIT" option, which will call `optionSelected()`
        this.optionSelected(null, OptionId.INIT);
    }

    @Override
    public void optionSelected(String optionText, Object optionData) {
        super.optionSelected(optionText, optionData);
        String acceptJobKeyPrefix = "accept--";
        String dismissJobKeyPrefix = "dismiss--";
        String confirmDismissJobKeyPrefix = "confirm-dismiss--";

        MagicBountyCoordinator instance = MagicBountyCoordinator.getInstance();
        if (optionData instanceof OptionId) {
            TextPanelAPI text = dialog.getTextPanel();

            OptionId optionId = (OptionId) optionData;
            switch (optionId) {
                case INIT:
                case BACK_TO_BOARD:
                    options.clear();
                    dialog.getVisualPanel().restoreSavedVisual();
                    dialog.getVisualPanel().saveCurrentVisual();
                    refreshBounties(market);

                    //"jobs are available on the bounty board."
                    text.addPara("%s " + (keysOfBountiesToShow.size() == 1 ? getString("mb_job") : getString("mb_jobs")) + getString("mb_available"),
                            Misc.getHighlightColor(),
                            Integer.toString(keysOfBountiesToShow.size()));
                    if (Global.getSettings().isDevMode()) {
                        if (!instance.shouldShowBountyBoardAt(market)) {
                            text.addPara("[Dev mode: Bounty board would not have been displayed normally]",
                                    Misc.getHighlightColor(),
                                    Misc.getHighlightColor());
                        }

                        text.addPara(String.format("[Dev mode: Based on market size (%s), there are %s bounty slots available (%s slots on cooldown from being used)]",
                                        market.getSize(),
                                        getNumberOfBountySlots(market),
                                        instance.getBountiesAcceptedAtMarket(market) != null
                                                ? instance.getBountiesAcceptedAtMarket(market).size()
                                                : 0
                                ),
                                Misc.getHighlightColor(),
                                Misc.getHighlightColor());
                    }

                    for (String key : keysOfBountiesToShow) {
                        MagicBountyData.bountyData bounty = MagicBountyData.getBountyData(key);

                        if (bounty != null) {
                            String name = bounty.job_name;

                            if (bounty.job_name == null) {
                                //"Unnamed job"
                                name = getString("mb_unnamed"); // TODO default job name
                            }

                            addOption(name, getBountyOptionKey(key), null, null);
                        }
                    }
                    //"Close the board."
                    addOptionAllPages(getString("mb_close"), OptionId.CLOSE, getString("mb_closed"), Keyboard.KEY_ESCAPE);

                    break;
                case CLOSE:
                    noContinue = true;
                    done = true;
                    break;
                default:
            }
        } else if (optionData instanceof String) {
            String data = (String) optionData;

            // Player accepted a bounty
            if (data.startsWith(acceptJobKeyPrefix)) {
                try {
                    String bountyKey = data.replaceFirst(acceptJobKeyPrefix, "");
                    MagicBountyData.bountyData bounty = MagicBountyData
                            .getBountyData(bountyKey);
                    //"Accepted job: "
                    text.addPara("%s", Misc.getHighlightColor(), getString("mb_accepted") + bounty.job_name);

                    ActiveBounty activeBounty = instance.getActiveBounty(bountyKey);
                    activeBounty.acceptBounty(dialog.getInteractionTarget(), activeBounty.calculateCreditReward());
                    removeBountyFromBoard(bountyKey);

                    optionSelected(null, OptionId.BACK_TO_BOARD);
                } catch (Exception e) {
                    Global.getLogger(this.getClass()).error(e.getMessage(), e);
                }
            } else if (data.startsWith(dismissJobKeyPrefix)) {
                try {
                    String bountyKey = data.replaceFirst(dismissJobKeyPrefix, "");
                    text.addPara("%s", Misc.getHighlightColor(), getString("mb_permDismissConfirm"));
                    options.clear();
                    optionsAllPages.clear();
                    addOption(getString("mb_permDismissConfirmOpt"), confirmDismissJobKeyPrefix + bountyKey, null, null);
                    addOption(getString("mb_returnBounty"), getBountyOptionKey(bountyKey), null, null);
                    addOption(getString("mb_returnBoard"), OptionId.BACK_TO_BOARD, null, Keyboard.KEY_ESCAPE);
                } catch (Exception e) {
                    Global.getLogger(this.getClass()).error(e.getMessage(), e);
                }
            } else if (data.startsWith(confirmDismissJobKeyPrefix)) {
                try {
                    String bountyKey = data.replaceFirst(confirmDismissJobKeyPrefix, "");
                    MagicBountyCoordinator.getInstance().getActiveBounty(bountyKey).endBounty(new ActiveBounty.BountyResult.DismissedPermanently());
                    removeBountyFromBoard(bountyKey);
                    text.addPara("%s", Misc.getHighlightColor(), getString("mb_permDismissConfirmed"));
                    options.clear();
                    optionsAllPages.clear();
                    optionSelected(null, OptionId.BACK_TO_BOARD);
                } catch (Exception e) {
                    Global.getLogger(this.getClass()).error(e.getMessage(), e);
                }
            } else {
                for (String key : keysOfBountiesToShow) {
                    if (getBountyOptionKey(key).equals(optionData)) {
                        // Player has selected to view a bounty
                        final MagicBountyData.bountyData bounty = MagicBountyData.getBountyData(key);

                        if (bounty == null)
                            continue;

                        ActiveBounty activeBounty = instance.getActiveBounty(key);

                        if (activeBounty == null) {
                            activeBounty = instance.createActiveBounty(key, bounty);

                            if (activeBounty == null) continue;
                        }

                        activeBounty.addDescriptionToTextPanel(text);

                        if (nullStringIfEmpty(bounty.job_forFaction) != null) {
                            //"Posted by %s."
                            FactionAPI faction = Global.getSector().getFaction(bounty.job_forFaction);
                            if (faction != null) {
                                text.addPara(getString("mb_from"), faction.getBaseUIColor(), faction.getDisplayNameWithArticle());
                            }
                        }

                        Float creditReward = activeBounty.calculateCreditReward();

                        if (creditReward != null) {
                            //"Reward: %s"
                            text.addPara(getString("mb_credits"), Misc.getHighlightColor(), Misc.getDGSCredits(creditReward));
                        }

                        if (bounty.job_deadline > 0) {
                            //"Time limit: %s days"
                            text.addPara(getString("mb_time"), Misc.getHighlightColor(), Misc.getWithDGS(bounty.job_deadline));
                        }
                        
                        if(bounty.job_show_distance!= MagicBountyData.ShowDistance.None){
                            switch (bounty.job_show_distance) {
                                case Vague:
                                    float distance = activeBounty.getFleetSpawnLocation().getLocation().length();
                                    String vague = getString("mb_distance_core");
                                    if(distance>MagicVariables.getSectorSize()/0.66f){
                                        vague = getString("mb_distance_far");
                                    } else if(distance>MagicVariables.getSectorSize()/0.33f){
                                        vague = getString("mb_distance_close");
                                    }   text.addPara(getString("mb_distance_vague"),
                                            Misc.getTextColor(),
                                            Misc.getHighlightColor(),
                                            vague);
                                    break;

                                case Distance:
                                    text.addPara(getString("mb_distance"),
                                            Misc.getTextColor(),
                                            Misc.getHighlightColor(),
                                            Math.round(Misc.getDistanceLY(market.getPrimaryEntity(), activeBounty.getFleetSpawnLocation()))+"");
                                    break;

                                case Vanilla:
                                    text.addPara(activeBounty.createLocationEstimateText());
                                    break;   

                                case VanillaDistance:
                                    text.addPara(activeBounty.createLocationEstimateText()+" "+getString("mb_distance"),
                                            Misc.getTextColor(),
                                            Misc.getHighlightColor(),
                                            Math.round(Misc.getDistanceLY(market.getPrimaryEntity(), activeBounty.getFleetSpawnLocation()))+"");
                                    break;

                                default:
                                    break;
                            }
                        }
                        
                        
                        //This is an %s mission, to get the reward you will need to %s.
                        if(bounty.job_show_type){
                            switch (bounty.job_type) {
                                case Assassination: 
                                    text.addPara(getString("mb_type"),
                                            Misc.getTextColor(),
                                            Misc.getHighlightColor(),
                                            getString("mb_type_assassination1"),getString("mb_type_assassination2")
                                    );
                                    break;
                                case Destruction: 
                                    text.addPara(getString("mb_type"),
                                            Misc.getTextColor(),
                                            Misc.getHighlightColor(),
                                            getString("mb_type_destruction1"),getString("mb_type_destruction2")
                                    );
                                    break;
                                case Obliteration: 
                                    text.addPara(getString("mb_type"),
                                            Misc.getTextColor(),
                                            Misc.getHighlightColor(),
                                            getString("mb_type_obliteration1"),getString("mb_type_obliteration2")
                                    );
                                    break;
                                case Neutralisation: 
                                    text.addPara(getString("mb_type"),
                                            Misc.getTextColor(),
                                            Misc.getHighlightColor(),
                                            getString("mb_type_neutralisation1"),getString("mb_type_neutralisation2")+Math.round(100*MagicSettings.getFloat("MagicLib", "bounty_neutralisationThreshold"))+getString("mb_type_neutralisation3")
                                    );
                                    break;
                            }
                        }
                        
                        /*
                        if (bounty.job_requireTargetDestruction) {
                            //"This bounty requires the destruction of the flagship. Flagship recovery will forfeit any rewards."
                            text.addPara(getString("mb_noRecovery1"),
                                    Misc.getTextColor(),
                                    Misc.getHighlightColor(),
                                    getString("mb_noRecovery2"));
                        }
                        */
                        if (bounty.job_show_captain) {
                            dialog.getVisualPanel().showPersonInfo(activeBounty.getFleet().getCommander());
                        } else if (nullStringIfEmpty(bounty.job_forFaction) != null && activeBounty.getGivingFaction() != null) {
                            String factionLogoSpriteName = activeBounty.getGivingFaction().getLogo();
                            SpriteAPI sprite = Global.getSettings().getSprite(factionLogoSpriteName);
                            InteractionDialogImageVisual visual = new InteractionDialogImageVisual(factionLogoSpriteName, sprite.getWidth(), sprite.getHeight());
                            visual.setShowRandomSubImage(false);
                            dialog.getVisualPanel().showImageVisual(visual);
                        }

                        if (bounty.job_difficultyDescription != null && bounty.job_difficultyDescription.equals(getString("mb_threatAssesmentAuto"))) {
                            int playerFleetStrength = Math.round(Global.getSector().getPlayerFleet().getEffectiveStrength());
                            float bountyFleetStrength = activeBounty.getFleet().getEffectiveStrength();
//                            String dangerStringArticle = getString("mb_threatArticle1");
                            String dangerStringPhrase;

                            if (playerFleetStrength < Math.round(bountyFleetStrength * 0.25f)) {
//                                dangerStringArticle = getString("mb_threatArticle2");
                                dangerStringPhrase = getString("mb_threatLevel6");
                            } else if (playerFleetStrength < Math.round(bountyFleetStrength * 0.5f)) {
                                dangerStringPhrase = getString("mb_threatLevel5");
                            } else if (playerFleetStrength < Math.round(bountyFleetStrength * 0.75f)) {
                                dangerStringPhrase = getString("mb_threatLevel4");
                            } else if (playerFleetStrength < Math.round(bountyFleetStrength * 1f)) {
                                dangerStringPhrase = getString("mb_threatLevel3");
                            } else if (playerFleetStrength < Math.round(bountyFleetStrength * 1.5f)) {
                                dangerStringPhrase = getString("mb_threatLevel2");
                            } else if (playerFleetStrength < Math.round(bountyFleetStrength * 2f)) {
                                dangerStringPhrase = getString("mb_threatLevel1");
                            } else {
//                                dangerStringArticle = getString("mb_threatArticle0");
                                dangerStringPhrase = getString("mb_threatLevel0");
                            }
                            //"Your intelligence officer informs you that the target poses "
//                            text.addPara(getString("mb_threat1") + dangerStringArticle + getString("mb_threat3"), Misc.getHighlightColor(), dangerStringPhrase + getString("mb_threat2"));
                            text.addPara(getString("mb_threat1") + getString("mb_threat2"), Misc.getHighlightColor(), dangerStringPhrase);
                        } else if (nullStringIfEmpty(bounty.job_difficultyDescription) != null
                                && !bounty.job_difficultyDescription.equals(getString("mb_threatAssesmentNone"))) {
                            text.addPara(bounty.job_difficultyDescription);
                        }

                        if (bounty.job_show_fleet != MagicBountyData.ShowFleet.None) {
                            String info = getString("mb_fleet2");
                            if (bounty.job_show_fleet == MagicBountyData.ShowFleet.Flagship) {
                                info = getString("mb_fleet0");
                            }
                            if (bounty.job_show_fleet == MagicBountyData.ShowFleet.Preset) {
                                info = getString("mb_fleet1");
                            }
                            text.addPara(getString("mb_fleet"),
                                    Misc.getTextColor(),
                                    Misc.getHighlightColor(),
                                    info);
                            int columns = 10;
                            List<FleetMemberAPI> ships = activeBounty.getFleet().getMembersWithFightersCopy();

                            if (bounty.job_show_fleet == MagicBountyData.ShowFleet.Flagship) {
                                ships = activeBounty.getFlagshipInFleet();
                            } else if (bounty.job_show_fleet == MagicBountyData.ShowFleet.Preset) {
                                ships = activeBounty.getPresetShipsInFleet();
                            }

                            text.beginTooltip()
                                    .addShipList(columns, 2, (dialog.getTextWidth() - 10) / columns,
                                            activeBounty.getFleet().getFaction().getBaseUIColor(),
                                            ships, 10f);
                            text.addTooltip();
                        }

                        options.clear();
                        optionsAllPages.clear();
                        addOption(bounty.job_pick_option != null && !bounty.job_pick_option.isEmpty()
                                ? bounty.job_pick_option
                                : getString("mb_accept"), acceptJobKeyPrefix + key, null, null);
                        addOption(getString("mb_permDismissOpt"), dismissJobKeyPrefix + key, null, null);
                        addOption(getString("mb_return"), OptionId.BACK_TO_BOARD, null, Keyboard.KEY_ESCAPE);
                    }
                }
            }
        }

        showOptions();
    }

    private void removeBountyFromBoard(String bountyKey) {
        keysOfBountiesToShow.remove(bountyKey);
        MagicBountyCoordinator.getInstance().setBlockBountyAtMarket(market, bountyKey);
    }

    enum OptionId {
        INIT,
        CLOSE,
        BACK_TO_BOARD
    }

    @Override
    public boolean isAlwaysShow() {
        return true;
    }

    private void refreshBounties(@NotNull MarketAPI market) {
        MagicBountyCoordinator instance = MagicBountyCoordinator.getInstance();
        List<String> keysToReturn = new ArrayList<>();

        List<String> bountiesAcceptedAtMarket = instance.getBountiesAcceptedAtMarket(market);
        int numberOfBountySlots = Math.max(0, getNumberOfBountySlots(market) - (bountiesAcceptedAtMarket != null
                ? bountiesAcceptedAtMarket.size()
                : 0));

        Map<String, MagicBountyData.bountyData> bountiesAtMarketById = instance.getBountiesWithChanceToSpawnAtMarketById(market);
        WeightedRandomPicker<String> picker = new WeightedRandomPicker<>(new Random(instance.getMarketBountyBoardGenSeed(market)));

        for (Map.Entry<String, MagicBountyData.bountyData> entry : bountiesAtMarketById.entrySet()) {
            picker.add(entry.getKey(), entry.getValue().trigger_weight_mult);
        }

        for (int i = keysToReturn.size(); i < numberOfBountySlots; i++) {
            String pickedKey = picker.pickAndRemove();

            if (pickedKey != null) {
                keysToReturn.add(pickedKey);
            }
        }

        this.keysOfBountiesToShow = keysToReturn;
    }

    private String getBountyOptionKey(String key) {
        return "optionkey-" + key;
    }

    private Map<String, MagicBountyData.bountyData> getBountiesToShow() {
        Map<String, MagicBountyData.bountyData> ret = new HashMap<>(keysOfBountiesToShow.size());

        for (String key : keysOfBountiesToShow) {
            ret.put(key, MagicBountyData.BOUNTIES.get(key));
        }

        return ret;
    }

    /**
     * The max number of bounties to show at once.
     */
    private int getNumberOfBountySlots(MarketAPI market) {
        return MagicBountyCoordinator.getInstance().getBountySlotsAtMarket(market);
    }
}
