package org.magiclib.campaign;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.campaign.FactionAPI.ShipPickMode;
import com.fs.starfarer.api.campaign.econ.CommoditySpecAPI;
import com.fs.starfarer.api.campaign.econ.SubmarketAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.submarkets.BaseSubmarketPlugin;
import com.fs.starfarer.api.impl.campaign.submarkets.BlackMarketPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.magiclib.util.MagicMap;

import java.io.IOException;
import java.util.*;

/**
 * Sets up a special submarket.
 * <p>
 * The goods and actions on the submarket can be filtered using a range of blacklists and whitelists.
 * These are defined in a `.json` file under `data/campaign/submarkets/`, with a name matching the id of the submarket
 * as defined in `submarkets.csv`. The file structure is:
 * <p>
 * <pre>
 *     {
 *         "no_sell_message": "OPTIONAL: If this is here, selling is banned and this message is shown as the reason.",
 *         "no_buy_message": "OPTIONAL: If this is here, buying is banned and this message is shown as the reason.",
 *         "message": "Generic message for why you can't trade X at this submarket. %s is replaced by the faction.",
 *         "black_market": False,          # OPTIONAL: Is this a black market? Defaults to no.
 *         "learns_blueprints": False,     # OPTIONAL: Does this faction learn blueprints sold to them? Defaults to no.
 *         "tariff": 0.3,                  # OPTIONAL: If present, the submarket tariff; if not, uses the market default.
 *         "in_economy": False,            # OPTIONAL: If present, whether it's part of the global economy; if not, it isn't.
 *                                         # Special submarkets should almost always not be in the economy.
 *         "hidden_key": "$my_hidden_key", # OPTIONAL: If present, the submarket is hidden unless this key exists and is True.
 *                                         # Checks the local market memory, then the global memory.
 *
 *         "ships": {               # OPTIONAL: If present, the submarket sells ships as defined here.
 *                                  # Ships are generated from the faction priority list first.
 *                                  # If it's not present, the market won't appear in the ship sales view.
 *              "ships_only": False,    # OPTIONAL: If present and true, the market doesn't have an items tab.
 *              "quality_bonus": 1,     # OPTIONAL: How much better the quality of ships is here than the parent market.
 *              "tanker": {                 # OPTIONAL: How many DP of tankers to add
 *                                          # Also "combat", "liner", "utility", "freighter", "transport"
 *                  "static": 0,                # OPTIONAL: How many static DP, regardless of colony size.
 *                  "per_size": 0,              # OPTIONAL: How many DP per market size.
 *              },
 *              "static": [             # OPTIONAL: What ship variants should always be available for sale?
 *                  "onslaught_Elite", "eagle_Assault"  # EXAMPLE: Ids to always sell. Weapons will be stripped.
 *              ],
 *              "cull_fraction": 0.5,  # OPTIONAL: Removes this fraction of ships after generating.
 *                                     # Useful if you need a high DP cap to spawn large ships but only want a few,
 *                                     # or want your static ships to not always appear.
 *
 *              "blacklist": {           # OPTIONAL: If present, hulls in the blacklist cannot be traded.
 *                  "ids": ["onslaught", "eagle"],    # Ids that can't be traded.
 *                  "message": "The message for why this hull can't be sold. %s is replaced with the hull name."
 *              },
 *              "whitelist": {           # OPTIONAL: If present, only hulls in the whitelist can be traded.
 *                  "ids": ["onslaught", "eagle"],    # Only these ids can be traded.
 *                  "message": "The message for why this hull can't be sold. %s is replaced with the hull name."
 *              },
 *          },
 *          "special_items": {
 *              # OPTIONAL: Takes "blacklist" and "whitelist" as the other types. Replaces %s with special item name.
 *          },
 *          "commodities": {
 *              # OPTIONAL: Takes "blacklist" and "whitelist" as the other types. Replaces %s with commodity name.
 *          },
 *          "hullmods": {
 *              # OPTIONAL: Takes "blacklist" and "whitelist" as the other types. Replaces %s with hull mod name.
 *              "count" : {         # OPTIONAL: How many hullmods to randomly add.
 *                  "static": 0,        # OPTIONAL: Flat number of hullmods regardless of colony size.
 *                  "per_size": 0       # OPTIONAL: How many hullmods per size of the colony to also add.
 *              }
 *              "tier": {           # OPTIONAL: Tier of hullmods to randomly add.
 *                  "static": 0,        # OPTIONAL: Flat tier of hullmods regardless of colony size.
 *                  "per_size": 0       # OPTIONAL: How tier should scale by colony size.
 *              },
 *              "static": ["ecm", "eccm"]       # OPTIONAL: List of individual hullmods to also add.
 *          },
 *          "weapons": {
 *              # OPTIONAL: Takes "blacklist" and "whitelist" as the other types. Replaces %s with weapon name.
 *              "count": {           # OPTIONAL: How many weapons to randomly add.
 *                                   # Weapons are generated from the faction priority list (first).
 *                  "static": 0,        # OPTIONAL: Flat number of weapons regardless of colony size.
 *                  "per_size_min": 0,  # OPTIONAL: How many weapons per size of the colony to also add (minimum).
 *                  "per_size_max": 0   # OPTIONAL: How many weapons per size of the colony to also add (maximum).
 *               },
 *              "tier": {           # OPTIONAL: Tier of weapons to randomly add.
 *                  "static": 0,        # OPTIONAL: Flat tier of weapons regardless of colony size.
 *                  "per_size": 0       # OPTIONAL: How tier should scale by colony size.
 *              },
 *              "static": ["lightmg", "lightmg", "arbalest"],  # OPTIONAL: List of individual weapons to also add.
 *              "cull_fraction": 0      # OPTIONAL:  Removes this fraction of weapons after generating. Shared with fighters.
 *          },
 *          "fighters": {
 *              # OPTIONAL: As weapons, takes blacklist, whitelist, count, tier, static and cull fraction.
 *          },
 *          "tags" {
 *              # OPTIONAL: Limits on tags on items that can be traded. Example:
 *              "blacklist": {
 *                  "ids": ["colony_item", "expensive"],
 *                  "message": "The message for why this can't be traded. No substitution, as tags have no display names."
 *              },
 *              "whitelist": {
 *                  "ids": ["colony_item", "expensive"],
 *                  "message": "The message for why this can't be traded. No substitution, as tags have no display names."
 *              }
 *          },
 *          "demand_classes" {
 *              # OPTIONAL: If present, limits on classes of commodities that can be traded. Example:
 *              "blacklist": {
 *                  "ids": ["luxury_goods", "ai_cores"],
 *                  "message": "The message for why this can't be traded. No substitution, as display classes have no names."
 *              },
 *              "whitelist": {
 *                  "ids": ["luxury_goods", "ai_cores"],
 *                  "message": "The message for why this can't be traded. No substitution, as display classes have no names."
 *              }
 *          }
 *     }
 * </pre>
 * <p>
 * A simple example, for "my_faction_submarket" declared in "submarkets.csv" and specified in "submarkets/my_faction_submarket.json".
 * It always sells at least one copy of `my_weapon`, plus some randomly generated faction priority weapons.
 * It has a 50% chance of selling a copy of `my_ship_AnyVariant`, plus some random extra ships.
 * <pre>
 *     {
 *          "no_buy_message": "My faction only sells!"
 *          "weapons": {
 *              "count": {
 *                  "static": 3,
 *                  "per_size_min": 1,
 *                  "per_size_max": 2
 *              }
 *              "tier": {
 *                  "static": 5
 *              }
 *              "static": ["my_weapon"]
 *          },
 *          "ships": {
 *              "quality_bonus": 3,
 *              "combat": {
 *                  "per_size": 20
 *              }
 *              "static": ["my_ship_AnyVariant"],
 *              "cull_fraction": 0.5
 *          }
 *     }
 * </pre>
 * <p>
 * <b>Developer Note</b>
 * The code stores everything loaded from the json as transient, i.e. just for the current session.
 * This is so updates to the JSON are applied each launch - you don't get stuck with markets not selling a new ship.
 * <p>
 * Slightly annoyingly every submarket sharing the same id will have their own copy of the JSON,
 * but it's not exactly a memory hog.
 *
 * @author Toaster
 */
@SuppressWarnings("unused")
public class MagicSubmarket extends BaseSubmarketPlugin {
    public static final float DEFAULT_TARIFF = -1f;
    /// Defaults to parent market tariff.
    public static final String DEFAULT_MESSAGE = "You cannot trade this item with %s";
    ///  Message if something's banned, but there's no others.
    public static final String DEFAULT_NO_BUY = "";
    /// By default, you can always buy.
    public static final String DEFAULT_NO_SELL = "";
    /// By default, you can always sell.

    /**
     * Categories of item a submarket can sell
     */
    public enum Items {
        ships, tags, commodities, demand_classes, special_items, fighters, weapons, hullmods
    }

    /**
     * The loaded JSON file. All the transient entries here are just keys in it, but it's nicer to refer to them this way.
     */
    protected transient MagicMap jsonMap;

    public transient Map<String, String> validityCache;
    ///  Cache of messages for if an item is valid or not.

    protected transient float tariff;
    /// Tariff the player pays on purchases/sales.

    protected transient boolean inEconomy = false;
    /// Does this participate in the sector economy?

    protected transient boolean isBlackMarket = false;
    ///  Does this allow transponder-off trade?

    protected transient boolean sellsShips = true;
    ///  Does this market sell ships?

    protected transient boolean sellsCargo = true;
    ///  Does this market sell cargo?

    protected transient boolean learnsBlueprints = false;
    ///  Does the faction of this market learn blueprints sold to it?

    protected transient String hiddenKey = null;
    ///  The memory key storing whether this market is visible.

    protected transient String messageGeneric;
    /// The 'none of the above' message on things the player can't sell.

    protected transient String messageNoBuy;
    /// The message shown when the player tries to buy. If null, buying is fine.

    protected transient String messageNoSell;
    /// The message shown when the player tries to sell. If null, selling is fine.

    public MagicSubmarket() {
        super();
    }

    protected String getMessageNoBuy() {
        return messageNoBuy;
    }

    protected String getMessageNoSell() {
        return messageNoSell;
    }

    protected String getMessageGeneric(Object object) {
        return capFirst(messageGeneric.replace("%s", object.toString()));
    }

    @Override
    public boolean isBlackMarket() {
        if (jsonMap == null && !loadJSON()) return false;  // Should error if the JSON isn't loaded anyway
        return this.isBlackMarket;
    }

    @Override
    public boolean isOpenMarket() {
        if (jsonMap == null && !loadJSON()) return false;  // Should error if the JSON isn't loaded anyway
        return !this.isBlackMarket;
    }

    @Override
    public boolean showInFleetScreen() {
        if (jsonMap == null && !loadJSON()) return false;  // Should error if the JSON isn't loaded anyway
        return this.sellsShips;
    }

    @Override
    public boolean showInCargoScreen() {
        if (jsonMap == null && !loadJSON()) return false;  // Should error if the JSON isn't loaded anyway
        return this.sellsCargo;
    }

    /**
     * Does this market learn blueprints sold to it, like the pirates?
     *
     * @return True if the market learns blueprints sold to it.
     */
    public boolean learnsBlueprints() {
        return this.learnsBlueprints;
    }

    /**
     * Does this submarket take part in the global economy?
     *
     * @return By default, false.
     */
    @Override
    public boolean isParticipatesInEconomy() {
        if (jsonMap == null && !loadJSON()) return false;  // Should error if the JSON isn't loaded anyway
        return this.inEconomy;
    }

    /**
     * Gets whether the market is hidden from the player.
     *
     * @return False, or if hiddenKey exists the inverse of it in the closest scope (local, global).
     */
    @Override
    public boolean isHidden() {
        if (jsonMap == null && !loadJSON()) return false;  // Should error if the JSON isn't loaded anyway
        if (hiddenKey == null) return false;
        if (getMarket().getMemoryWithoutUpdate().contains(this.hiddenKey)) {
            return !market.getMemoryWithoutUpdate().getBoolean(this.hiddenKey);
        } else if(Global.getSector().getMemoryWithoutUpdate().contains(this.hiddenKey)) {
            return !Global.getSector().getMemoryWithoutUpdate().getBoolean(this.hiddenKey);
        }
        return true;
    }

    /**
     * Gets the id of the submarket, for loading from the right JSON.
     *
     * @return The id of the submarket this was created from.
     */
    public String getId() {
        if (getSubmarket() != null) {
            return getSubmarket().getSpecId();
        }
        return null;
    }

    @Override
    public void init(SubmarketAPI submarket) {
        super.init(submarket);
    }

    /**
     * Gets the tariff paid on sales and purchases on this submarket.
     *
     * @return The specified tariff, or parent market tariff if none.
     */
    @Override
    public float getTariff() {
        if (tariff >= 0f) {
            return tariff;
        } else {
            return getSubmarket().getMarket().getTariff().getModifiedValue();
        }
    }

    @Override
    public String getTooltipAppendix(CoreUIAPI ui) {
        RepLevel level = market.getFaction().getRelationshipLevel(Global.getSector().getFaction(Factions.PLAYER));
        return super.getTooltipAppendix(ui);
    }

    @Override
    public boolean isEnabled(CoreUIAPI ui) {
        return true;
    }


    /**
     * Loads the JSON for this submarket.
     *
     * @return Whether it loaded successfully (if it doesn't it should error, so should be irrelevant...)
     */
    public boolean loadJSON() {
        String jsonPath = "data/campaign/submarkets/" + getId() + ".json";

        try {
            JSONObject json = Global.getSettings().loadJSON(jsonPath, true);
            jsonMap = MagicMap.fromJSON(json);
        } catch (JSONException e) {
            Global.getLogger(MagicSubmarket.class).error("Failed to load JSON file: '"+jsonPath+"', exception: ", e);
            return false;  // Shouldn't be reached
        } catch (IOException e) {
            Global.getLogger(MagicSubmarket.class).error("Failed to load JSON file: '"+jsonPath+"', exception: ", e);
            return false;  // Shouldn't be reached
        }

        validityCache = new HashMap<>();
        messageGeneric = jsonMap.getStringOrDefault("message", DEFAULT_MESSAGE);
        messageNoSell = jsonMap.getStringOrDefault("no_sell_message", DEFAULT_NO_SELL);
        messageNoBuy = jsonMap.getStringOrDefault("no_buy_message", DEFAULT_NO_BUY);
        tariff = jsonMap.getFloatOrDefault("tariff", DEFAULT_TARIFF);
        inEconomy = jsonMap.getBoolOrDefault("in_economy", false);
        isBlackMarket = jsonMap.getBoolOrDefault("black_market", false);
        learnsBlueprints = jsonMap.getBoolOrDefault("learns_blueprints", false);
        sellsShips = jsonMap.containsKey("ships");
        if (sellsShips) sellsCargo = !jsonMap.getBoolOrDefault("ships_only", false);
        hiddenKey = jsonMap.getStringOrDefault("hidden_key", null);
        return true; // Should be irrelevant
    }

    /**
     * Every time the player visits, clears the auto-generated supplies and generates a custom set based on the JSON.
     *
     * @author Shoi (original, from Arma Armatura).
     * @author Toaster (modified).
     */
    @Override
    public void updateCargoPrePlayerInteraction() {
        if (this.jsonMap == null) {
            // On new game, it'll sometimes try to generate the cargo before the parent submarket is properly initialised.
            if (getId() == null) {
                sinceLastCargoUpdate = 999f;
                return;
            }
            loadJSON();
        }

        sinceLastCargoUpdate = 0f;

        if (okToUpdateShipsAndWeapons()) {
            int size = this.submarket.getMarket().getSize();
            float cullFraction = 0f;

            sinceSWUpdate = 0f;

            // Clear the auto-generated ships
            this.getCargo().getMothballedShips().clear();
            // Clear the existing weapon stockpile
            pruneWeapons(0f);

            // --------------------------------
            // Add ships
            // --------------------------------
            if (jsonMap.containsKey(Items.ships.name())) {
                MagicMap categoryMap = jsonMap.getMap(Items.ships.name());

                for (String itemId : categoryMap.getStringListOrDefault("static", List.of())) {
                    if (Global.getSettings().getVariant(itemId) != null) {
                        this.getCargo().addMothballedShip(FleetMemberType.SHIP, itemId, null);
                    } else {
                        Global.getLogger(MagicSubmarket.class).warn("Submarket: " + getId() + " - variant '" + itemId + "' does not exist");
                    }
                }
                // Add ships based on the faction doctrine
                addShips(
                        this.submarket.getFaction().getId(),
                        getScaledFloat(categoryMap.getMapOrDefault("combat", new MagicMap()), size),
                        getScaledFloat(categoryMap.getMapOrDefault("freighter", new MagicMap()), size),
                        getScaledFloat(categoryMap.getMapOrDefault("tanker", new MagicMap()), size),
                        getScaledFloat(categoryMap.getMapOrDefault("transport", new MagicMap()), size),
                        getScaledFloat(categoryMap.getMapOrDefault("liner", new MagicMap()), size),
                        getScaledFloat(categoryMap.getMapOrDefault("utility", new MagicMap()), size),
                        null,
                        categoryMap.getIntOrDefault("quality_bonus", 0),
                        ShipPickMode.PRIORITY_THEN_ALL,
                        this.submarket.getFaction().getDoctrine().clone()
                );
                // Then delete a random set of the ships
                pruneShips(1f - categoryMap.getFloatOrDefault("cull_fraction", 1f));
            }

            // --------------------------------
            // Add fighters
            // --------------------------------
            if (jsonMap.containsKey(Items.fighters.name())) {
                MagicMap categoryMap = jsonMap.getMap(Items.fighters.name());

                for (String itemId : categoryMap.getStringListOrDefault("static", List.of())) {
                    if (Global.getSettings().getFighterWingSpec(itemId) != null) {
                        this.getCargo().addFighters(itemId, 1);
                    } else {
                        Global.getLogger(MagicSubmarket.class).warn("Submarket: " + getId() + " - fighter wing '" + itemId + "' does not exist");
                    }
                }
                if (categoryMap.containsKey("count")) {
                    List<Integer> countRange = getScaledIntegerRange(
                            categoryMap.getMapOrDefault("count", new MagicMap()), size
                    );

                    addFighters(
                            countRange.get(0), countRange.get(1),
                            getScaledInteger(categoryMap.getMapOrDefault("tier", new MagicMap()), size),
                            submarket.getFaction().getId()
                    );
                }
                if (categoryMap.containsKey("cull_fraction")) cullFraction = categoryMap.getFloat("cull_fraction");
            }

            // --------------------------------
            // Add weapons
            // --------------------------------
            if (jsonMap.containsKey(Items.weapons.name())) {
                MagicMap categoryMap = jsonMap.getMap(Items.weapons.name());

                for (String itemId : categoryMap.getStringListOrDefault("static", List.of())) {
                    if (Global.getSettings().getWeaponSpec(itemId) != null) {
                        this.getCargo().addWeapons(itemId, 1);
                    } else {
                        Global.getLogger(MagicSubmarket.class).warn("Submarket: " + getId() + " - Weapon '" + itemId + "' does not exist");
                    }
                }
                if (categoryMap.containsKey("count")) {
                    List<Integer> countRange = getScaledIntegerRange(
                            categoryMap.getMapOrDefault("count", new MagicMap()), size
                    );

                    addWeapons(
                            countRange.get(0), countRange.get(1),
                            getScaledInteger(categoryMap.getMapOrDefault("tier", new MagicMap()), size),
                            submarket.getFaction().getId()
                    );
                }
                if (categoryMap.containsKey("cull_fraction")) cullFraction = categoryMap.getFloat("cull_fraction");
            }

            // --------------------------------
            // Add hullmods
            // --------------------------------
            if (jsonMap.containsKey(Items.hullmods.name())) {
                MagicMap categoryMap = jsonMap.getMap(Items.special_items.name());

                for (String itemId : categoryMap.getStringListOrDefault("static", List.of())) {
                    if (Global.getSettings().getHullModSpec(itemId) != null) {
                        this.getCargo().addHullmods(itemId, 1);
                    } else {
                        Global.getLogger(MagicSubmarket.class).warn("Submarket: " + getId() + " - hull mod '" + itemId + "' does not exist");
                    }
                }
                if (categoryMap.containsKey("count")) {
                    addHullMods(
                            getScaledInteger(categoryMap.getMapOrDefault("tier", new MagicMap()), size),
                            getScaledInteger(categoryMap.getMapOrDefault("count", new MagicMap()), size)
                    );
                }
            }
            pruneWeapons(1f - cullFraction);
        }
        getCargo().sort();
    }

    /**
     * Gets the message explaining why an item is illegal, if any.
     *
     * @param typeMap  A dict describing a set of limits (blacklist and/or whitelist).
     * @param itemId   The id of the item to check.
     * @param itemName The name of the item, to stick in the message.
     * @return Either "" if it's legal, or the message why it's illegal.
     */
    protected String getTypeValidMessage(MagicMap typeMap, String itemId, String itemName) {
        if (typeMap.containsKey("whitelist")) {
            MagicMap limitMap = typeMap.getMap("whitelist");
            if (!limitMap.getStringSet("ids").contains(itemId)) {
                if (itemName != null) {
                    return limitMap.getString("message").replace("%s", itemName);
                } else {
                    return limitMap.getString("message");
                }
            }
        }
        if (typeMap.containsKey("blacklist")) {
            MagicMap limitMap = typeMap.getMap("blacklist");
            if (limitMap.getStringSet("ids").contains(itemId)) {
                if (itemName != null) {
                    return limitMap.getString("message").replace("%s", itemName);
                } else {
                    return limitMap.getString("message");
                }

            }
        }
        return "";
    }

    /**
     * Gets whether a set of tags is valid.
     *
     * @param tagMap The dict containing the tag black and whitelists.
     * @param tags   The set of tags of the item.
     * @return Either "" if the item is legal, or the tag invalid message.
     */
    protected String getTagsValidMessage(MagicMap tagMap, Set<String> tags) {
        if (tagMap.containsKey("whitelist")) {
            MagicMap limitMap = tagMap.getMap("whitelist");
            if (!new HashSet<>(tags).removeAll(limitMap.getStringSet("ids"))) {
                // As in, if when removing whitelist tags no tags are removed
                return limitMap.getString("message");
            }
        }
        if (tagMap.containsKey("blacklist")) {
            MagicMap limitMap = tagMap.getMap("blacklist");
            if (new HashSet<>(tags).removeAll(limitMap.getStringSet("ids"))) {
                // As in, if when removing blacklist tags any are removed
                return limitMap.getString("message");
            }
        }
        return "";
    }

    /**
     * Filters out banned special items & weapons.
     *
     * @param stack  Stack of cargo items to check.
     * @param action What's the player trying to do with them?
     * @return True if it's blocked on this submarket.
     */
    @Override
    public boolean isIllegalOnSubmarket(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_BUY && !getMessageNoBuy().isEmpty()) return true;
        if (action == TransferAction.PLAYER_SELL && !getMessageNoSell().isEmpty()) return true;

        String itemId = "";
        if (stack.getHullModSpecIfHullMod() != null) {
            itemId = stack.getHullModSpecIfHullMod().getId();
        } else if (stack.isSpecialStack()) {
            itemId = stack.getSpecialItemSpecIfSpecial().getId();
        } else if (stack.isFighterWingStack()) {
            itemId = stack.getFighterWingSpecIfWing().getId();
        } else if (stack.isWeaponStack()) {
            itemId = stack.getWeaponSpecIfWeapon().getWeaponId();
        }

        if (!itemId.isEmpty()) {
            if (!validityCache.containsKey(itemId)) {
                String cacheMessage = "";
                Set<String> tags = new HashSet<>();

                if (stack.getHullModSpecIfHullMod() != null) {
                    tags.addAll(stack.getHullModSpecIfHullMod().getTags());
                    if (jsonMap.containsKey(Items.hullmods.name())) {
                        String limitMessage = getTypeValidMessage(
                                jsonMap.getMap(Items.hullmods.name()),
                                itemId, stack.getDisplayName()
                        );
                        if (!limitMessage.isEmpty()) cacheMessage = limitMessage;
                    }

                } else if (stack.isWeaponStack()) {
                    tags.addAll(stack.getWeaponSpecIfWeapon().getTags());
                    if (jsonMap.containsKey(Items.weapons.name())) {
                        String limitMessage = getTypeValidMessage(
                                jsonMap.getMap(Items.weapons.name().toLowerCase()),
                                itemId, stack.getDisplayName()
                        );
                        if (!limitMessage.isEmpty()) cacheMessage = limitMessage;
                    }

                } else if (stack.isFighterWingStack()) {
                    tags.addAll(stack.getFighterWingSpecIfWing().getTags());
                    if (jsonMap.containsKey(Items.fighters.name())) {
                        String limitMessage = getTypeValidMessage(
                                jsonMap.getMap(Items.fighters.name()),
                                itemId, stack.getDisplayName()
                        );
                        if (limitMessage.isEmpty()) cacheMessage = limitMessage;
                    }

                } else if (stack.isSpecialStack()) {
                    tags.addAll(stack.getSpecialItemSpecIfSpecial().getTags());
                    if (jsonMap.containsKey(Items.special_items.name())) {
                        String limitMessage = getTypeValidMessage(
                                jsonMap.getMap(Items.special_items.name()),
                                itemId, stack.getDisplayName()
                        );
                        if (!limitMessage.isEmpty()) cacheMessage = limitMessage;
                    }

                } else if (stack.isCommodityStack()) {
                    tags.addAll(
                            Global.getSettings().getCommoditySpec(stack.getCommodityId()).getTags()
                    );
                    // Rest of this logic is handled elsewhere
                }
                if (jsonMap.containsKey(Items.tags.name())) {
                    String limitMessage = getTagsValidMessage(
                            jsonMap.getMap(Items.tags.name()),
                            tags
                    );
                    if (!limitMessage.isEmpty()) cacheMessage = limitMessage;
                }
                if (cacheMessage.isEmpty()) {
                    if (super.isIllegalOnSubmarket(stack, action)) {
                        cacheMessage = getMessageGeneric(getName());
                    }
                }
                if (!cacheMessage.isEmpty()) cacheMessage = capFirst(cacheMessage);
                validityCache.put(itemId, cacheMessage);
            }
            return !validityCache.get(itemId).isEmpty();

        } else {
            return super.isIllegalOnSubmarket(stack, action);
        }
    }

    /**
     * Filters out commodities that are from a banned demand class.
     *
     * @param commodityId The ID of the commodity.
     * @param action      What the player is trying to do.
     * @return True if it's a banned commodity.
     */
    @Override
    public boolean isIllegalOnSubmarket(String commodityId, SubmarketPlugin.TransferAction action) {
        if (action == TransferAction.PLAYER_BUY && !getMessageNoBuy().isEmpty()) return true;
        if (action == TransferAction.PLAYER_SELL && !getMessageNoSell().isEmpty()) return true;
        if (!validityCache.containsKey(commodityId)) {
            String cacheMessage = "";
            CommoditySpecAPI commoditySpec = Global.getSettings().getCommoditySpec(commodityId);

            if (jsonMap.containsKey(Items.commodities.name())) {
                String limitMessage = getTypeValidMessage(
                        jsonMap.getMap(Items.commodities.name()),
                        commodityId, commoditySpec.getName()
                );
                if (!limitMessage.isEmpty()) cacheMessage = limitMessage;
            }
            if (jsonMap.containsKey(Items.demand_classes.name())) {
                String limitMessage = getTypeValidMessage(
                        jsonMap.getMap(Items.demand_classes.name()),
                        commoditySpec.getDemandClass(), null
                );
                if (!limitMessage.isEmpty()) cacheMessage = limitMessage;
            }
            if (jsonMap.containsKey(Items.tags.name())) {
                String limitMessage = getTagsValidMessage(
                        jsonMap.getMap(Items.tags.name()),
                        new HashSet<>(commoditySpec.getTags())
                );
                if (!limitMessage.isEmpty()) cacheMessage = limitMessage;
            }

            if (cacheMessage.isEmpty()) {
                if (super.isIllegalOnSubmarket(commodityId, action)) {
                    cacheMessage = getMessageGeneric(getName());
                }
            }
            if (!cacheMessage.isEmpty()) cacheMessage = capFirst(cacheMessage);
            validityCache.put(commodityId, cacheMessage);
        }
        return !validityCache.get(commodityId).isEmpty();
    }

    /**
     * Filters out ships of banned hulls or with forbidden tags.
     *
     * @param member The fleet member being transferred.
     * @param action What the player is trying to do.
     * @return True if it's a banned ship.
     */
    @Override
    public boolean isIllegalOnSubmarket(FleetMemberAPI member, TransferAction action) {
        if (action == TransferAction.PLAYER_BUY && !getMessageNoBuy().isEmpty()) return true;
        if (action == TransferAction.PLAYER_SELL && !getMessageNoSell().isEmpty()) return true;

        if (!validityCache.containsKey(member.getId())) {
            String cacheMessage = "";

            if (jsonMap.containsKey(Items.ships.name().toLowerCase())) {
                String limitMessage = getTypeValidMessage(
                        jsonMap.getMap(Items.ships.name().toLowerCase()),
                        member.getHullId(), member.getHullSpec().getHullName()
                );
                if (!limitMessage.isEmpty()) cacheMessage = limitMessage;
            }
            if (jsonMap.containsKey(Items.ships.name().toLowerCase())) {
                Set<String> tags = new HashSet<>();
                tags.addAll(member.getVariant().getTags());
                tags.addAll(member.getHullSpec().getTags());

                String limitMessage = getTagsValidMessage(
                        jsonMap.getMap(Items.ships.toString().toLowerCase()),
                        tags
                );
                if (!limitMessage.isEmpty()) cacheMessage = limitMessage;

            }
            if (cacheMessage.isEmpty()) {
                if (super.isIllegalOnSubmarket(member, action)) {
                    if (!getMessageGeneric(getName()).isEmpty()) {
                        cacheMessage = getMessageGeneric(getName());
                    } else {
                        cacheMessage = getIllegalTransferText(member, action);
                    }
                }
            }
            if (!cacheMessage.isEmpty()) cacheMessage = capFirst(cacheMessage);
            validityCache.put(member.getId(), cacheMessage);
        }
        return !validityCache.get(member.getId()).isEmpty();
    }

    /**
     * Tooltip message for illegal ship transfers.
     *
     * @param member The fleet member being transferred.
     * @param action The transfer action.
     * @return String explaining why.
     */
    @Override
    public String getIllegalTransferText(FleetMemberAPI member, TransferAction action) {
        return validityCache.getOrDefault(
                member.getId(), super.getIllegalTransferText(member, action)
        );
    }

    /**
     * Tooltip message for illegal item transfers.
     * <p>
     * Custom response per blacklisted class.
     *
     * @param stack  The illegal stack of items.
     * @param action The action the player is performing.
     * @return String explaining why.
     */
    @Override
    public String getIllegalTransferText(CargoStackAPI stack, TransferAction action) {
        if (action == TransferAction.PLAYER_BUY && !getMessageNoBuy().isEmpty()) return getMessageNoBuy();
        if (action == TransferAction.PLAYER_SELL && !getMessageNoSell().isEmpty()) return getMessageNoSell();

        String validityCacheKey = "";
        if (stack.getHullModSpecIfHullMod() != null) {
            validityCacheKey = stack.getHullModSpecIfHullMod().getId();
        } else if (stack.isSpecialStack()) {
            validityCacheKey = stack.getSpecialItemSpecIfSpecial().getId();
        } else if (stack.isCommodityStack()) {
            validityCacheKey = stack.getCommodityId();
        } else if (stack.isWeaponStack()) {
            validityCacheKey = stack.getWeaponSpecIfWeapon().getWeaponId();
        } else if (stack.isFighterWingStack()) {
            validityCacheKey = stack.getFighterWingSpecIfWing().getId();
        }
        if (!validityCacheKey.isEmpty() && this.validityCache.containsKey(validityCacheKey)) {
            return this.validityCache.getOrDefault(validityCacheKey, super.getIllegalTransferText(stack, action));
        } else {
            return getMessageGeneric(getSubmarket().getName());
        }
    }

    /**
     * Wraps the 'transaction complete' reporter to see if the faction should learn blueprints as a result.
     *
     * @param transaction The transaction the player just performed.
     * @author Alex
     */
    @Override
    public void reportPlayerMarketTransaction(PlayerMarketTransaction transaction) {
        super.reportPlayerMarketTransaction(transaction);

        if (this.learnsBlueprints()) {
            FactionAPI faction = submarket.getFaction();
            BlackMarketPlugin.delayedLearnBlueprintsFromTransaction(
                    faction, getCargo(), transaction, 60f + 60 * (float) Math.random()
            );
        }
    }

    /**
     * Gets a float value from a definition and size.
     *
     * @param valueMap A dict containing keys "static" and/or "per_size".
     * @param size     The size of the market, to scale.
     * @return The scaled value.
     */
    protected float getScaledFloat(MagicMap valueMap, int size) {
        return valueMap.getFloatOrDefault("static", 0) + valueMap.getFloatOrDefault("per_size", 0) * size;
    }

    /**
     * Gets an integer value from a definition and size.
     *
     * @param valueMap A dict containing keys "static" and/or "per_size".
     * @param size     The size of the market, to scale.
     * @return The scaled value.
     */
    protected int getScaledInteger(MagicMap valueMap, int size) {
        return valueMap.getIntOrDefault("static", 0) + valueMap.getIntOrDefault("per_size", 0) * size;
    }

    /**
     * Gets a float value from a definition and size.
     *
     * @param valueMap A dict containing keys "static" and/or "per_size_min" & "per_size_max".
     * @param size     The size of the market, to scale.
     * @return The scaled value.
     */
    protected List<Integer> getScaledIntegerRange(MagicMap valueMap, int size) {
        return List.of(
                valueMap.getIntOrDefault("static", 0) + valueMap.getIntOrDefault("per_size_min", 0) * size,
                valueMap.getIntOrDefault("static", 0) + valueMap.getIntOrDefault("per_size_max", 0) * size
        );
    }

    /**
     * Capitalises the first letter of a string.
     * <p>
     * Should probably be in a utility file somewhere.
     *
     * @param string String to capitalise
     * @return Input, with first letter capitalised.
     */
    protected static String capFirst(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }
}
