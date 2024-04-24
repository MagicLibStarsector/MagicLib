# README

The goal of MagicLib is to create a community-built library of useful (and well documented) scripts and plugins that can
be leveraged and contributed to by every modder.

To get your stuff added to MagicLib, please contact Wisp

...on the Unofficial Starsector Discord:
http://fractalsoftworks.com/forum/index.php?topic=11488.0

...on the official forum:
https://fractalsoftworks.com/forum/index.php?topic=25868.0

All contributions must be fully documented on the Wiki and given proper Javadocs:
https://starsector.wiki.gg/wiki/MagicLib

View the Javadoc: https://magiclibstarsector.github.io/MagicLib/

## Combat Activators to MagicSubsystems Migration Guide

- Rename from Activators to MagicSubsystems.
    - Change your imports from `activators` to `org.magiclib.subsystems`.
    - Change `ActivatorManager` to `MagicSubsystemsManager`.
    - Change `CombatActivator` to `MagicSubsystem`.
    - (etc)
- `advance(float amount)` and `advanceEveryFrame()` are now a single method, `advance(float amount, boolean isPaused)`.
    - All logic that was in `advanceEveryFrame` before should move to `advance`, which is now called even when paused.
    - All logic that was in `advance` before should have `if (!isPaused)` added around it.
- `ActivatorManager.addActivator` has been renamed to `MagicSubsystemsManager.addSubsystemToShip`.
  - Same with similar methods.

Bonus: Kotlin extension methods for adding/removing subsystems have been added on `ShipAPI`.


## Todo add this to the wiki

for the new magiclib version there's new ordering parameter
```
MagicSubsystem.getOrder()
    protected static int ORDER_MOD_MODULAR = 4;
    protected static int ORDER_MOD_UNIQUE = 5;
    protected static int ORDER_FACTION_MODULAR = 6;
    protected static int ORDER_FACTION_UNIQUE = 7;
    protected static int ORDER_SHIP_MODULAR = 8;
    protected static int ORDER_SHIP_UNIQUE = 9;
```
these are the suggested values but you can use anything
the default is 0
if two subsystems have the same "Order", then it picks alphabetically by display name
highest "Order" gets first key index and renders first