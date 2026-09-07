import java.util.zip.ZipFile
import org.gradle.process.ExecOperations
import javax.inject.Inject
import java.io.ByteArrayOutputStream


/*
 * ------------------------------------------------------------
 *  USER CONFIGURATION lives in settings.gradle.kts.
 *  Edit the values there, not here. settings.gradle.kts publishes them onto `gradle.extra`, and we just read them back out below
 *  so the rest of this file (the build pipeline) doesn't need to change or know where the config actually lives.
 * ------------------------------------------------------------
 */
val starsectorPath = gradle.extra["starsectorPath"] as String
val jarName = gradle.extra["jarName"] as String
val zipName = gradle.extra["zipName"] as String
@Suppress("UNCHECKED_CAST")
val modDependencies = gradle.extra["modDependencies"] as List<String>
@Suppress("UNCHECKED_CAST")
val packageIncludes = gradle.extra["packageIncludes"] as List<String>
@Suppress("UNCHECKED_CAST")
val packageIncludeExtensions = gradle.extra["packageIncludeExtensions"] as List<String>
@Suppress("UNCHECKED_CAST")
val otherDependencies = gradle.extra["otherDependencies"] as List<String>
val libsFolder = gradle.extra["libsFolder"] as String
val devResolution = gradle.extra["devResolution"] as String
val javaVersion = gradle.extra["javaVersion"] as Int
val isLibrary = gradle.extra["isLibrary"] as Boolean

val useCommunityApiDocs: Boolean = gradle.extra["useCommunityApiDocs"] as Boolean
val communityApiDocsPath: File = (gradle.extra["communityApiDocsPath"] as? String)?.let { file(it) } ?:
    layout.buildDirectory.dir("communityApiDocs").get().asFile
val communityApiDocsRepoUrl: String = gradle.extra["communityApiDocsRepoUrl"] as String
val communityApiDocsAutoUpdate: Boolean = gradle.extra["communityApiDocsAutoUpdate"] as Boolean
//How often (in hours) to re-check for updates. Checks are throttled by a marker file's mtime, so
//most Gradle syncs don't pay for a network round-trip at all. Set to 0 to check every time.
val communityApiDocsUpdateIntervalHours: Long = 24L








/// BUILD PIPELINE
/// In Most cases, you should not need to change anything below here.




// Workaround for a Gradle issue: the Kotlin compiler can try to write its session-alive flag file before build/.kotlin/sessions/ exists.
// This creates the folder preemptively before compilation to avoid that issue.
layout.buildDirectory.dir(".kotlin/sessions").get().asFile.mkdirs()

//Local Maven repo where mod-dependency jars get staged, along with a matching "-sources.jar"
//(see stageModDependency / addModJars below). Declared up here (rather than next to docsRepoDir)
//because it needs to be initialized before the dependencies{} block runs, which happens earlier
//in this file.
val modDepsRepoDir = layout.buildDirectory.dir("modDepsRepo").get().asFile

dependencies {
    addModJars(modDependencies)
    otherDependencies.forEach { addCompileOnlyJar(it) }

    //Loads basic starsector dependencies.
    addStarsectorCoreDependencies()
}

fun DependencyHandler.addStarsectorCoreDependencies() {

    //Starsectors core jars live in different folders per OS, so look them up through the layout.
    val coreDir = starsectorLayout().gameWorkingDir

    //Starsector. The API jar comes through the local Maven repo (see repositories block) so IntelliJ can attach its source.
    //starfarer_obf is obfuscated with no source available, so it stays a plain file dependency.
    compileOnly("com.fs.starfarer:starfarer-api:local")

    //All other core jars in one files(...) call.
    compileOnly(files(
        File(coreDir, "starfarer_obf.jar"),
        File(coreDir, "commons-compiler.jar"),
        File(coreDir, "commons-compiler-jdk.jar"),
        File(coreDir, "fs.common_obf.jar"),
        File(coreDir, "fs.sound_obf.jar"),
        File(coreDir, "janino.jar"),
        File(coreDir, "jaxb-api-2.4.0-b180830.0359.jar"),
        File(coreDir, "jaxb-api-2.4.0-b180830.0359-sources.jar"),
        File(coreDir, "jinput.jar"),
        File(coreDir, "jogg-0.0.7.jar"),
        File(coreDir, "jorbis-0.0.15.jar"),
        File(coreDir, "json.jar"),
        File(coreDir, "log4j-1.2.9.jar"),
        File(coreDir, "lwjgl.jar"),
        File(coreDir, "lwjgl_util.jar"),
        File(coreDir, "txw2-3.0.2.jar"),
        File(coreDir, "webp-imageio-0.1.6.jar"),
        File(coreDir, "xstream-1.4.10.jar"),
    ))
}



plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)

    // Apply the java-library plugin for API and implementation separation.
    `java-library`

    // The built-in `idea` plugin lets us steer IntelliJ's module config from this script,
    // namely the compile-output dirs (see the `idea { ... }` block below).
    idea
}

// Move IntelliJ's compiled output from out/ to build/idea-out/ so we only have one top-level
// build folder.
idea {
    module {
        outputDir = file("build/idea-out/main")
        testOutputDir = file("build/idea-out/test")
    }
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()

    //Local Maven repo of staged Starsector API artifacts. The maven layout (vs flatDir) is what
    //actually lets IntelliJ pick up the "-sources.jar" sibling for autocomplete and navigation.
    maven { url = uri(stageStarsectorApi()) }

    //Local Maven repo of staged mod-dependency jars (see addModJars/stageModDependency). Same trick as
    //above: each mod jar is also staged under a matching "-sources.jar" name so IntelliJ attaches
    //docs/navigation for it. Starsector mod jars already bundle their .java/.kt source files
    //alongside the .class files, so the jar itself works fine as its own "sources" jar.
    maven { url = uri(modDepsRepoDir) }
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(javaVersion)
    }
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
        kotlin {
            setSrcDirs(listOf("src"))
        }
    }
}

//Build in parameter names, in case another mod needs to check out the code without having source access.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-parameters")
}
kotlin {
    compilerOptions {
        javaParameters = true
    }
}

tasks.test {
    enabled = false
}

tasks.jar {
    destinationDirectory.set(file("$rootDir/jars"))
    archiveFileName.set(jarName)

    if (isLibrary) {
        //Includes the .java and .kt sources for documentation detection
        from(sourceSets.main.get().allSource) {
            include("**/*.java", "**/*.kt")
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

fun DependencyHandler.addModJars(jarNames: List<String>) {
    if (jarNames.isEmpty()) return

    val modsDir = file("$starsectorPath/mods/")
    // Exclude this project's own folder. Otherwise, the configuration cache
    // treats the mods own /jars/ directory listing as a config-time input, and
    // every rebuild of the mod jar invalidates the cache.
    val thisProjectFolder = projectDir.name
    val modJarFiles = fileTree(modsDir) {
        jarNames.forEach { include("*/jars/**/$it") }
        exclude("$thisProjectFolder/**")
    }

    // Also look inside the local libs folder, if present. Matched by filename, recursively.
    val libsDir = file(libsFolder)
    val libsJarFiles = if (libsDir.exists()) {
        fileTree(libsDir) {
            jarNames.forEach { include("**/$it") }
        }
    } else {
        files()
    }

    val allJarFiles = (modJarFiles + libsJarFiles).files

    // Realize the file tree once to detect missing entries.
    val foundNames = allJarFiles.map { it.name }.toSet()
    jarNames.filterNot { it in foundNames }.forEach { missing ->
        logger.error(
            "Mod dependency '$missing' was not found in any mod's " +
                    "/jars folder under ${modsDir.absolutePath} " +
                    "or in ${libsDir.absolutePath}."
        )
    }

    // A jar name could theoretically be found more than once (e.g. present in both the mods
    // folder and the libs folder) - keep only the first match per filename.
    allJarFiles.distinctBy { it.name }.forEach { jarFile ->
        val notation = stageModDependency(jarFile)
        compileOnly(notation)
        if (jarDeclaresAnnotationProcessor(jarFile)) {
            annotationProcessor(notation)
        }
    }
}

//Jars that ship an annotation processor declare it in META-INF/services.
//Such jars get registered on the annotation processor path too, so javac picks the processor
//up automatically. Kotlin sources would additionally need the kapt/ksp plugin, this only
//covers Java compilation.
fun jarDeclaresAnnotationProcessor(jarFile: File): Boolean {
    //Track the jar's mtime as a configuration-cache input, so a swapped/updated jar re-runs this check.
    providers.of(FileMtimeSource::class.java) { parameters.path.set(jarFile.absolutePath) }.get()
    return runCatching {
        ZipFile(jarFile).use { zip ->
            zip.getEntry("META-INF/services/javax.annotation.processing.Processor") != null
        }
    }.getOrDefault(false)
}

//Stages a mod-dependency jar as a local Maven artifact under modDepsRepoDir, so it can be added
//as "modjars:<jarBaseName>:local". This mirrors stageStarsectorApi() below: the maven layout +
//"-sources.jar" naming convention is what lets IntelliJ automatically attach sources/docs for a
//compileOnly dependency.
//Starsector mod jars typically bundle their .java/.kt source files alongside the .class files
//in the same jar.
fun stageModDependency(jarFile: File): String {
    val jarBaseName = jarFile.nameWithoutExtension
    val artifactDir = File(modDepsRepoDir, "modjars/$jarBaseName/local")
    val dstJar = File(artifactDir, "$jarBaseName-local.jar")
    val dstSources = File(artifactDir, "$jarBaseName-local-sources.jar")
    val pomFile = File(artifactDir, "$jarBaseName-local.pom")

    artifactDir.mkdirs()

    if (!dstJar.exists() || dstJar.lastModified() < jarFile.lastModified()) {
        jarFile.copyTo(dstJar, overwrite = true)
    }

    if (!dstSources.exists() || dstSources.lastModified() < jarFile.lastModified()) {
        extractSourceEntriesOnly(jarFile, dstSources)
    }

    if (!pomFile.exists()) {
        pomFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>modjars</groupId>
                <artifactId>$jarBaseName</artifactId>
                <version>local</version>
            </project>
            """.trimIndent()
        )
    }

    return "modjars:$jarBaseName:local"
}

//Builds a "real" sources jar containing only .kt/.java/.kts entries copied out of the mod jar,
//discarding the .class entries. A straight copy of the whole jar technically also satisfies the
//"-sources.jar" naming convention and works fine for Java classes (IntelliJ's Java decompiler
//navigation matches by filename regardless of what else is in the jar), but the Kotlin plugin's
//library-source resolution appears to fall back to the compiled stub when it finds .class files
//sitting in what's supposed to be a pure source root. Filtering them out fixes that.
fun extractSourceEntriesOnly(srcJar: File, dstJar: File) {
    val extractDir = File(dstJar.parentFile, "${dstJar.nameWithoutExtension}-tmp")
    extractDir.deleteRecursively()

    project.copy {
        from(zipTree(srcJar))
        into(extractDir)
        include("**/*.kt", "**/*.java", "**/*.kts")
    }

    dstJar.delete()
    ant.withGroovyBuilder {
        "zip"(
        "destfile" to dstJar.absolutePath,
        "basedir" to extractDir.absolutePath
        )
    }

    extractDir.deleteRecursively()
}

fun DependencyHandler.addCompileOnlyJar(path: String) {
    val jarFile = file(path)
    if (jarFile.exists()) {
        compileOnly(files(jarFile))
        if (jarDeclaresAnnotationProcessor(jarFile)) annotationProcessor(files(jarFile))
        return
    }
    // Fallback: try resolving the same path relative to the libs folder.
    val libsFile = file("$libsFolder/$path")
    if (libsFile.exists()) {
        compileOnly(files(libsFile))
        if (jarDeclaresAnnotationProcessor(libsFile)) annotationProcessor(files(libsFile))
        return
    }
    logger.error(
        "Dependency '$path' was not found at ${jarFile.absolutePath} " +
                "or at ${libsFile.absolutePath}."
    )
}

enum class StarsectorPlatform { WINDOWS, LINUX, MAC }

// Functions rather than vals so they can be called from the `dependencies {}`
// block at the top of the script, which runs before any val declared below it
// would be initialized.
fun currentPlatform(): StarsectorPlatform = System.getProperty("os.name").lowercase().let { os ->
    when {
        "win" in os -> StarsectorPlatform.WINDOWS
        "mac" in os || "darwin" in os -> StarsectorPlatform.MAC
        else -> StarsectorPlatform.LINUX
    }
}

//Holds the per-OS paths Starsector needs: the launcher file, the bundled java executable, and the games working dir.
data class StarsectorLayout(
    val launcherFile: File,
    val javaExecutable: File,
    val gameWorkingDir: File,
)

//Resolves all three paths for the current OS. Starsector ships a different folder structure on each platform.
fun starsectorLayout(): StarsectorLayout = file(starsectorPath).let { root ->
    when (currentPlatform()) {
        StarsectorPlatform.WINDOWS -> StarsectorLayout(
            launcherFile = File(root, "vmparams"),
            javaExecutable = File(root, "jre/bin/java.exe"),
            gameWorkingDir = File(root, "starsector-core"),
        )
        StarsectorPlatform.LINUX -> StarsectorLayout(
            launcherFile = File(root, "starsector.sh"),
            javaExecutable = File(root, "jre_linux/bin/java"),
            gameWorkingDir = root,
        )
        StarsectorPlatform.MAC -> StarsectorLayout(
            launcherFile = File(root, "Contents/MacOS/starsector_mac.sh"),
            javaExecutable = File(root, "Contents/Home/bin/java"),
            gameWorkingDir = File(root, "Contents/Resources/Java"),
        )
    }
}

//Reads a file's modification time in a way the configuration cache will track as an input.
//Plain File.lastModified() calls at config time are NOT tracked by Gradle, so without this
//a Starsector update would not invalidate the cache and we'd keep serving the old staged
//API jar. Routing through a ValueSource is the documented escape hatch for "track external
//file state at configuration time".
abstract class FileMtimeSource : ValueSource<Long, FileMtimeSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val path: Property<String>
    }
    override fun obtain(): Long = File(parameters.path.get()).let {
        if (it.exists()) it.lastModified() else -1L
    }
}

//Small helper result type for a single git invocation, used internally by CommunityApiDocsSyncSource below.
data class GitCommandResult(val exitCode: Int, val output: String) {
    val ok: Boolean get() = exitCode == 0
}

//Everything involved in keeping CommunityApiDocs in sync lives inside this one ValueSource's
//obtain(): the interval throttle, the initial clone, and the incremental fetch+reset.
//
//Returns the newest mtime across every file under the checked-out src/ folder, or -1 if there's no
//usable checkout. That value is also what Gradle compares between builds: it only changes when
//content actually changed (a fresh clone or a fetch that moved the tip), which is exactly when the
//rest of the build needs to know to rebuild the sources jar - and it staying the same is what lets
//Gradle skip everything else on a cache hit once the interval hasn't elapsed.
abstract class CommunityApiDocsSyncSource : ValueSource<Long, CommunityApiDocsSyncSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val repoPath: Property<File>
        val repoUrl: Property<String>
        val autoUpdate: Property<Boolean>
        val intervalHours: Property<Long>
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    private val log = Logging.getLogger(CommunityApiDocsSyncSource::class.java)

    override fun obtain(): Long {
        val repoPath = parameters.repoPath.get()
        val srcDir = File(repoPath, "src")

        if (!parameters.autoUpdate.get()) {
            return newestMtime(srcDir)
        }

        val gitDir = File(repoPath, ".git")
        val markerFile = File(repoPath.parentFile, "${repoPath.name}.last-checked")
        val intervalMs = parameters.intervalHours.get() * 3_600_000L
        val needsCheck = !srcDir.exists() ||
                !markerFile.exists() ||
                System.currentTimeMillis() - markerFile.lastModified() >= intervalMs

        if (needsCheck) {
            if (!gitDir.exists()) {
                //First time only: a full (but still shallow, --depth 1) clone. Cloned into a temp
                //folder and swapped in atomically so a build interrupted mid-clone never leaves a
                //half-checked-out repo behind for the next run to trip over.
                log.lifecycle("Cloning CommunityApiDocs into $repoPath ...")
                repoPath.parentFile?.mkdirs()
                val freshDir = File(repoPath.parentFile, "${repoPath.name}.tmp")
                freshDir.deleteRecursively()

                val result = runGit(
                    repoPath.parentFile ?: File("."),
                    "clone", "--depth", "1", parameters.repoUrl.get(), freshDir.absolutePath,
                    timeoutSeconds = 120,
                )

                if (result.ok) {
                    repoPath.deleteRecursively()
                    if (!freshDir.renameTo(repoPath)) {
                        //Fall back to copy+delete in case the rename crosses a filesystem boundary.
                        freshDir.copyRecursively(repoPath, overwrite = true)
                        freshDir.deleteRecursively()
                    }
                    markerFile.parentFile?.mkdirs()
                    markerFile.writeText(System.currentTimeMillis().toString())
                } else {
                    freshDir.deleteRecursively()
                    log.warn(
                        result.output.substringAfter('\n') +
                        "\nCould not clone CommunityApiDocs (offline, or git isn't installed?). " +
                        "Falling back to vanilla Starsector API sources for hover docs."
                    )
                }
            } else {
                //Already cloned: fetch just the new tip commit - `--depth 1` clones set up a
                //single-branch tracking config, so this pulls down only what changed, not the whole
                //repo again - then force the working tree to match it. `origin/HEAD` is the symbolic
                //ref git points at the remote's default branch at clone time, so this tracks that
                //branch without hardcoding "main" vs "master". Using fetch+reset rather than `git
                //pull` sidesteps merge/ff-only failures entirely - there are never local commits
                //here worth preserving.
                log.lifecycle("Fetching CommunityApiDocs updates...")
                val fetched = runGit(repoPath, "fetch", "--depth", "1", "origin", timeoutSeconds = 60)
                if (fetched.ok && runGit(repoPath, "reset", "--hard", "origin/HEAD", timeoutSeconds = 30).ok) {
                    markerFile.parentFile?.mkdirs()
                    markerFile.writeText(System.currentTimeMillis().toString())
                } else {
                    log.warn("Could not check CommunityApiDocs for updates; using the existing local checkout.")
                }
            }
        }

        return newestMtime(srcDir)
    }

    private fun newestMtime(dir: File): Long {
        if (!dir.exists()) return -1L
        return dir.walkTopDown().filter { it.isFile }.maxOfOrNull { it.lastModified() } ?: -1L
    }

    //Runs `git <args>` in `dir` via ExecOperations - the pattern Gradle actually supports for
    //starting external processes from configuration-time code (a raw ProcessBuilder/Runtime.exec
    //call is what triggers "Starting an external process during configuration time is unsupported"
    //once the configuration cache is on). ExecOperations.exec() has no timeout parameter and blocks
    //until the process exits, so rather than trying to kill a hung process from the Java side, the
    //timeout is enforced by git itself: GIT_TERMINAL_PROMPT=0 stops it from ever blocking on an
    //interactive credential prompt, and http.lowSpeedLimit/http.lowSpeedTime tell git to abort on
    //its own if a transfer stalls.
    private fun runGit(dir: File, vararg args: String, timeoutSeconds: Long = 30): GitCommandResult {
        val output = ByteArrayOutputStream()
        val result = execOperations.exec {
            workingDir = dir
            commandLine(
                listOf(
                    "git",
                    "-c", "http.lowSpeedLimit=1000",
                    "-c", "http.lowSpeedTime=$timeoutSeconds",
                ) + args
            )
            environment("GIT_TERMINAL_PROMPT", "0")
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true
        }
        return GitCommandResult(result.exitValue, output.toString().trim())
    }
}

//Resolves (and, subject to the interval/throttle inside CommunityApiDocsSyncSource, updates)
//CommunityApiDocs, returning its .../src folder to use as sources-jar content, or null if there
//isn't one (auto-update disabled and nothing checked out yet, or the clone/fetch failed with
//nothing to fall back on).
fun resolveCommunityApiDocsSrc(): File? {
    val mtime = providers.of(CommunityApiDocsSyncSource::class.java) {
        parameters.repoPath.set(communityApiDocsPath)
        parameters.repoUrl.set(communityApiDocsRepoUrl)
        parameters.autoUpdate.set(communityApiDocsAutoUpdate)
        parameters.intervalHours.set(communityApiDocsUpdateIntervalHours)
    }.get()
    if (mtime < 0) return null
    return File(communityApiDocsPath, "src").takeIf { it.exists() }
}

//Stages the Starsector API as a local Maven repo under build/starsector-api/.
//Using a maven layout (not flatDir) because IntelliJ only reliably attaches sources when the
//artifact has a POM and follows the standard "<name>-<version>-sources.jar" classifier convention.
//A jar IS a zip with optional manifest, so the source side is just a copy (or a re-zip) with the
//right filename. If you ever hit a zip layout IntelliJ does not like, swap the copy for a real
//extract + repack.
//
//The "-sources.jar" content itself comes from one of two places:
//  - CommunityApiDocs (see CommunityApiDocsSyncSource/resolveCommunityApiDocsSrc() above),
//    auto-cloned/updated under communityApiDocsPath. If present, that folder is zipped up and used
//    as the sources jar instead of Starsector's own, mostly undocumented starfarer.api.zip.
//    IntelliJ's Quick Documentation / hover popup then shows the community-written Javadoc instead.
//  - Otherwise (auto-update disabled and nothing checked out, or the clone failed with no
//    prior copy to fall back on) it falls back to starfarer.api.zip, same as the original setup.
//Runs at configuration time so the files exist before Gradle resolves dependencies (including IDE sync).
fun stageStarsectorApi(): File {
    val repoDir = layout.buildDirectory.dir("starsector-api").get().asFile
    val artifactDir = File(repoDir, "com/fs/starfarer/starfarer-api/local")
    val coreDir = starsectorLayout().gameWorkingDir

    val srcJar = File(coreDir, "starfarer.api.jar")
    val srcZip = File(coreDir, "starfarer.api.zip")
    //This is what actually clones/updates CommunityApiDocs (subject to the throttle/interval),
    //so it needs to run before the freshness checks below, not just resolve a path.
    val communityDocsSrc: File? = if (useCommunityApiDocs) resolveCommunityApiDocsSrc() else null
    val dstJar = File(artifactDir, "starfarer-api-local.jar")
    val dstSources = File(artifactDir, "starfarer-api-local-sources.jar")
    val pomFile = File(artifactDir, "starfarer-api-local.pom")

    //Force the configuration cache to depend on the source file mtimes. The `.get()` calls
    //pull the values, which makes them part of the cache fingerprint. When Starsector is
    //updated and these mtimes change, the cache invalidates and the staging logic re-runs.
    providers.of(FileMtimeSource::class.java) { parameters.path.set(srcJar.absolutePath) }.get()
    providers.of(FileMtimeSource::class.java) { parameters.path.set(srcZip.absolutePath) }.get()

    require(srcJar.exists()) {
        "Starsector API jar not found at ${srcJar.absolutePath}. " +
                "Check starsectorPath at the top of this build script."
    }

    val useCommunityDocs = communityDocsSrc != null

    //Newest mtime across every file under CommunityApiDocs/src, so a freshly re-cloned copy
    //(different content, same folder) is always detected as newer than a stale sources jar.
    val communityDocsMtime = if (useCommunityDocs) {
        communityDocsSrc.walkTopDown().filter { it.isFile }.maxOfOrNull { it.lastModified() } ?: 0L
    } else 0L

    //Fast path: staged files match (or post-date) their sources, so we can return without doing anything.
    val jarFresh = dstJar.exists() && dstJar.lastModified() >= srcJar.lastModified()
    val sourcesFresh = if (useCommunityDocs) {
        dstSources.exists() && dstSources.lastModified() >= communityDocsMtime
    } else {
        !srcZip.exists() || (dstSources.exists() && dstSources.lastModified() >= srcZip.lastModified())
    }
    if (jarFresh && sourcesFresh && pomFile.exists()) return repoDir

    artifactDir.mkdirs()

    //Only copy if the source is newer than the staged file, so repeat syncs are cheap.
    fun stageIfStale(src: File, dst: File) {
        if (!src.exists()) return
        if (!dst.exists() || dst.lastModified() < src.lastModified()) {
            src.copyTo(dst, overwrite = true)
        }
    }

    stageIfStale(srcJar, dstJar)

    if (useCommunityDocs) {
        if (!sourcesFresh) {
            dstSources.delete()
            ant.withGroovyBuilder {
                "zip"(
                    "destfile" to dstSources.absolutePath,
                    "basedir" to communityDocsSrc.absolutePath
                )
            }
        }
    } else {
        stageIfStale(srcZip, dstSources)
    }

    //Minimal POM. Gradle's maven resolver needs one to recognise the artifact and to look up the -sources classifier.
    if (!pomFile.exists()) {
        pomFile.writeText(
            """
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>com.fs.starfarer</groupId>
                <artifactId>starfarer-api</artifactId>
                <version>local</version>
            </project>
            """.trimIndent()
        )
    }
    return repoDir
}

data class StarsectorLaunchSpec(
    val jvmArgs: List<String>,
    val classpath: List<File>,
    val mainClass: String,
)

//Intermediate value returned by each per-platform parser. Classpath entries are still
//strings here. parseLauncher() resolves them to absolute Files against the working dir.
data class RawLaunchSpec(
    val jvmArgs: List<String>,
    val classpath: List<String>,
    val mainClass: String,
)

//Whitespace-aware tokenizer that keeps quoted content as a single token. Single or double
//quotes group their content; the quote chars themselves are consumed. Needed so launcher
//flags like -Dfoo="bar baz" survive instead of becoming two tokens.
fun shellTokenize(s: String): List<String> {
    val out = mutableListOf<String>()
    val cur = StringBuilder()
    var quote: Char? = null
    for (c in s) when {
        quote != null -> if (c == quote) quote = null else cur.append(c)
        c == '"' || c == '\'' -> quote = c
        c.isWhitespace() -> if (cur.isNotEmpty()) { out += cur.toString(); cur.clear() }
        else -> cur.append(c)
    }
    if (cur.isNotEmpty()) out += cur.toString()
    return out
}

//vmparams is a single line listing every flag, separated by whitespace.
fun parseWindowsLauncher(file: File): RawLaunchSpec {
    val tokens = shellTokenize(file.readText().trim())
    return sliceJavaCommand(tokens, classpathSeparator = ';', sourceForError = file)
}

//starsector.sh is a multi-line shell script with one flag per line, joined by `\`-continuations.
//Drop comment lines, then collapse each `\<newline>` into a space so the whole java invocation
//lands on one logical line before tokenizing.
fun parseLinuxLauncher(file: File): RawLaunchSpec {
    val joined = file.readLines()
        .filterNot { it.trim().startsWith("#") }
        .joinToString("\n")
        .replace(Regex("""\\\r?\n"""), " ")
    val tokens = shellTokenize(joined)
    return sliceJavaCommand(tokens, classpathSeparator = ':', sourceForError = file)
}

//Same as Linux but additionally drops `${VAR}` placeholders. The mac script has `${EXTRAARGS}`
//as an injection point that would normally be expanded by the shell; we have nothing to expand
//it to, so we skip the token.
fun parseMacLauncher(file: File): RawLaunchSpec {
    val joined = file.readLines()
        .filterNot { it.trim().startsWith("#") }
        .joinToString("\n")
        .replace(Regex("""\\\r?\n"""), " ")
    val tokens = shellTokenize(joined).filterNot { it.startsWith("\${") }
    return sliceJavaCommand(tokens, classpathSeparator = ':', sourceForError = file)
}

//Pulls jvmArgs, classpath entries, and main class out of the tokenized java invocation.
//Expected layout: [java] [jvmArgs...] [-classpath|-cp] [cp string] [mainClass] [args...]
fun sliceJavaCommand(
    tokens: List<String>,
    classpathSeparator: Char,
    sourceForError: File,
): RawLaunchSpec {
    //Match the executable by basename. Case-sensitive on purpose: the Mac script does
    //`cd ../Resources/Java`, and lowercase `java` must not match the uppercase `Java` dir.
    val javaIdx = tokens.indexOfFirst { token ->
        val basename = token.substringAfterLast('/').substringAfterLast('\\')
        basename == "java" || basename == "java.exe"
    }
    require(javaIdx >= 0) { "Could not locate the java invocation in $sourceForError" }

    //First -classpath/-cp after the java token. If Starsector ever switches to --module-path,
    //this is where it would break; extend the parser then.
    val cpIdx = (javaIdx + 1 until tokens.size).firstOrNull { i ->
        tokens[i] == "-classpath" || tokens[i] == "-cp"
    } ?: error("Could not locate -classpath/-cp in $sourceForError")
    require(cpIdx + 2 < tokens.size) {
        "Missing classpath value or main class in $sourceForError"
    }

    val classpath = tokens[cpIdx + 1].split(classpathSeparator)
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    return RawLaunchSpec(
        //Everything between `java` and `-classpath` is treated as a jvm arg.
        jvmArgs = tokens.subList(javaIdx + 1, cpIdx),
        classpath = classpath,
        mainClass = tokens[cpIdx + 2],
    )
}

//Reads the launcher for the current OS and returns a fully resolved launch spec.
//Relative classpath entries get resolved against the games working directory.
fun parseLauncher(): StarsectorLaunchSpec {
    val layout = starsectorLayout()
    val launcherFile = layout.launcherFile
    require(launcherFile.exists()) {
        "Starsector launcher file not found at ${launcherFile.absolutePath} " +
                "(expected for platform=${currentPlatform()})"
    }

    val raw = when (currentPlatform()) {
        StarsectorPlatform.WINDOWS -> parseWindowsLauncher(launcherFile)
        StarsectorPlatform.LINUX -> parseLinuxLauncher(launcherFile)
        StarsectorPlatform.MAC -> parseMacLauncher(launcherFile)
    }

    val workingDirPath = layout.gameWorkingDir.toPath()
    val classpath = raw.classpath.map { workingDirPath.resolve(it).normalize().toFile() }
    return StarsectorLaunchSpec(raw.jvmArgs, classpath, raw.mainClass)
}

val launcherInfo by lazy { starsectorLayout() to parseLauncher() }
fun List<String>.filteredArgs(): List<String> = filterNot { it.contains("PrintCodeCache") }

//Builds the mod jar, then runs Starsector using the same classpath/jvmArgs the launcher would use.
tasks.register<JavaExec>("runStarsector") {
    group = "starsector"
    description = "Build the mod and launch Starsector (with launcher)."
    dependsOn(tasks.jar)

    val (layout, parsed) = launcherInfo
    setExecutable(layout.javaExecutable.absolutePath)
    workingDir = layout.gameWorkingDir
    mainClass.set(parsed.mainClass)
    classpath = files(parsed.classpath)
    //Stops treating game-crashes as build errors
    isIgnoreExitValue = true
    jvmArgs = parsed.jvmArgs.filteredArgs()
}

//Same as above, but skips the launcher window and jumps straight in to the game.
//The extra -D flags are the same ones the launcher passes when you hit play, so the game gets the settings it expects.
tasks.register<JavaExec>("runStarsectorNoLauncher") {
    group = "starsector"
    description = "Build the mod and launch Starsector, skipping the launcher."
    dependsOn(tasks.jar)

    val (layout, parsed) = launcherInfo
    setExecutable(layout.javaExecutable.absolutePath)
    workingDir = layout.gameWorkingDir
    mainClass.set(parsed.mainClass)
    classpath = files(parsed.classpath)
    isIgnoreExitValue = true
    jvmArgs = listOf(
        "-DstartRes=$devResolution",
        "-DlaunchDirect=true",
        "-DstartFS=false",
        "-DstartSound=true",
    ) + parsed.jvmArgs.filteredArgs()
}

//Ensure IntelliJ's "Build and run using" stays on IDEA (not Gradle) so HotSwap can recompile
//changed classes in milliseconds via IntelliJ's incremental compiler instead of shelling out to
//Gradle on every reload. The .idea/ folder is gitignored (IDE config is user-specific), so we
//re-apply this on every Gradle sync. Never creates gradle.xml: if it's missing, IntelliJ is in
//the middle of a first-time import and writing the file ourselves can break its sync detection.
//Wrapped in runCatching: any failure here is non-fatal, sothe build/sync continues.
runCatching {
    val gradleXml = file(".idea/gradle.xml")
    if (gradleXml.exists()) {
        val text = gradleXml.readText()
        val canonical = """<option name="delegatedBuild" value="false" />"""
        val existingLine = Regex("""<option name="delegatedBuild" value="(?:true|false)"\s*/>""")
        val updated = if (existingLine.containsMatchIn(text)) {
            text.replace(existingLine, canonical)
        } else {
            //Insert as first child of the GradleProjectSettings block if present.
            Regex("""<GradleProjectSettings[^>]*>""").find(text)?.let { match ->
                text.replaceRange(match.range.last + 1, match.range.last + 1, "\n        $canonical")
            } ?: text
        }
        if (updated != text) gradleXml.writeText(updated)
    }
}.onFailure { e ->
    logger.warn("Could not enforce delegatedBuild=false in .idea/gradle.xml (non-fatal): ${e.message}")
}


tasks.register<Zip>("packageMod") {
    group = "distribution"
    description = "Packages the mod into a ZIP file for release."

    // The name of the resulting zip file
    archiveFileName.set(zipName)
    // Where to put the zip
    destinationDirectory.set(layout.projectDirectory)

    // Wrap everything inside a top-level folder named after this project's root directory,
    // so the zip extracts to a single "<ProjectName>/" folder ready to drop into /mods/.
    // Every from() below inherits this prefix.
    into(projectDir.name)

    // 1. Include the compiled jar from the build task
    from(tasks.jar) {
        into("jars") // Optional: place inside a jar folder in the zip
    }

    // 2. Include the files and folders listed in packageIncludes.
    // Directories are placed into a same-named folder; files go at the folder root.
    packageIncludes.forEach { name ->
        val source = file(name)
        if (source.isDirectory) {
            from(source) { into(name) }
        } else {
            from(source)
        }
    }

    // 3. Include any project-root files matching packageIncludeExtensions.
    from(projectDir) {
        packageIncludeExtensions.forEach { ext -> include("*.$ext") }
    }
}


