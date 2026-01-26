

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
