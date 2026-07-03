# Ethan Veritas — Tensura: Reincarnated Addon

A [Tensura: Reincarnated](https://modrinth.com/mod/tensura-reincarnated) addon for **Minecraft 1.21.1** on **NeoForge**.

## Requirements

- Java 21 (the Gradle toolchain will download it automatically if missing)
- The NeoForge 1.21.1 jars of **Tensura: Reincarnated** and **ManasCore** placed in the [`libs/`](libs/README.md) folder

## Setup

1. Download the **NeoForge 1.21.1** builds of:
   - [Tensura: Reincarnated](https://modrinth.com/mod/tensura-reincarnated/versions?l=neoforge) (e.g. `tensura-neoforge-2.0.1.0.jar`)
   - [ManasCore](https://modrinth.com/mod/manascore/versions?l=neoforge) (e.g. `manascore-neoforge-3.0.3.8.jar`)
2. Put both jars into the `libs/` folder.
3. Build:

   ```
   ./gradlew build
   ```

   The finished addon jar ends up in `build/libs/`.

4. Run the dev client (Tensura, ManasCore and Architectury will all be loaded):

   ```
   ./gradlew runClient
   ```

Architectury API (required by Tensura and ManasCore) is resolved automatically from
its maven, so you don't need to download it yourself.

Instead of the `libs/` folder you can also resolve Tensura/ManasCore from the Modrinth
maven — see the commented lines in the `dependencies` block of `build.gradle`.

## Useful references

- [BanditHelps/TensuraAddonExample](https://github.com/BanditHelps/TensuraAddonExample) — example addon (skills, races) for the older 1.19.2 version; the concepts still apply
- [RisingEclipse/Tensura-Starlight-1.21.1](https://github.com/RisingEclipse/Tensura-Starlight-1.21.1) — a real addon on the same NeoForge 1.21.1 setup as this project
- [ManasMods/ManasCore](https://github.com/ManasMods/ManasCore) — the library Tensura is built on (skill/race APIs)
- [ManasMods/ManasMods-1.21-Template](https://github.com/ManasMods/ManasMods-1.21-Template) — official ManasMods 1.21 template
