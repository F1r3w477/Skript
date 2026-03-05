![Skript Language](.github/assets/Cover.jpg)

---

# Skript
**Skript** is a Minecraft plugin for Paper, which allows server owners and other people
to modify their servers without learning Java. It can also be useful if you
*do* know Java; some tasks are quicker to do with Skript, and so it can be used
for prototyping etc.

This Github fork of Skript is based on Mirreski's improvements which was built
on Njol's original Skript.

## Requirements
Skript requires **Paper** to work. You heard it right, **Spigot** does *not* work.

Skript supports the last 18 months of Minecraft versions, counting from the release date of Skript's newest version.
For example, this means that 1.20.4 is supported, but 1.20.3 is *not*.

New Minecraft versions will be supported as soon as possible.

## Download
You can find the downloads for each version with their release notes in the [releases page](https://github.com/SkriptLang/Skript/releases).

Two major feature updates are expected each year in January and July, with monthly patches occurring in between. For full details, please review our [release model](CLOCKWORK_RELEASE_MODEL.md).

## Documentation
Documentation is available [here](https://docs.skriptlang.org/) for the
latest version of Skript.

## Reporting Issues
Please see our [contribution guidelines](https://github.com/SkriptLang/Skript/blob/master/.github/contributing.md)
before reporting issues.

## Help Us Test
Wanting to help test Skript's new features and releases?
You can head on over to our [Official Testing Discord](https://discord.gg/ZPsZAg6ygu), and whenever we start testing new features/releases you will be the first to know.

Please note this is not a help Discord.
If you require assistance with how to use Skript please check out the [Relevant Links](https://github.com/SkriptLang/Skript#relevant-links) section for a list of available resources to assist you.

## A Note About Add-ons
We don't support add-ons here, even though some of Skript developers have also
developed their own add-ons.

## Compiling
Skript uses Gradle for compilation. Use your command prompt of preference and
navigate to Skript's source directory. Then you can just call Gradle to compile
and package Skript for you:

```bash
./gradlew clean build # on UNIX-based systems (mac, linux)
gradlew clean build # on Windows
```

You can get source code from the [releases page](https://github.com/SkriptLang/Skript/releases).
You may also clone this repository, but that code may or may not be stable.

### Compiling Modules
Parts of Skript are provided as Gradle subprojects. They require Skript, so
they are compiled *after* it has been built. For this reason, if you want them
embedded in Skript jar, you must re-package it after compiling once. For example:

```
./gradlew jar
```

Note that modules are not necessary for Skript to work. Currently, they are
only used to provide compatibility with old WorldGuard versions.

### Testing
Skript has some tests written in Skript. Running them requires a Minecraft
server, but our build script will create one for you. Running the tests is easy:

```
./gradlew (quickTest|skriptTest|skriptTestJava21)
```

<code>quickTest</code> runs the test suite on newest supported server version.
<code>skriptTestJava21</code> (1.21+) runs the tests on Java 21 supported versions.
<code>skriptTest</code> runs the tests on all versions (currently identical to the Java 21 test).

To run the test suite on the **Fabric** port:

```
./gradlew quickTestFabric
```

This builds the Fabric mod JAR, starts a Fabric 1.21.11 server, and runs the shared Skript test suite against it.

By running the tests, you agree to Mojang's End User License Agreement.

---

## Skript on Fabric

This repository includes a **Fabric** port of Skript's core engine, allowing scripts to run on Minecraft 1.21.11 with Fabric and Fabric API. The Fabric mod uses the same platform-agnostic core as the Paper plugin; feature parity is being expanded over time (see [docs/PARITY.md](docs/PARITY.md)).

### Building the Fabric mod

From the project root:

```bash
./gradlew :skript-fabric:build
```

The mod JAR is produced at `skript-fabric/build/libs/Skript-Fabric.jar`.

### Installing on a Fabric server

1. Install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api) for Minecraft 1.21.11.
2. Copy `Skript-Fabric.jar` into the server's `mods` folder.
3. Start the server. Skript will create `config/skript/` and `config/skript/scripts/` the first time it runs.

### Script directory

Place your `.sk` script files in:

- **Default:** `<server root>/config/skript/scripts/`

Scripts are loaded at server start. Use the in-game command to reload without restarting:

- `/skript reload` — reload all scripts from the scripts directory
- `/skript test` — run the test suite (when the server is started under the test harness)

### Supported syntax (Fabric)

The Fabric port currently supports a minimal subset of the full Skript language:

- Event handlers: `on load:`, `on join:`, `on quit:`, `on world_load:`, `on world_unload:`
- Effects: `broadcast "message"`, `log "message"`
- Test mode: `test "name":` and `assert true is false with "message"` (for the test harness)

Example script (`config/skript/scripts/welcome.sk`):

```skript
on load:
    log "Skript (Fabric) scripts loaded."

on join:
    broadcast "A player joined!"
```

More language features and events are being migrated from the Paper plugin; see [docs/PARITY.md](docs/PARITY.md) for the current status.

### Releasing
```
./gradlew clean build
./gradlew <flavor>Release
```
Available flavors are github and spigot. Please do not abuse flavors by
compiling your own test builds as releases.

## Contributing
Please review our [contribution guidelines](https://github.com/SkriptLang/Skript/blob/master/.github/contributing.md).
In addition to that, if you are contributing Java code, check our
[coding conventions](https://github.com/SkriptLang/Skript/blob/master/code-conventions.md).

## Maven Repository
If you use Skript as (soft) dependency for your plugin, and use maven or Gradle,
this is for you.

First, you need to add the Maven repository at the **END** of all your repositories. Skript is not available in Maven Central.
```gradle
repositories {
    maven {
        url 'https://repo.skriptlang.org/releases'
    }
}
```

Or, if you use Maven:
```maven
<repositories>
    <repository>
        <id>skript-releases</id>
        <name>Skript Repository</name>
        <url>https://repo.skriptlang.org/releases</url>
    </repository>
</repositories>
```

For versions of Skript after dev37 you might need to add the paper-api repository to prevent build issues.

```gradle
maven {
    url 'https://repo.destroystokyo.com/repository/maven-public/'
}
```

Or, if you use Maven:
```maven
<repository>
    <id>destroystokyo-repo</id>
    <url>https://repo.destroystokyo.com/content/repositories/snapshots/</url>
</repository>
```

Then you will also need to add Skript as a dependency.
```gradle
dependencies {
    implementation 'com.github.SkriptLang:Skript:[versionTag]'
}
```

An example of the version tag would be ```2.8.5```.

> Note: If Gradle isn't able to resolve Skript's dependencies, just [disable the resolution of transitive dependencies](https://docs.gradle.org/current/userguide/resolution_rules.html#sec:disabling_resolution_transitive_dependencies) for Skript in your project.

Or, if you use Maven:
```
<dependency>
    <groupId>com.github.SkriptLang</groupId>
    <artifactId>Skript</artifactId>
    <version>[versionTag]</version>
    <scope>provided</scope>
</dependency>
```

## Relevant Links
* [skUnity forums](https://forums.skunity.com)
* [skUnity addon releases](https://forums.skunity.com/forums/addon-releases)
* [skUnity Discord invite](https://discord.gg/0l3WlzBPKX7WNjkf)
* [Skript Chat Discord invite](https://discord.gg/0lx4QhQvwelCZbEX)
* [Skript Hub](https://skripthub.net)
* [Original Skript at Bukkit](https://dev.bukkit.org/bukkit-plugins/skript) (inactive)

Note that these resources are not maintained by Skript's developers. Don't
contact us about any problems you might have with them.

## Developers
You can find all contributors [here](https://github.com/SkriptLang/Skript/graphs/contributors).

All code is owned by its writer, licensed for others under GPLv3 (see [LICENSE](LICENSE)).
Some contributors may choose to release their code under the MIT License.
Further information can be found within [LICENSING.md](LICENSING.md).

---

## Skript Fabric Port Workspace

This repository is also used as a workspace for a work-in-progress **Fabric-based port** of Skript.

### Branches

- `upstream-main`: read-only mirror of `SkriptLang/Skript`’s `master` branch. Never commit directly here; only fast‑forward from `upstream/master`.
- `fabric`: main development branch for the Fabric port, created from `upstream-main`.

To sync with upstream Skript and keep feature parity:

1. Update the local mirror:
   - `git checkout upstream-main`
   - `git fetch upstream`
   - `git pull --ff-only upstream master`
2. Merge the new upstream work into the Fabric branch:
   - `git checkout fabric`
   - `git merge upstream-main` (or `git rebase upstream-main`)

You can inspect what changed upstream since the last sync with:

```bash
git log <last-sync-tag>..upstream-main
git diff <last-sync-tag>..upstream-main
```

### Gradle modules

On the `fabric` branch, additional modules are declared in `settings.gradle`:

- `skript-core`: planned shared engine/runtime code for Skript, independent of any platform.
- `skript-bukkit`: planned Bukkit/Paper-specific layer which can be gradually extracted from the legacy plugin.
- `skript-fabric`: initial Fabric mod project that depends on `skript-core`.

The legacy Bukkit/Paper plugin remains in the root project for now; over time, logic can be moved into `skript-core` and platform-specific adapters in `skript-bukkit` / `skript-fabric`.

To get a quick snapshot of how much of the legacy parser and test surface has been migrated into the new modules, you can run:

```bash
./gradlew conversionReport
```

This prints counts of parser/test classes in the legacy `lang`, `patterns`, and `test` packages and how many have simple-name counterparts in `skript-core`, `skript-bukkit`, or `skript-fabric`.

### Building and running the Fabric mod

The Fabric module uses the Fabric Loom Gradle plugin and defines its own `fabric.mod.json` and entrypoint.

- To build the Fabric mod JAR:

```bash
./gradlew :skript-fabric:build
```

- To run a Fabric development server (provided by Loom):

```bash
./gradlew :skript-fabric:runServer
```

This will download the required Minecraft and Fabric dependencies on first run.

### Porting workflow (feature-by-feature)

When moving behaviour from the legacy Bukkit plugin into the shared core/Fabric/Bukkit modules, follow this workflow:

1. **Pick a feature slice** (for example, a group of expressions, a structure, or a small part of the parser) and identify its classes under `src/main/java/ch/njol/skript/**`.
2. **Extract platform-agnostic logic** into `skript-core` under an appropriate package, keeping it free of direct Bukkit/Fabric APIs. Use `SkriptPlatform`, `SkriptScheduler`, and `SkriptLogger` for any host interactions.
3. **Add or extend platform adapters** in `skript-bukkit` and `skript-fabric` to implement any new abstractions needed by the core.
4. **Ensure tests cover the behaviour** using the shared `.sk` test suite under `src/test/skript/tests`, and run both:
   - `./gradlew quickTest` (Bukkit/Paper)
   - `./gradlew quickTestFabric` (Fabric)
5. **Track progress over time** with `./gradlew conversionReport` so you can see the balance between legacy-only and core-backed code for parser/test-related classes.
