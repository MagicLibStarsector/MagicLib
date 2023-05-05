MagicLib

1.0.0

**MagicBounty**
- Fixed intel showing # ships changing each time it's viewed (reported by Selkie).
- `\n` now creates new paragraphs consistently (reported by vicegrip).
- 

0.46.1

**Other**
  - Added HMI themes to blacklist so that they are considered "already occupied".
  - Reduced some logspam with devmode on (error -> info/warn).
**MagicCampaign**
  - Added `createCaptainBuilder` as a replacement to `createCaptain` (non-breaking change).
**MagicBounty**
  - Added `fleet_musicSetId` for custom bounty battle music (suggested/contributed to by NiaTahl).
    - See `data/config/magicBounty_data_example.json` for usage. 
  - Renamed `fleet_flagship_recoverable` to `fleet_flagship_alwaysRecoverable` (backwards compatible).
  - List Requirements command now shows `trigger_memKeys_none`.
  - Fixed `Tags.VARIANT_ALWAYS_RECOVERABLE` not being added to the flagship when `fleet_flagship_alwaysRecoverable` is `true`.
  - `vanilla` and `vanillaDistance` target distance options now point to the constellation in the bar event (suggested by Avanitia).
  - Comms replies now support highlights (same syntax, wrap with `==`) (suggested by raycrasher).
**Kotlin**
  - Added `prepareShipForRecovery` extension.

0.46.0 - (the first Wisp release 🤞)

**Other**
  - Changed MagicLib's `mod_info.json` version format to the `major/minor/patch` object format instead of just a string.
    - Mod authors: I recommend changing to `"version": {"major":0, "minor":46, "patch":0}` in your `dependencies` section, as it fixes MagicLib updates showing as incompatible when only the minor version changes.

**New: Kotlin Extensions**
  - Added a new jar, `MagicLib-Kotlin.jar`, containing Kotlin-only extension methods.
    - To use, include the jar in your Kotlin-using project. There is no point for Java-only projects.

**MagicAsteroids**
  - No longer added to the save file.
  - Fixes a bug causing asteroid impacts to stop happening.

**MagicBounty**
  - `job_reputation_reward` may now be negative. Failing a bounty with a negative rep reward will result in 0 rep change.
  - HVBs no longer have a time limit of 1 cycle (they have no time limit in Vayra's Sector).
  - `job_show_distance` has a new option, `system`. "The target is located in the <system> system."
  - `MagicLib_ListBounties` is now sorted alphabetically.
  - `MagicLib_ListBounties <bountyKey>` now displays details for that bounty. 
  - Fixed bug reading HVBs where `neverSpawnWhenFactionHostile` used target faction instead of posting faction.
  - Fixed bug in `MagicList_ResetBounty` where the bounty wasn't reset if the Intel hadn't yet expired.
  - Fixed bug where resetting and re-accepting bounties with a typo in the faction caused a crash (typo wasn't corrected second time).
  - Fixed `fleet_no_retreat` allowing individual ships to retreat.
  - Fixed `job_show_arrow` only displaying if `job_show_distance` was set to `exact`. Points to system or constellation depending on `job_show_distance`.
  - Fixed a bounty offer showing the exact system if `job_show_distance` was `vanilla` or `vanilla_distance`. No longer shows map when offering bounty (can't point to constellation there).

**MagicCampaign**
  - Added `org.magiclib.campaign.MagicFleetBuilder`, a new, more configurable way to build a fleet. 
  - Added default values for all parameters in `createFleet`.

**MagicTrail**
  - Fixed campaign trails (by President Matt Damon). 

**MagicUI**
  - Added overloads for manually positioning status and system bar (by President Matt Damon and Timid).
  - Fixed dual system ships having overlap with custom status bar (by President Matt Damon and Timid).

0.45.2

- MagicBounties:
  - Fixed accidentally-introduced backwards incompatibility.

0.45.1

- MagicAsteroid plugins updated for SafariJohn
- MagicCampaign/MagicBounty:
  - Finding target objects now supports defaulting to any system if requested themes could not be found.

0.44.1

- Added MagicAsteroid plugins to restore functionalities related to asteroids that have been removed in the vanilla game (by SafariJohn)
- MagicIncompatibleHullmod:
  - Now handles hullmods with scripted incompatibilities that have been S-modded by removing the incompatible hullmod added instead of itself. (Thanks to Timid)

- MagicCampaign:
  - createFleet now handles fleets that have a spawn location but no assignment target.

- MagicBounty: 
  - Hopefully fixed the log spam after a bounty expired.

0.43.1

- [RC3] fixed a typo that prevented hvb's from showing up after loading a save.

- MagicBounties:
  - Fixed an issue with HVBs memkeys improperly set for trigger requirements,
  - Probably fixed the intel spam when ending some bounties (please report it to me immediately if it still occurs after the patch!),
  - Fixed some memkeys requirements not evaluated properly when combined together,
  - Weapons and hullmods can be offered as extra rewards,
  - Extra rewards are no longer given for bounties failed through flagship recovery.

- MagicVariables:
  - Changed the way they are pulled to ensure a proper evaluation beforehand.

- MagicRender: 
  - Fixed issue where objectspace sprite jittering was not applied correctly.

MagicIndustryItemWrangler: 
  - Code cleanup.

0.42

- [RC6] Fixed an issue with time threshold evaluation for bounties (that one is on Alex' undocumented API!)
- [RC5] Fixed a crash occurring when bounties have a smaller reinforcement size than their preset size.
- [RC3] Fixed a very stupid mistake that let MagicBounty detect invalid bounties, but not properly remove them.

- MagicBounties:
  - Added [fleet_no_retreat] boolean parameter.
  - Finally bit the bullet and added a thorough bounty validation on game load. This should solve pretty much all issues remaining with invalid bounties. 
  - Dev-mode log will precisely indicate why bounties can't be loaded so that bounty writers can quickly fix them.
  - Fixed various triggers not being evaluated properly when combined with each-other.
  - Added the console command [MagicBounty_ListRequirements] to check where and when you are able to see the bounties.
  - Dismissed bounty are now properly cleaned up.
  - Completed bounties are properly cleaned up.
  - [MagicLib_ResetBounty] console command now works properly.
- MagicCampaign:
  - Fixed an issue when generating captains with custom skill sets that prevented all skills from being assigned.

0.41

[RC2]
- MagicBounties:
  - Further work on properly converting HVBs. 
  - Fixed overzealous "invalid" bounty checker.
  - Improved difficulty rating.
  - Fixed some errors related to incorrect fleet and reward scaling.
  - More informative logs in dev mode to see what is going on while writing bounties.
- MagicCampaign:
  - Fixed the fleet quality thing that I broke when I tried to make it toggleable on the flagship/escort, that I repaired in 0.40 but was messing up the Flagship/escort loadouts, that never worked before... It's been a journey.


- Added MagicIndustryItemWrangler:
  - Courtesy of Wyvern.
  - Allows mods to define "rating" for their items that can be mounted in the same industries as other items.
  - When an item with higher priority is sold at a public market, the AI will replace the existing one in its industry.
  - Works on both vanilla and modded industries.
  - Items priority is defined in modSettings.json

- MagicBounties:
  - MagicBounties no longer requires a modSettings.json entry.
  - WARNING: HOW BOUNTIES DEFINE MOD REQUIREMENTS HAS BEEN CHANGED!
 PLEASE REFER TO THE EMPTY BOUNTY FILES IN THE MOD TO ADD THE NEW MOD REQUIREMENT PARAMETER.
  - Removed Bounties Expanded hvb check.
  - Added parameters to magicBounty_data to force the autofitter on the flagship/preset ships.
  - Variables should now properly get replaced in the conclusion intel.
  - Fixed another source of crashes from HVB conversions.
  - Accepted bounties are now placed in the "MISSION" intel tab rather than bounties.
  - Added another blacklist tag to prevent having bounties in Blackhole or Pulsar systems.

- MagicCampaign:
  - Fixed an issue with the quality setting that was modifying the Flagship and Preset ships loadouts to fit its value,
  - Flagship and escort ships now keep their original variants unless the autofitter is specifically allowed to alter them.

0.4

[RC3]
- MagicCampaign.placeOnStableOrbit(): Fixed not one but two effing stupid mistakes! Affected both the Diable Avionics unique ships and the Plague-Bearer ones.
- MagicBounty HVBs: improved the validation check so that it does not freak out on empty fields.

[RC2]
- MagicCampaign.spawnFleet(): Patrol assignment should now be properly respected. (also affects MagicBounty ROAMING assignment)
- MagicBounty HVBs: Added many checks to prevent invalid bounties from being offered.
- MagicBounty bar event: added map direction like vanilla missions, modified the difficulty description to be more accurate. 
- MagicBounty: Fixed several sources of crashes, added more fail-safes to prevent hard crashes in case of mistakes present in the bounty definition.

- Added MagicBounty framework, a highly customizable yet easy to implement system to add unique story-driven bounties to a board present in bars.
- Huge thanks to Wisp that did the bulk of the work, to Schaf-Unschaf and Rubi for their contributions.

- Added a merged "occupied" system themes list to MagicSetting, for finding suitable places where to spawn exploration content. Currently adds all the Remnant tags, Blade-Breaker, OCI and Plague-Bearer tags.

- MagicRender: Fixed issue where sprite flickering was applied incorrectly.
- MagicLensFlare: Now uses the "visual only" API.
- MagicSettings: Improved error messages with the lines where the issue is if possible.
- MagicTargeting: Added the option to exclude flares from missile picks.
- MagicCampaign: Many small improvement to the logic of nearly all functions. Better checks for suitable systems, more coherent fleet spawning, Fleet quality is now a thing, made a lot of parameter nullable for procgen filler...
- MagicUI: the widgets texts can be null to be ignored, same for numbers if they are negative.


0.34

- MagicRender: Singleframe render is now maintained while the game is paused
- Magic UI: Should be displayed sharp at 100% UI scaling
- MagicTrail:
  - Now properly supports texture scrolling whether the source is moving or not.
  - Now supports one-time random texture offsets.
  - Now supports per-segment texture offsets.
  - MagicTrail.csv updated with a random trail offset boolean.
  - Big thanks to Originem for fixing the scrolling issue as well as doing a massive optimization work, and making these new features possible.
- MagicLensFlare: createSmoothFlare() temporarily deprecated due to changes in 0.95 breaking them.

- Added some test plugin to add depth to combat nebulae. Disabled by default due to its jankyness, but can be enabled in the settings if you so desire.

0.33

- MagicUI: Should work with UI scalling now
- MagicCampaign:
 - Fixed AI-core captains crashing the game upon looting their fleet,
 - Deprecated the old captain declaration that still works for human officers
 - Added support for 0.95 automatic levels and skills.

0.32

- Basic 0.95 compatibility update
- MagicUI is probably broken to hell and back due to the UI scaling, gotta fix that soon

0.31

- Added MagicCampaign:
  - A collection of methods that are handy to create systems and spawn stuff in them.
  - Also includes a method to create custom bounty-type fleets and give them simple orders.

- Consolidated a lot of methods with incrementally more detailed declaration variable into fewer ones, 
the old methods are still available to maintain compatibility but are now tagged as deprecated and will be removed in a future update.

0.30

- Added MagicSettings:
  - A collections of methods to easily read variables and lists from a shared modSettings.json file.
  - It is intended to create a unified settings system across mods to make inter-mod integration much easier.
  - Produces helpful error messages when failing, as well as a detailed log when in dev-mode.

- MagicRender:
  - Now supports sprite flickering and jittering.

- MagicTrails:
  - Added "base_trail_contrail" to default available trails.
  - Added compensation attribute to MagicAutoTrails to fix offset following projectiles with fast lateral drift. (courtesy of TomatoPaste)
  - Added a non-verbose implementation of MagicAutoTrails used when not in dev Mode.

- MagicVectorThrusters:
  - Probably fixed a long standing rotation error with the vectoring thruster script
  - Added support for vector thruster covers (non animated thrusters will only turn).

- MagicInterferencePlugin:
  - Fixed minor error in the Interference hullmod tooltip.
  - Added a non-verbose implementation used when not in dev Mode.

0.29

- Added MagicInterferencePlugin
  - Makes exceptionally strong weapons with the "Interference" trait have negative effects when more that one is mounted on a given ship.
  - Those weapons need to trigger the plugin to check for possible other interference sources from a CSV file.
  - If other sources are found, it will add a hullmod that reduced the ship's dissipation depending on the number of interfering weapons and their interference strength.

- Added MagicBasicInterferenceEffec loose weapon script to trigger an interference check.

- Added MagicIncompatibleHullmods
  - Proposes a uniform "incompatible hullmod" solution.
  - When triggered as two incompatible hullmods are added, it will remove the offending hullmod and add a hullmod to warn the player about that operation.
  - The added hullmod's tooltip will indicate which hullmod was removed and why.

- Added MagicModuleRetreatCleaner
  - Fixes the bug with retreating ships with modules preventing the combat from ending. 

- MagicRender:
  - Added optional blending modes to the sprite renders.

- MagicUI:
  - Fixed crash when used the first frame in combat.

0.28

- Added MagicGuidedProjectileScript loose weapon script

- MagicAnim:
  - Added cycle(float x, float min, float max)


0.27

- MagicRender:
  - Fixed screencheck culling being overly aggressive.

- MagicTrail:
  - Fixed case issue for Linux players.



0.26

- MagicTrail:
  - Now supports render order overrides.

- MagicRender:
  - Now supports render order overrides.

- MagicTrail CSV plugin:
  - Added renderBelowExplosion boolean to automatically render the trail under the FX layer
  


0.25

RC2

- MagicTrail CSV plugin:
  - Finally NAILED THAT FRICKING BUG WITH RON'S EDITOR CORRUPTING THE FILES! 
  - (and also future proofed the CSV read process, thanks Kitta Khan for the help)

- MagicTrail CSV plugin:
  - Added a bunch of default textures.
  - Fixed trail duplication bug on save reload from a battle.
  - Fixed trail_data corruption using Ron's editor

- MagicFakeBeam:
  - Added spawnAdvancedFakeBeam that can use custom vanilla-like textures.



0.24

- MagicTrail CSV plugin:
  - Added a velocity randomization parameter.

- MagicTargeting:
  - Fixed issue with random missile targeting only working in the front of the source ship.



0.23

RC3

- MagicTrail CSV plugin:
  - Fixed issue with multiple trails getting weird offsets
  - Added second dispersion method to create non-linear offsets

- Added CSV-based projectile trail manager. (see data/trails/trail_data.csv)

- MagicTrails:
  - Now uses an alternate rendering method which should fix a variety of issues
  - The old render version is available under "specialOptions", as the bool "FORWARD_PROPAGATION" (default false)

- MagicUI:
  - Added two new methods to display full fat status bar in the UI and the player ship hud widget.

- MagicRender:
  - Fixed issue with sprite attached to fading projectiles.



0.22

- MagicTargeting:
  - Missile targeting now ignores missiles without collisions

- All scripts now use simplified PI from LazyLib.



0.20

- Compiled for Starsector 0.9.0a

CONTENT:

- MagicTrail (by Nicke)
  - Draws missile-like trails from arbitrary coordinates with a ton of fancy options to play with.



0.12

CONTENT:

- MagicUI (from Dark.Revenant)
  - Draws a small UI bar/tick box near the ship system for custom dual-system charge-bar

- MagicMissileAI (loose script)
  - Fast and easily customizable missile AI

BUGFIXES/IMPROVEMENTS:

- MagicTargeting
  - Added a superior simpler function for targeting from both missile and ships, with fallback target option. 



0.10

CONTENT:

- MagicAnim
  - A collection of functions to make smooth animations.

- MagicFakeBeam
  - Creates convincing ponctual beams from arbitrary coordinates.

- MagicLensFlare
  - Creates "cinematic" lensflares.

- MagicRender
  - Draw arbitrary sprites on screen with constraints to entities/camera when needed. (aka "SpriteRenderManager")
    - Also has a screencheck function

- MagicTargeting
  - Allows "smart" target selection for systems and missiles within distance and search cone parameters, plus it can use ship-class preferences.

- MagicVectorThruster (loose script)
  - Manages vectoring or vernier-style attitude thrusters. 