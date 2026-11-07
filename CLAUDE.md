# MagicLib

MagicLib is a library mod that many Starsector mods depend on. Treat every change as **backwards-compatible** by default:

- Keep public classes, methods, and signatures as they are. Add an overload or a new method instead of changing an old one.
- Keep save data compatible. Anything MagicLib stores in saves must still load in old and new versions, and avoid introducing anything into the save whenever possible.
- The deprecated `data.scripts.*` package mirrors `org.magiclib.*`, and some old mods still use it, but it's considered archived.
- Be careful, conservative, and rigorous in this codebase. Test religiously and avoid assumptions. Consider side effects.

## Branches and remotes

- Work in `dev` or a new branch. `master` is the last public release's code.
- `GitHub_MagicLib` is the main repo (MagicLibStarsector/MagicLib). The other remotes are contributors' forks.
- The `gh` CLI is not installed. Read issues and PRs through the public GitHub REST API (`Invoke-RestMethod` in PowerShell). Posting, labeling, or closing on GitHub needs the user's OK first.

## Building

- `settings.gradle.kts` sets `starsectorPath = "../../"`, which assumes the repo sits in the game's `mods` folder.
- `./gradlew jar` writes `jars/MagicLib.jar`. Jars are not tracked in git.
- `MagicVariables.verbose` is `true` whenever the game is in dev mode.

## Changelog

Never touch `CHANGELOGS.md`. You may propose changelog entries to the user in chat.

## Fixing a bug

Test every fix in the real game both before and after:

1. Write a check group that fails on the bug. Add "setup" checks that prove the test did what it meant to, and "unchanged" checks for nearby behavior that must stay the same.
2. Run it on an unfixed build and see the bug checks go red.
3. Fix, rebuild, and run the same groups on the fixed build. Think about and look for potential side effects.
4. For anything a player can see, capture before and after screenshots of the real game screen.
5. Report to the user: the before/after check table, the screenshots, and what changed.
