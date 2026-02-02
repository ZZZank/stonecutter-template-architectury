plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
    id("com.gradleup.shadow")
    `repo-convention`
}

val loader = prop("loom.platform")!!
val minecraft: String = stonecutter.current.version
val common: Project = requireNotNull(stonecutter.node.sibling("")?.project) {
    "No common project for $project"
}

version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-$loader")
}
architectury {
    platformSetupLoomIde()
    fabric()
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
    get("developmentFabric").extendsFrom(commonBundle)
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.fabricmc:yarn:$minecraft+build.${common.mod.dep("yarn_build")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${mod.dep("fabric_loader")}")

    commonBundle(project(common.path, "namedElements")) { isTransitive = false }
    shadowBundle(project(common.path, "transformProductionFabric")) { isTransitive = false }
}

loom {
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

    val requiredJava = when {
        sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
        sc.current.parsed >= "1.20.6" -> JavaVersion.VERSION_21
        sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
        sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
        else -> JavaVersion.VERSION_1_8
    }

    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
}

tasks.compileJava {
    options.encoding = "UTF-8"

    // very few developers will provide source jar when publishing mods, we add param names in production jar
    // to make life easier for those who need to work with the mod
    options.compilerArgs.add("-parameters")
}

tasks.shadowJar {
    configurations = listOf(shadowBundle)
    archiveClassifier = "dev-shadow"
}

tasks.remapJar {
    injectAccessWidener = true
    inputFile = tasks.shadowJar.get().archiveFile
    archiveClassifier = null
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    archiveClassifier = "dev"
}

tasks.processResources {
    properties(listOf("fabric.mod.json"),
        "id" to mod.id,
        "name" to mod.name,
        "version" to mod.version,
        "minecraft" to common.mod.requireProp("mod.mc_dep_fabric")
    )
}

tasks.register<Copy>("buildAndCollect") {
    from(tasks.remapJar.get().archiveFile)
    into(rootProject.layout.buildDirectory.file("libs/${mod.version}/$loader"))
    doLast {
        val file = rootProject.layout
            .buildDirectory
            .file("libs/publish/$minecraft-$loader.txt")
            .get()
            .asFile
        file.parentFile.mkdirs()
        file.writeText(
            mapOf(
                "id" to mod.id,
                "version" to mod.version,
                "name" to mod.name,
                "group" to mod.group,
                "platform" to loader,
                "mc_version" to minecraft,
                "file_name" to tasks.remapJar.get().archiveFileName.get(),
            ).asSequence().joinToString("\n")
        )
    }
    dependsOn(tasks.build)
}
