plugins {
    id("dev.architectury.loom")
    id("architectury-plugin")
}

val minecraft = stonecutter.current.version

version = "${mod.version}+$minecraft"
base {
    archivesName.set("${mod.id}-common")
}

architectury.common(stonecutter.tree.branches.mapNotNull {
    if (stonecutter.current.project in it) {
        it.project.prop("loom.platform")
    } else {
        null
    }
})

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings("net.fabricmc:yarn:$minecraft+build.${mod.dep("yarn_build")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${mod.dep("fabric_loader")}")
}

loom {
    accessWidenerPath = rootProject.file("src/main/resources/template.accesswidener")

    decompilers {
        get("vineflower").apply { // Adds names to lambdas - useful for mixins
            options.put("mark-corresponding-synthetics", "1")
        }
    }
}

repositories {
    fun strictMaven(name: String, url: String, vararg groups: String) = exclusiveContent {
        forRepository {
            maven(url) {
                this.name = name
            }
        }
        filter {
            groups.forEach(this::includeGroup)
        }
    }

    mavenCentral()
    strictMaven("CurseMaven", "https://cursemaven.com", "curse.maven")
    strictMaven("Modrinth", "https://api.modrinth.com/maven", "maven.modrinth")
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