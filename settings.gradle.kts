val minecraft = "1.20.1"
extra["minecraft"] = minecraft

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/common.gradle.kts")

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/kotlin.gradle.kts")

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/forge.gradle.kts")

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/parchmentmc.gradle.kts")

apply("https://github.com/SettingDust/MinecraftGradleScripts/raw/main/mixin.gradle.kts")

dependencyResolutionManagement.versionCatalogs.named("catalog") {
    library("lightmans-currency", "maven.modrinth", "lightmans-currency").version("$minecraft-2.2.2.2")
    library("ftb-chunks", "dev.ftb.mods", "ftb-chunks-forge").version("2001.3.1")
    library("architectury", "dev.architectury", "architectury-forge").version("9.2.14")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

val name: String by settings

rootProject.name = name
