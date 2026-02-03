plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
    id("com.hypherionmc.modutils.modpublisher")
    `repo-convention`
}

val loader = prop("loom.platform")!!
val minecraft: String = stonecutter.current.version
val common: Project = requireNotNull(stonecutter.node.sibling("")?.project) {
    "No common project for $project"
}
val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.6" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-$loader")
}
architectury {
    platformSetupLoomIde()
    forge()
}

val commonBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val shadowBundle: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations {
    compileClasspath.get().extendsFrom(commonBundle)
    runtimeClasspath.get().extendsFrom(commonBundle)
    get("developmentForge").extendsFrom(commonBundle)
}

repositories {
    maven("https://maven.minecraftforge.net")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.fabricmc:yarn:$minecraft+build.${common.mod.dep("yarn_build")}:v2")
    "forge"("net.minecraftforge:forge:$minecraft-${common.mod.dep("forge_loader")}")

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionForge")) { isTransitive = false }
}

loom {
    forge.convertAccessWideners = true
    forge.mixinConfigs(
        "template-common.mixins.json",
        "template-forge.mixins.json",
    )

    runConfigs.all {
        isIdeConfigGenerated = true
        runDir = "../../../run"
        vmArgs("-Dmixin.debug.export=true")
    }

    decompilers {
        get("vineflower").apply { // Adds names to lambdas - useful for mixins
            options.put("mark-corresponding-synthetics", "1")
        }
    }
}

java {
    withSourcesJar()

    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks.compileJava {
    options.encoding = "UTF-8"

    // very few developers will provide source jar when publishing mods, we add param names in production jar
    // to make life easier for those who need to work with the mod
    options.compilerArgs.add("-parameters")
}

tasks.jar {
    archiveClassifier = "dev"
}

tasks.remapJar {
    injectAccessWidener = true
    inputFile = tasks.shadowJar.get().archiveFile
    archiveClassifier = null
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
    exclude("fabric.mod.json", "architectury.common.json")
}

tasks.processResources {
    properties(listOf("META-INF/mods.toml", "pack.mcmeta"),
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "minecraft" to common.mod.requireProp("mod.mc_dep_forgelike")
    )
}

tasks.register<Copy>("buildAndCollect") {
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}/$loader"))
    dependsOn(tasks.build)
}

// see: https://github.com/firstdarkdev/modpublisher
publisher {
    fun validatedProp(prop: String, env: String): Pair<String, String>? {
        val projectID = prop(prop)
        val apiKey: String? = System.getenv(env)
        if (projectID != null && !projectID.startsWith('[') && !apiKey.isNullOrEmpty()) {
            return projectID to apiKey
        }
        return null
    }

    validatedProp("publish.modrinth", "MODRINTH_TOKEN")?.let { (id, key) ->
        modrinthID.set(id)
        apiKeys { modrinth(key) }
    }

    validatedProp("publish.curseforge", "CURSE_TOKEN")?.let { (id, key) ->
        curseID.set(id)
        apiKeys { curseforge(key) }
    }

    validatedProp("publish.github", "GITHUB_TOKEN")?.let { (id, key) ->
        githubRepo.set(id)
        apiKeys { github(key) }
    }

    // Enable Debug mode. When enabled, no files will actually be uploaded
    debug.set(false)

    changelog.set(rootProject.file("CHANGELOG.md"))
    projectVersion.set(mod.version)
    // Example: Display Name 1.20.1-forge
    displayName.set("${mod.name} ${minecraft}-${loader}")
    gameVersions.set(listOf(minecraft))
    loaders.set(listOf(loader))
    artifact.set(tasks.remapJar)
    setJavaVersions(requiredJava)
}