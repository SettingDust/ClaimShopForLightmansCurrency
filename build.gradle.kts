import groovy.lang.Closure
import org.spongepowered.asm.gradle.plugins.struct.DynamicProperties
import java.text.SimpleDateFormat
import java.util.*

plugins {
    idea
    java
    `maven-publish`
    alias(catalog.plugins.idea.ext)

    alias(catalog.plugins.kotlin.jvm)
    alias(catalog.plugins.kotlin.plugin.serialization)

    alias(catalog.plugins.git.version)

    alias(catalog.plugins.forge.gradle)
    alias(catalog.plugins.librarian.forgegradle)
    alias(catalog.plugins.mixin)
}

apply(
    "https://github.com/SettingDust/MinecraftGradleScripts/raw/main/gradle_issue_15754.gradle.kts")

group = "settingdust.lightmanscurrency.claimshop"

val gitVersion: Closure<String> by extra

version = gitVersion()

val id: String by rootProject.properties
val name: String by rootProject.properties
val author: String by rootProject.properties
val description: String by rootProject.properties

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }

    // Still required by IDEs such as Eclipse and Visual Studio Code
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build"
    // task if it is present.
    // If you remove this line, sources will not be generated.
    withSourcesJar()

    // If this mod is going to be a library, then it should also generate Javadocs in order to
    // aid with development.
    // Uncomment this line to generate them.
    withJavadocJar()
}

kotlin { jvmToolchain(17) }

minecraft {
    mappings(
        "parchment", "2023.09.03-${catalog.versions.minecraft.get()}")

    runs.all {
        mods {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("forge.enabledGameTestNamespaces", id)
            property("terminal.jline", "true")
            mods { create(id) { source(sourceSets.main.get()) } }
        }
    }

    runs.run {
        create("client") {
            property("log4j.configurationFile", "log4j2.xml")
            jvmArg("-XX:+AllowEnhancedClassRedefinition")
            args("--username", "Player")
        }

        create("server") {}
        create("gameTestServer") {}
        create("data") {
            workingDirectory(project.file("run"))
            args(
                "--mod",
                id,
                "--all",
                "--output",
                file("src/generated/resources/"),
                "--existing",
                file("src/main/resources"))
        }
    }
}

mixin {
    add("main", "$id.refmap.json")

    config("claim_shop_for_lightmans_currency.mixins.json")

    dumpTargetOnFailure = true
    val debug = debug as DynamicProperties
    debug.propertyMissing("verbose", true)
    debug.propertyMissing("export", true)
}

sourceSets.main.configure { resources.srcDirs("src/generated/resources/") }

repositories {
    maven("https://maven.ftb.dev/releases") { content { includeGroup("dev.ftb.mods") } }
    maven("https://maven.architectury.dev/") { content { includeGroup("dev.architectury") } }
}

dependencies {
    minecraft(catalog.minecraft.forge)
    annotationProcessor(variantOf(catalog.mixin) { classifier("processor") })

    implementation(catalog.kotlin.forge)

    implementation(fg.deobf(catalog.lightmans.currency.get()))

    implementation(fg.deobf(catalog.ftb.chunks.get()))
    implementation(fg.deobf(catalog.ftb.library.get()))
    implementation(fg.deobf(catalog.ftb.teams.get()))
    implementation(fg.deobf(catalog.architectury.get()))
}

val metadata =
    mapOf(
        "group" to group,
        "author" to author,
        "id" to id,
        "name" to name,
        "version" to version,
        "description" to description,
        "source" to "https://github.com/SettingDust/ClaimShopForLightmansCurrency",
    )

tasks {
    withType<ProcessResources> {
        inputs.properties(metadata)
        filesMatching(listOf("META-INF/mods.toml", "*.mixins.json", "pack.mcmeta")) {
            expand(metadata)
        }
    }

    withType<Jar> {
        archiveBaseName.set(id)
        manifest {
            attributes(
                mapOf(
                    "Specification-Title" to id,
                    "Specification-Vendor" to author,
                    "Specification-Version" to "1",
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version.toString(),
                    "Implementation-Vendor" to author,
                    "Implementation-Timestamp" to
                        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(Date())))
        }
        finalizedBy("reobfJar")
    }
}
