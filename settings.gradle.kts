val minecraft = "1.20.1"
extra["minecraft"] = minecraft

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/common.gradle.kts")

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/kotlin.gradle.kts")

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/forge.gradle.kts")

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/parchmentmc.gradle.kts")

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/mixin.gradle.kts")

dependencyResolutionManagement.versionCatalogs.named("catalog") {
    library("lightmans-currency", "maven.modrinth", "lightmans-currency").version("$minecraft-2.2.2.2")

    library("ftb-library", "dev.ftb.mods", "ftb-library-forge").version("2001.2.3")
    library("ftb-teams", "dev.ftb.mods", "ftb-teams-forge").version("2001.3.0")
    library("ftb-chunks", "dev.ftb.mods", "ftb-chunks-forge").version("2001.3.1")

    library("architectury", "dev.architectury", "architectury-forge").version("9.2.14")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

val name: String by settings

rootProject.name = name
