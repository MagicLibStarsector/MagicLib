//Automatically points to the starsector folder if the mod is placed in to the "mods" folder.
//If you do not place the project in to your mods folder, replace this with the path to Starsectors root folder.
val starsectorPath= "../../";

//The name of the file that the code is compiled to. This will automatically place in to the /jars folder.
//Make sure that the "jars" entry in your mod_info.json matches this.
val jarName = "MagicLib.jar"

//Name for the Zip that is created when you run package_mod.bat.
//This zip includes the data, graphics, jars, sounds and src folder.
//It also includes the mod_info.json and .version files at the root folder.
val zipName = "MagicLib.zip"

//Other mods to load as compile-time dependencies. Adding them will provide auto-complete for their functions.
//Each entry is the jar name. The build searches every mod /jars/ folder for a matching file ("LazyLib.jar" -> "Starsector/mods/LazyLib/jars/LazyLib.jar")
//Mods added this way still need to be added to mod_info.json if they are always required (hard-dependency).
val modDependencies = listOf(
    "LazyLib.jar", //LazyLib
    "LazyLib-Kotlin.jar",

    "LunaLib.jar", //LunaLib

    "Graphics.jar", //GraphicsLibs

    "lw_Console.jar", //Console Commands

    "SWP.jar",
)






//Files and folders (relative to the project root) included in the packaged zip.
//Directories keep their structure in the zip; files are placed at the zip root.
//Missing entries are silently skipped by Gradle.
val packageIncludes = listOf(
    "mod_info.json",
    "data",
    "graphics",
    "sounds",
    "src",
    "LICENSE"
)

//File extensions to include from the project root in the packaged zip.
//Each entry is matched as "*.<ext>" against files directly in the project root.
val packageIncludeExtensions = listOf(
    "version",
    "md",
    "txt"
)

//Additional jars to include, like libraries you ship with your mod.
//Paths are relative to this projects root directory.
val otherDependencies = listOf<String>(
    // "jars/dependency.jar",
)

//Folder (relative to this project root) that is also searched for modDependencies and otherDependencies.
//Drop jars here when you don't have the source mod installed under /mods/, or want to pin a specific version.
//For modDependencies, files are matched by filename (recursively).
//For otherDependencies, the entry's path is also tried relative to this folder.
val libsFolder = "libs"

//Resolution used when launching via "runStarsectorNoLauncher" (the run configuration that skips the launcher window).
val devResolution = "1920x1080"

//Java version to use. Should be 17, as it is what starsector itself uses.
val javaVersion = 17

//When set to true, .java and .kt source files will be bundled with your jar. This will provide people
//with real javadocs/comments when viewing things from your mod within their IDE. Does increase the jar size.
//You should set this to "true" if you expect other people to add your mod as a dependency
val isLibrary = true













/*
 * ------------------------------------------------------------
 *  Do not edit below this line.
 * ------------------------------------------------------------
 */

// Here is where your user configuration is fed into build.gradle.kts
gradle.extra["starsectorPath"] = starsectorPath
gradle.extra["jarName"] = jarName
gradle.extra["zipName"] = zipName
gradle.extra["modDependencies"] = modDependencies
gradle.extra["packageIncludes"] = packageIncludes
gradle.extra["packageIncludeExtensions"] = packageIncludeExtensions
gradle.extra["otherDependencies"] = otherDependencies
gradle.extra["libsFolder"] = libsFolder
gradle.extra["devResolution"] = devResolution
gradle.extra["javaVersion"] = javaVersion
gradle.extra["isLibrary"] = isLibrary










plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
