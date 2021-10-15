package data.scripts.bounty;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.ReputationActionResponsePlugin;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.BaseIntelPlugin;
import com.fs.starfarer.api.ui.SectorMapAPI;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.scripts.util.MagicDeserializable;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static com.fs.starfarer.api.util.Misc.random;
import static data.scripts.util.MagicTxt.getString;

/**
 * Displays MagicLib Bounties to the player.
 *
 * @author Wisp, Tartiflette
 */
public class MagicBountyIntel extends BaseIntelPlugin implements MagicDeserializable {
    @NotNull
    public final String bountyKey;

    private static final Float PADDING_DESC = 10f;
    private static final Float PADDING_INFO_SUBTITLE = 3f;

    public MagicBountyIntel(@NotNull String bountyKey) {
        this.bountyKey = bountyKey;

        if (getBounty() == null) {
            throw new NullPointerException("Expected an active bounty for key " + bountyKey);
        }
    }

    @Override
    public Object readResolve() {
        return this;
    }

    @NotNull
    private ActiveBounty getBounty() {
        return MagicBountyCoordinator.getInstance().getActiveBounty(bountyKey);
    }

    @Override
    public String getIcon() {
        /*
        ActiveBounty bounty = getBounty();
        if (bounty.getFleet().getCommander() != null && bounty.getFleet().getCommander().getPortraitSprite() != null) {
            return bounty.getFleet().getCommander().getPortraitSprite();
        } else {
            return null; // TODO special bounty board icon?
        }
        */
        return "graphics/magic/icons/ml_bounty.png";
    }

    @Override
    protected String getName() {
        ActiveBounty bounty = getBounty();

        switch (bounty.getStage()) {
            case Succeeded:
                return String.format(getString("mb_intelTitleCompleted"), bounty.getSpec().job_name);
            case FailedSalvagedFlagship:
                return String.format(getString("mb_intelTitleFailed"), bounty.getSpec().job_name);
            case Accepted:
            case NotAccepted:
            default:
                return String.format(getString("mb_intelTitleInProgress"), bounty.getSpec().job_name);
        }
    }

    @Override
    public Color getTitleColor(ListInfoMode mode) {
        ActiveBounty bounty = getBounty();

        switch (bounty.getStage()) {
            case Accepted:
            case NotAccepted:
                return super.getTitleColor(mode);
            case Succeeded:
            case FailedSalvagedFlagship:
            default:
                return Misc.getGrayColor();
        }
    }

    @Override
    public void createIntelInfo(TooltipMakerAPI info, ListInfoMode mode) {
        super.createIntelInfo(info, mode);
        ActiveBounty bounty = getBounty();


        switch (bounty.getStage()) {
            case Succeeded:
                if (bounty.hasCreditReward()) {
                    bullet(info);
                    //"%s credits received"
                    info.addPara(getString("mb_intelRewarded"),
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(bounty.getRewardCredits()));
                    unindent(info);
                }
                break;
            case FailedSalvagedFlagship:
                break;
            case Accepted:
            case NotAccepted:
            default:
                if (bounty.getGivingFaction() != null) {
                    bullet(info);
                    //"Faction: %s"
                    info.addPara(getString("mb_intelFaction"),
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            bounty.getGivingFactionTextColor(),
                            bounty.getGivingFaction().getDisplayName());
                    unindent(info);
                }

                if (bounty.hasCreditReward() && bounty.hasExpiration()) {
                    bullet(info);
                    //"%s reward, %s days remaining"
                    info.addPara(getString("mb_intelRewardRemaining"),
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(bounty.getRewardCredits()),
                            Integer.toString(Math.round(bounty.getDaysRemainingToComplete())));
                    unindent(info);
                } else if (bounty.hasCreditReward()) {
                    bullet(info);
                    //"%s reward"
                    info.addPara(getString("mb_intelReward"),
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(bounty.getRewardCredits()));
                    unindent(info);
                } else if (bounty.hasExpiration()) {
                    bullet(info);
                    //"%s days remaining"
                    info.addPara(getString("mb_intelRemaining"),
                            PADDING_INFO_SUBTITLE,
                            Misc.getGrayColor(),
                            Misc.getHighlightColor(),
                            Integer.toString(Math.round(bounty.getDaysRemainingToComplete())));
                    unindent(info);
                }

                break;
        }
    }

    @Override
    public void createSmallDescription(TooltipMakerAPI info, float width, float height) {
        ActiveBounty bounty = getBounty();

        if (bounty.getSpec().job_show_captain && bounty.getFleet().getCommander() != null && bounty.getFleet().getCommander().getPortraitSprite() != null) {
            info.addImage(bounty.getFleet().getCommander().getPortraitSprite(), width, 128f, PADDING_DESC);
        } else {
            info.addImage("graphics/magic/icons/ml_bountyBoard.png", width, 128f, PADDING_DESC);
        }

        //I'm removing the whole description from the small description because it's just too much text
        /*
        bounty.addDescriptionToTextPanel(info,
                bounty.getStage().ordinal() > ActiveBounty.Stage.Accepted.ordinal()
                        ? Misc.getGrayColor()
                        : Misc.getTextColor(),
                PADDING_DESC);
        */

        switch (bounty.getStage()) {
            case Succeeded:
                
                info.addPara(bounty.getSpec().job_conclusion_script, 0f);
                //"You have successfully completed this bounty."
                info.addPara(getString("mb_descSuccess"), 0f);
                
                //adding optional success text:
                if(bounty.getSpec().job_intel_success!=null && !bounty.getSpec().job_intel_success.isEmpty() ){
                    info.addPara(
                            bounty.getSpec().job_intel_success,
                            Misc.getGrayColor(),
                            PADDING_DESC
                    );
                }
                
                if (bounty.hasCreditReward()) {
//                    bullet(info);
                    //"%s credits received"
                    info.addPara(
                            getString("mb_descRewarded"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(bounty.getRewardCredits())
                    );
//                    unindent(info);
                }

                if (bounty.hasReputationReward()) {
                    //TO DO: VANILLA STYLE REPUTATION MESSAGE
                    /*
                    CoreReputationPlugin.addAdjustmentMessage(
                            bounty.getRewardReputation(),
                            bounty.getGivingFaction(),
                            null,
                            null,
                            null,
                            info, 
                            bulletColor,
                            isUpdate,
                            initPad
                    );
                    bullet(info);
                    //"Your reputation with %s has improved by %s."
                    info.addPara(
                            getString("mb_descReputation"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getHighlightColor(),
                            bounty.getGivingFaction().getDisplayNameWithArticle(),
                            Math.round(bounty.getRewardReputation() * 100) + ""
                    );
                    unindent(info);
                    */
                    addRepMessage(info, PADDING_DESC, bounty.getGivingFaction(), bounty.getRewardReputation());
                }
                break;
                
            case FailedSalvagedFlagship:
                //"You have failed this bounty."
                info.addPara(getString("mb_descFailure"), 0f);
                
                //adding optional failure text:
                if(bounty.getSpec().job_intel_failure!=null && !bounty.getSpec().job_intel_failure.isEmpty() ){
                    info.addPara(
                            bounty.getSpec().job_intel_failure,
                            Misc.getGrayColor(),
                            PADDING_DESC
                    );
                }
                
                if(bounty.hasReputationReward()){     
                    /*
                    bullet(info);
                    //"Your reputation with %s has been reduced by %s."
                    info.addPara(
                            getString("mb_descReputationBad"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getNegativeHighlightColor(),
                            bounty.getGivingFaction().getDisplayNameWithArticle(),
                            Math.max(5, Math.round(bounty.getRewardReputation() * 100)) + ""
                    );
                    unindent(info);
                    */
                    addRepMessage(info, PADDING_DESC, bounty.getGivingFaction(), Math.min(-0.05f, -bounty.getRewardReputation()));
                }
                break;
                
            case ExpiredAfterAccepting:
                //"You have failed this bounty."
                info.addPara(getString("mb_descExpired"), 0f);
                
                //adding optional failure text:
                if(bounty.getSpec().job_intel_expired!=null && !bounty.getSpec().job_intel_expired.isEmpty() ){
                    info.addPara(
                            bounty.getSpec().job_intel_expired,
                            Misc.getGrayColor(),
                            PADDING_DESC
                    );
                }
                
                if(bounty.hasReputationReward()){      
                    /*
                    bullet(info);
                    //"Your reputation with %s has been reduced by %s."
                    info.addPara(
                            getString("mb_descReputationBad"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getNegativeHighlightColor(),
                            bounty.getGivingFaction().getDisplayNameWithArticle(),
                            Math.min(5, Math.round(bounty.getRewardReputation() * 100)) + ""
                    );
                    unindent(info);
                    */
                    addRepMessage(info, PADDING_DESC, bounty.getGivingFaction(), Math.max(-0.05f, -bounty.getRewardReputation()));
                }
                break;
                
            case EndedWithoutPlayerInvolvement:
                
                //"Someone else completed this mission."
                info.addPara(getString("mb_descUninvolved"), 0f);
                
                if(bounty.hasReputationReward()){   
                    /*
                    bullet(info);
                    //"Your reputation with %s was unafected."
                    info.addPara(
                            getString("mb_descReputationNothing"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getNegativeHighlightColor(),
                            bounty.getGivingFaction().getDisplayNameWithArticle()
                    );
                    unindent(info);
                    */
                    addRepMessage(info, PADDING_DESC, bounty.getGivingFaction(), 0);
                }
                break;
            case Accepted:
            case NotAccepted:
            default:

                //This is an %s mission, to get the reward you will need to %s.
                if (bounty.getSpec().job_show_type) {
                    switch (bounty.getSpec().job_type) {
                        case Assassination:
                            info.addPara(getString("mb_intelType"),
                                    PADDING_DESC,
                                    Misc.getTextColor(),
                                    Misc.getHighlightColor(),
                                    getString("mb_type_assassination1")
                            );
                            break;
                        case Destruction:
                            info.addPara(getString("mb_intelType"),
                                    PADDING_DESC,
                                    Misc.getTextColor(),
                                    Misc.getHighlightColor(),
                                    getString("mb_type_destruction1")
                            );
                            break;
                        case Obliteration:
                            info.addPara(getString("mb_intelType"),
                                    PADDING_DESC,
                                    Misc.getTextColor(),
                                    Misc.getHighlightColor(),
                                    getString("mb_type_obliteration1")
                            );
                            break;
                        case Neutralisation:
                            info.addPara(getString("mb_intelType"),
                                    PADDING_DESC,
                                    Misc.getTextColor(),
                                    Misc.getHighlightColor(),
                                    getString("mb_type_neutralisation1")
                            );
                            break;
                    }
                    unindent(info);
                }

                if (bounty.hasCreditReward()) {
                    bullet(info);
                    //"%s reward"
                    info.addPara(getString("mb_descReward"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getHighlightColor(),
                            Misc.getDGSCredits(bounty.getRewardCredits()));
                    unindent(info);
                }

                if (bounty.hasExpiration()) {
                    bullet(info);
                    //"remaining"
                    addDays(info, getString("mb_descRemaining"), Math.round(bounty.getDaysRemainingToComplete()), Misc.getTextColor());
                    unindent(info);
                }

                if (true) { // todo
                    info.addPara(MagicBountyUtils.createLocationEstimateText(bounty), 10f);
                }

                if (bounty.getSpec().job_show_fleet != MagicBountyData.ShowFleet.None) {
                    /*
                    //"Fleet information is attached to the posting."
                    String fleetInfo = getString("mb_fleet2");
                    if (bounty.getSpec().job_show_fleet == MagicBountyData.ShowFleet.Flagship) {
                        fleetInfo = getString("mb_fleet0");
                    }
                    if (bounty.getSpec().job_show_fleet == MagicBountyData.ShowFleet.Preset) {
                        fleetInfo = getString("mb_fleet1");
                    }
                    info.addPara(fleetInfo + getString("mb_fleet"), PADDING_DESC);

                    int columns = 7;
                    List<FleetMemberAPI> ships = bounty.getFleet().getMembersWithFightersCopy();
                    if (bounty.getSpec().job_show_fleet == MagicBountyData.ShowFleet.Flagship) {
                        ships = bounty.getFlagshipInFleet();
                    } else if (bounty.getSpec().job_show_fleet == MagicBountyData.ShowFleet.Preset) {
                        ships = bounty.getPresetShipsInFleet();
                    }

                    info.addShipList(columns, (int) Math.round(Math.ceil((double) ships.size() / columns)), (width - 10) / columns,
                            bounty.getFleet().getFaction().getBaseUIColor(),
                            ships, 10f);
                    */
                    showFleet(
                            info,
                            width,
                            bounty.getFleet().getFaction().getBaseUIColor(),
                            bounty.getSpec().job_show_fleet,
                            bounty.getFleet().getMembersWithFightersCopy(),
                            bounty.getFlagshipInFleet(),
                            bounty.getPresetShipsInFleet()
                    );
                }
                break;
        }
    }

    @Override
    public SectorEntityToken getMapLocation(SectorMapAPI map) {
        ActiveBounty bounty = getBounty();

        return bounty.getFleet().isInHyperspace() ? bounty.getFleet() : bounty.getFleet().getStarSystem().getCenter();
    }

    @Override
    public List<ArrowData> getArrowData(SectorMapAPI map) {
        ActiveBounty bounty = getBounty();

        if (!bounty.getSpec().job_show_arrow) {
            return null;
        }

        ArrowData arrowData = new ArrowData(bounty.getBountySource(), bounty.getFleet());
        arrowData.color = bounty.getGivingFactionTextColor();
        return Collections.singletonList(arrowData);
    }

    @Override
    public Set<String> getIntelTags(SectorMapAPI map) {
        Set<String> tags = super.getIntelTags(map);
        Collections.addAll(tags, Tags.INTEL_BOUNTY, Tags.INTEL_ACCEPTED);
        ActiveBounty bounty = getBounty();

        if (bounty.getGivingFaction() != null) {
            tags.add(bounty.getGivingFaction().getDisplayName());
        }

        return tags;
    }
    
    public static void addRepMessage(TooltipMakerAPI info, float pad, FactionAPI faction, float change) {
        
        if (info==null || faction==null)return;

        String factionName = faction.getDisplayNameWithArticle();
        int deltaInt = Math.round(Math.abs(change) * 100f);
        FactionAPI player = Global.getSector().getPlayerFaction();
        int repInt = RepLevel.getRepInt(player.getRelationship(faction.getId()));
        RepLevel repLevel = player.getRelationshipLevel(faction.getId());
        Color factionColor = faction.getBaseUIColor();
        Color deltaColor = Misc.getPositiveHighlightColor();
        Color relationColor = faction.getRelColor(player.getId());

        String deltaString = getString("mb_descRepGood") + deltaInt;
        String standing = "" + repInt + getString("mb_descRepStanding") + getString("(") + repLevel.getDisplayName().toLowerCase() + getString(")");

        if (change < 0) {
            deltaColor = Misc.getNegativeHighlightColor();
            deltaString = getString("mb_descRepBad") + deltaInt;
        } else if (change == 0) {
            deltaString = getString("mb_descRepNothing");
            deltaColor = Misc.getTextColor();
        }

        Color[] highlightColors = {factionColor, deltaColor, relationColor};
        //"Relationship with %s %s, currently at %s",
        info.addPara(getString("mb_descRep"), pad, highlightColors, factionName, deltaString, standing);
    }
    
    private void showFleet(TooltipMakerAPI info, float width, Color factionBaseUIColor, MagicBountyData.ShowFleet setting, List<FleetMemberAPI> ships, List<FleetMemberAPI> flagship, List<FleetMemberAPI> preset) {

        int columns = 7;
        switch (setting) {
            case Text:
                //write the number of ships
                int num = ships.size();
                if (num < 5) {
                    num = 5;
                    info.addPara(getString("mb_fleet6"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getHighlightColor(),
                            "" + num
                    );
                    break;
                } else if (num < 10) num = 5;
                else if (num < 20) num = 10;
                else if (num < 30) num = 20;
                else num = 30;
                info.addPara(getString("mb_fleet5"),
                        PADDING_DESC,
                        Misc.getTextColor(),
                        Misc.getHighlightColor(),
                        "" + num
                );
            case Flagship:
                //show the flagship
                info.addPara(getString("mb_fleet0") + getString("mb_fleet"), PADDING_DESC);
                info.addShipList(
                        columns,
                        1,
                        (width - 10) / columns,
                        factionBaseUIColor,
                        ships,
                        10f
                );
                break;
            case FlagshipText:
                //show the flagship
                info.addPara(getString("mb_fleet0") + getString("mb_fleet"), PADDING_DESC);
                info.addShipList(
                        columns,
                        1,
                        (width - 10) / columns,
                        factionBaseUIColor,
                        ships,
                        10f
                );
                //write the number of other ships
                num = ships.size() - 1;
                num = Math.round((float) num * (1f + random.nextFloat() * 0.5f));
                if (num < 5) {
                    info.addPara(getString("mb_fleet4"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getHighlightColor()
                    );
                    break;
                } else if (num < 10) num = 5;
                else if (num < 20) num = 10;
                else if (num < 30) num = 20;
                else num = 30;
                info.addPara(getString("mb_fleet3"),
                        PADDING_DESC,
                        Misc.getTextColor(),
                        Misc.getHighlightColor(),
                        "" + num
                );
                break;
            case Preset:
                //show the preset fleet
                info.addPara(getString("mb_fleet1") + getString("mb_fleet"), PADDING_DESC);
                info.addShipList(
                        columns,
                        (int) Math.round(Math.ceil((double) preset.size() / columns)),
                        (width - 10) / columns,
                        factionBaseUIColor,
                        preset,
                        10f
                );
                break;
            case PresetText:
                //show the preset fleet
                info.addPara(getString("mb_fleet1") + getString("mb_fleet"), PADDING_DESC);
                List<FleetMemberAPI> toShow = preset;
//                toShow.addAll(preset);
                info.addShipList(
                        columns,
                        (int) Math.round(Math.ceil((double) preset.size() / columns)),
                        (width - 10) / columns,
                        factionBaseUIColor,
                        toShow,
                        10f
                );
                //write the number of other ships
                num = ships.size() - toShow.size();
                num = Math.round((float) num * (1f + random.nextFloat() * 0.5f));
                if (num < 5) {
                    info.addPara(getString("mb_fleet4"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getHighlightColor()
                    );
                    break;
                } else if (num < 10) num = 5;
                else if (num < 20) num = 10;
                else if (num < 30) num = 20;
                else num = 30;
                info.addPara(getString("mb_fleet3"),
                        PADDING_DESC,
                        Misc.getTextColor(),
                        Misc.getHighlightColor(),
                        "" + num
                );
                break;
            case Vanilla:
                //show the Flagship and the 6 biggest ships in the fleet
                info.addPara(getString("mb_fleet1") + getString("mb_fleet"), PADDING_DESC);

                //there are less than 7 ships total, all will be shown
                if (ships.size() <= columns) {
                    toShow = ships;
                    info.addShipList(
                            columns,
                            1,
                            (width - 10) / columns,
                            factionBaseUIColor,
                            toShow,
                            10f
                    );
                    info.addPara(getString("mb_fleet4"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getHighlightColor()
                    );
                    break;
                }
                //If there are more than 7 ships, pick the largest 7
                //make a random picker with squared FP weight to heavily trend toward bigger ships
                WeightedRandomPicker<FleetMemberAPI> picker = new WeightedRandomPicker<>();
                for (FleetMemberAPI m : ships) {
                    if (m == flagship.get(0)) continue;
                    if (m.isFighterWing()) continue;
                    picker.add(m, (float) Math.pow(m.getFleetPointCost(), 2));
                }
                //make a pick list starting with the flagship 
                toShow = flagship;
                for (int i = 1; i < columns; i++) {
                    toShow.add(picker.pickAndRemove());
                }
                //make the ship list
                info.addShipList(
                        columns,
                        1,
                        (width - 10) / columns,
                        factionBaseUIColor,
                        toShow,
                        10f
                );

                //write the number of other ships
                num = ships.size() - columns;
                num = Math.round((float) num * (1f + random.nextFloat() * 0.5f));
                if (num < 5) {
                    info.addPara(getString("mb_fleet4"),
                            PADDING_DESC,
                            Misc.getTextColor(),
                            Misc.getHighlightColor()
                    );
                    break;
                } else if (num < 10) num = 5;
                else if (num < 20) num = 10;
                else if (num < 30) num = 20;
                else num = 30;
                info.addPara(getString("mb_fleet3"),
                        PADDING_DESC,
                        Misc.getTextColor(),
                        Misc.getHighlightColor(),
                        "" + num
                );
                break;
            case All:
                //show the full fleet
                info.addPara(getString("mb_fleet2") + getString("mb_fleet"), PADDING_DESC);
                info.addShipList(
                        columns,
                        (int) Math.round(Math.ceil((double) ships.size() / columns)),
                        (width - 10) / columns,
                        factionBaseUIColor,
                        ships,
                        10f
                );
            default:
                break;
        }
    }
}
