import org.apache.commons.lang3.SystemUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.IOException
import java.util.Properties
plugins {
    idea
    java
    id("gg.essential.loom") version "0.10.0.+"
    id("dev.architectury.architectury-pack200") version "0.1.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
val baseGroup: String by project
val mcVersion: String by project
val mixinGroup = "$baseGroup.mixin"

val requestedTasks: String = gradle.startParameter.taskNames.joinToString(" ")
val betaBuild: Boolean = requestedTasks.contains("buildBeta", ignoreCase = true)
val releaseBuild: Boolean =
    betaBuild || requestedTasks.contains("buildLatest", ignoreCase = true)
val devBuild: Boolean = requestedTasks.contains("buildDev", ignoreCase = true)
        || requestedTasks.contains("Devbuild", ignoreCase = true)

// Per-day build counters, one per channel (e.g., 261007+5 = 5th build of Oct 7, 2026).
// Each channel resets to 1 when the day changes. Release builds consume the next
// number; plain builds (buildDll) reuse the last number built today.
val buildCountFile = file("build-count.properties")
val today: String = LocalDate.now().toString()
val countProps = Properties()
if (buildCountFile.exists()) {
    buildCountFile.inputStream().use { countProps.load(it) }
}

fun storedCount(dateKey: String, countKey: String): Int =
    if (countProps.getProperty(dateKey, "") == today) {
        countProps.getProperty(countKey, "0").toIntOrNull() ?: 0
    } else {
        0
    }

fun consumeCount(dateKey: String, countKey: String): Int {
    val next = storedCount(dateKey, countKey) + 1
    countProps.setProperty(dateKey, today)
    countProps.setProperty(countKey, next.toString())
    buildCountFile.outputStream().use { countProps.store(it, null) }
    return next
}

val releaseDateKey = "lastReleaseDate"
val releaseCountKey = "releaseBuildCount"
val betaDateKey = "lastBetaDate"
val betaCountKey = "betaBuildCount"
val devDateKey = "lastDevDate"
val devCountKey = "devBuildCount"

val buildCount: Int = when {
    devBuild -> consumeCount(devDateKey, devCountKey)
    releaseBuild && betaBuild -> consumeCount(betaDateKey, betaCountKey)
    releaseBuild -> consumeCount(releaseDateKey, releaseCountKey)
    else -> storedCount(releaseDateKey, releaseCountKey)
}

val buildDate: String = LocalDate.now().format(DateTimeFormatter.ofPattern("yyMMdd"))
version = when {
    betaBuild -> "Beta-$buildDate+$buildCount"
    devBuild -> "DevBuild+$buildCount"
    else -> "$buildDate+$buildCount"
}

if (releaseBuild || devBuild) {
    println("myau build: $version")
}
val modid: String by project
val jarName: String by project
val transformerFile = file("src/main/resources/accesstransformer.cfg")
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}
loom {
    log4jConfigs.from(file("log4j2.xml"))
    launchConfigs {
        "client" {
            property("mixin.debug", "true")
            arg("--tweakClass", "org.spongepowered.asm.launch.MixinTweaker")
        }
    }
    runConfigs {
        "client" {
            if (SystemUtils.IS_OS_MAC_OSX) {
                vmArgs.remove("-XstartOnFirstThread")
            }
        }
        remove(getByName("server"))
    }
    forge {
        pack200Provider.set(dev.architectury.pack200.java.Pack200Adapter())
        mixinConfig("mixins.$modid.json")
	    if (transformerFile.exists()) {
			println("Installing access transformer")
		    accessTransformer(transformerFile)
	    }
    }
    mixin {
        defaultRefmapName.set("mixins.$modid.refmap.json")
    }
}
sourceSets.main {
    output.setResourcesDir(sourceSets.main.flatMap { it.java.classesDirectory })
}
repositories {
    mavenCentral()
    maven("https://repo.spongepowered.org/maven/")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
}
val shadowImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}
dependencies {
    minecraft("com.mojang:minecraft:1.8.9")
    mappings("de.oceanlabs.mcp:mcp_stable:22-1.8.9")
    forge("net.minecraftforge:forge:1.8.9-11.15.1.2318-1.8.9")
    shadowImpl("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    annotationProcessor("org.spongepowered:mixin:0.8.5-SNAPSHOT")
    shadowImpl("org.ow2.asm:asm:9.7")
    shadowImpl("org.ow2.asm:asm-tree:9.7")
    shadowImpl("org.ow2.asm:asm-commons:9.7")
    runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
}
tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
}
tasks.withType(org.gradle.jvm.tasks.Jar::class) {
    archiveBaseName.set(jarName)
    manifest.attributes.run {
        this["FMLCorePluginContainsFMLMod"] = "true"
        this["ForceLoadAsMod"] = "true"
        this["TweakClass"] = "org.spongepowered.asm.launch.MixinTweaker"
        this["Premain-Class"] = "myau.inject.Agent"
        this["Agent-Class"] = "myau.inject.Agent"
        this["Can-Retransform-Classes"] = "true"
        this["Can-Redefine-Classes"] = "true"
        this["Main-Class"] = "myau.inject.Injector"
        this["MixinConfigs"] = "mixins.$modid.json"
	    if (transformerFile.exists())
			this["FMLAT"] = "${modid}_at.cfg"
    }
}
tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("mcversion", mcVersion)
    inputs.property("modid", modid)
    inputs.property("basePackage", baseGroup)
    filesMatching(listOf("mcmod.info", "mixins.$modid.json","version.json")) {
        expand(inputs.properties)
    }
    rename("accesstransformer.cfg", "META-INF/${modid}_at.cfg")
}
val remapJar by tasks.named<net.fabricmc.loom.task.RemapJarTask>("remapJar") {
    archiveClassifier.set("")
    from(tasks.shadowJar)
    input.set(tasks.shadowJar.get().archiveFile)
}
tasks.jar {
    archiveClassifier.set("without-deps")
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    from(layout.buildDirectory.dir("classes/java/main"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.shadowJar {
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("non-obfuscated-with-deps")
    configurations = listOf(shadowImpl)
    doLast {
        configurations.forEach {
            println("Copying dependencies into mod: ${it.files}")
        }
    }
    relocate("org.objectweb.asm", "myau.shadow.asm")
}
tasks.assemble.get().dependsOn(tasks.remapJar)

val nativesDir = file("../myau-natives")
val nativesBuildDir = File(nativesDir, "build")
val nativeJavaHome: String = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")

fun cmakeConfigure(extra: List<String>) {
    providers.exec {
        commandLine(
            listOf("cmake", "-S", nativesDir.absolutePath, "-B", nativesBuildDir.absolutePath) + extra
        )
        environment("JAVA_HOME", nativeJavaHome)
    }.standardOutput.asText.get().let { println(it) }
}

fun cmakeBuild() {
    providers.exec {
        commandLine("cmake", "--build", nativesBuildDir.absolutePath, "--config", "Release")
        environment("JAVA_HOME", nativeJavaHome)
    }.standardOutput.asText.get().let { println(it) }
}

fun latestJar(): File {
    val expected = File(layout.buildDirectory.get().asFile, "libs/$jarName-$version.jar")
    if (!expected.isFile) {
        throw GradleException("client jar not found: ${expected.name}")
    }
    return expected
}

fun buildNative(outputName: String, destDir: File, devLoader: Boolean = false) {
    val jar = latestJar()
    println("client jar: ${jar.name}")
    cmakeConfigure(listOf("-DCLIENT_JAR=${jar.invariantSeparatorsPath}"))
    cmakeBuild()
    val built = File(nativesBuildDir, "Release/myau_native.dll")
    val target = File(nativesBuildDir, "Release/$outputName")
    if (built.absolutePath != target.absolutePath) {
        built.copyTo(target, overwrite = true)
        built.delete()
    }
    println("output: $target")
    // Publish into <Channel>/<YYMMDD>/<version>/ so each day gets its own
    // folder and every build its own child folder (e.g. Release/261007/261007+1).
    val datedDir = File(destDir, "$buildDate/$version")
    datedDir.mkdirs()
    jar.copyTo(File(datedDir, jar.name), overwrite = true)
    publishCopy(target, File(datedDir, target.name))
    if (devLoader) {
        // Dev builds ship only the dev loader -- the download/local loaders
        // can't load myau_native_dev.dll anyway.
        File(nativesBuildDir, "Release/myau_loader_dev.exe").takeIf { it.isFile }
            ?.let { publishCopy(it, File(datedDir, "myau_loader_dev.exe")) }
    } else {
        File(nativesBuildDir, "Release/myau_loader.exe").takeIf { it.isFile }
            ?.let { publishCopy(it, File(datedDir, "myau_loader.exe")) }
        File(nativesBuildDir, "Release/myau_loader_local.exe").takeIf { it.isFile }
            ?.let { publishCopy(it, File(datedDir, "myau_loader_local.exe")) }
    }
    println("published to: ${datedDir.absolutePath}")
}

val releaseDir = file("../Release")
val betaDir = file("../Beta")

fun publishCopy(source: File, dest: File) {
    var lastError: Throwable? = null
    for (attempt in 1..5) {
        try {
            if (dest.exists() && !dest.delete()) {
                throw IOException("destination is locked by another process")
            }
            source.copyTo(dest, overwrite = true)
            return
        } catch (e: Throwable) {
            lastError = e
            Thread.sleep(500L)
        }
    }
    throw GradleException(
        "Could not publish ${dest.name} (file is likely in use - "
            + "close the running loader/game and rebuild).",
        lastError
    )
}

tasks.register("buildDll") {
    group = "myau"
    description = "Builds myau_native.dll from the current jar without bumping the version."
    dependsOn(tasks.assemble)
    doLast { buildNative("myau_native.dll", releaseDir) }
}

tasks.register("buildLatest") {
    group = "myau"
    description = "Builds the Latest channel payload and bumps the version."
    dependsOn(tasks.assemble)
    doLast { buildNative("myau_native.dll", releaseDir) }
}

tasks.register("buildBeta") {
    group = "myau"
    description = "Builds the Beta channel payload and bumps the version."
    dependsOn(tasks.assemble)
    doLast { buildNative("myau_native_beta.dll", betaDir) }
}

val devDir = file("../Dev")

tasks.register("buildDev") {
    group = "myau"
    description = "Builds a Dev test payload (DevBuild+N) into Dev/<date>/ without touching beta/release counters."
    dependsOn(tasks.assemble)
    doLast { buildNative("myau_native_dev.dll", devDir, devLoader = true) }
}

tasks.register("Devbuild") {
    group = "myau"
    description = "Alias for buildDev."
    dependsOn("buildDev")
}
