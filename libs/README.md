# libs folder

Drop the **NeoForge 1.21.1** jars of Tensura: Reincarnated and ManasCore in here.
The build picks up every `*.jar` in this folder as a compile + runtime dependency
(see the `fileTree` line in `build.gradle`).

Required jars:

| Mod | File (example) | Download |
| --- | --- | --- |
| Tensura: Reincarnated | `tensura-neoforge-2.0.1.0.jar` | [Modrinth](https://modrinth.com/mod/tensura-reincarnated/versions?l=neoforge) / [CurseForge](https://www.curseforge.com/minecraft/mc-mods/tensura-reincarnated/files/all) |
| ManasCore | `manascore-neoforge-3.0.3.8.jar` | [Modrinth](https://modrinth.com/mod/manascore/versions?l=neoforge) / [CurseForge](https://www.curseforge.com/minecraft/mc-mods/manascore/files/all) |

Make sure you grab the **NeoForge** builds (not Fabric) for Minecraft **1.21.1**.

After adding or updating jars, refresh Gradle:

```
./gradlew --refresh-dependencies
```

Do not commit these jars to a public repository — both mods are "All Rights Reserved".
(`.gitignore` already excludes `libs/*.jar`.)
