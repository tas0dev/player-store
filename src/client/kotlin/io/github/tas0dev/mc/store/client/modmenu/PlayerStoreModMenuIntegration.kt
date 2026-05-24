package io.github.tas0dev.mc.store.client.modmenu

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import io.github.tas0dev.mc.store.client.config.HudPosition
import io.github.tas0dev.mc.store.client.config.PlayerStoreClientConfig
import me.shedaniel.clothconfig2.api.ConfigBuilder
import me.shedaniel.clothconfig2.api.ConfigCategory
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text

object PlayerStoreModMenuIntegration : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> {
        return ConfigScreenFactory { parent -> buildScreen(parent) }
    }

    private fun buildScreen(parent: Screen): Screen {
        val builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Text.literal("PlayerStore"))
            .setSavingRunnable { PlayerStoreClientConfig.setHudPosition(PlayerStoreClientConfig.hudPosition) }

        val entryBuilder: ConfigEntryBuilder = builder.entryBuilder()
        val category: ConfigCategory = builder.getOrCreateCategory(Text.literal("HUD"))

        category.addEntry(
            entryBuilder.startEnumSelector(
                Text.literal("Silver HUD Position"),
                HudPosition::class.java,
                PlayerStoreClientConfig.hudPosition,
            )
                .setDefaultValue(HudPosition.TOP_RIGHT)
                .setEnumNameProvider { value -> Text.literal(value.name.lowercase()) }
                .setSaveConsumer { value -> PlayerStoreClientConfig.setHudPosition(value) }
                .build(),
        )

        return builder.build()
    }
}

