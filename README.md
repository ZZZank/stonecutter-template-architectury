# Stonecutter Template (Architectury Loom)

Minecraft mod development workspace, utilizing [Stonecutter](https://stonecutter.kikugie.dev)
and [Architectury Loom](https://github.com/architectury/architectury-loom) to provide support for developing modern (
1.14.4+) multi-platform (Fabric/Forge/...) Minecraft mods.

Use `/gradlew buildAndCollect` to build mod file for all enabled platforms. Their result will be copied to
`./build/libs`, grouped by mod version and platform.

Commonly used maven is registered in [repo-convention.gradle.kts](./buildSrc/src/main/kotlin/repo-convention.gradle.kts)

## Project Structure

Similar to Architectury-only template, this template uses `common + {platform}` project layout. But there's no dedicated
`common` subproject, instead, the root project now serves as `common`.

To support multi-version development, there's a `versions` folder for holding version-specific data like
`gradle.properties`, in each subproject. Check Stonecutter Wiki for more details.

### Example: 1.20.1-forge

- `src`: common source code
- `forge/src`: Forge specific source code
- `forge/versions/1.20.1`: version-specific data for 1.20.1-forge
- `versions/1.20.1/gradle.properties`: properties applied to 1.20.1
- `forge/gradle.properties`: properties applied to Forge platform
- `gradle.properties`: properties applied globally

## Publishing

This template uses [ModPublisher](https://github.com/firstdarkdev/modpublisher) to handle publishing of mods. The plugin
includes four tasks. `publishMod`, `publishCurseforge`, `publishGitHub`, `publishModrinth` and can be found under the
`publishing` section of the Gradle tasks. `publishMod` can be used for publishing to all valid platforms

| Platform   | Properties ([gradle.properties](./gradle.properties)) | GitHub secret    |
|------------|-------------------------------------------------------|------------------|
| GitHub     | `publish.github`                                      | `GITHUB_TOKEN`   |
| Modrinth   | `publish.modrinth`                                    | `MODRINTH_TOKEN` |
| CurseForge | `publish.curseforge`                                  | `CURSE_TOKEN`    |

Note: In GitHub Action, `GITHUB_TOKEN` will be generated automatically.

## Related Resources

- Stonecutter: [IDEA plugin](https://plugins.jetbrains.com/plugin/25044-stonecutter-dev), [WIKI](https://stonecutter.kikugie.dev/wiki/), [Homepage](https://stonecutter.kikugie.dev/)
- Architectury: [WIKI](https://docs.architectury.dev/)
- Fabric: [WIKI](https://docs.architectury.dev/)
- Forge: [WIKI](https://docs.minecraftforge.net/)
