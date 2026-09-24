# Local Minecraft-dependent build

Folium does not store Minecraft binaries.

For modules that compile against Minecraft's public class signatures, point
Gradle at your locally installed 26.3 client JAR.

Example:

```bash
gradle :render-webgpu:compileJava \
  -PminecraftJar="$HOME/.local/share/PandoraLauncher/libraries/net/minecraft/26.3/minecraft-client-26.3.jar"
```

Or export:

```bash
export FOLIUM_MINECRAFT_JAR="$HOME/.local/share/PandoraLauncher/libraries/net/minecraft/26.3/minecraft-client-26.3.jar"
gradle :render-webgpu:compileJava
```

The module uses Java 25 because Minecraft 26.3 targets Java 25.

The local JAR must never be committed.
