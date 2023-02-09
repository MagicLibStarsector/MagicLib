# MagicLib Bounties

## TODO

- Done: Handle where a fleet is spawned (when a bounty is viewed) but not accepted, and the fleet is destroyed (not by
  the player) without player ever having accepted the bounty.
    - Resolution: Bounty fleets are no longer spawned on the map until bounty is accepted.
    - The fleet and location will still be persisted if the user does not accept, preventing scumming.