package io.github.tas0dev.mc.store.client.config

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import java.nio.file.Files
import java.nio.file.Path

object PlayerStoreClientConfig {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val path: Path = FabricLoader.getInstance().configDir.resolve("playerstore-client.json")

    @Volatile
    var hudPosition: HudPosition = HudPosition.TOP_RIGHT
        private set

    fun load() {
        if (!Files.exists(path)) {
            save()
            return
        }
        runCatching {
            Files.newBufferedReader(path).use { reader ->
                val json = gson.fromJson(reader, JsonModel::class.java) ?: return
                hudPosition = json.hudPosition ?: HudPosition.TOP_RIGHT
            }
        }.onFailure {
            // Keep defaults if config is broken.
        }
    }

    fun setHudPosition(pos: HudPosition) {
        hudPosition = pos
        save()
    }

    private fun save() {
        runCatching {
            Files.createDirectories(path.parent)
            Files.newBufferedWriter(path).use { writer ->
                gson.toJson(JsonModel(hudPosition), writer)
            }
        }
    }

    private data class JsonModel(
        val hudPosition: HudPosition? = null,
    )
}

