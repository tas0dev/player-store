package io.github.tas0dev.mc.store

import io.github.tas0dev.mc.store.registry.ModBlockEntities
import io.github.tas0dev.mc.store.registry.ModBlocks
import io.github.tas0dev.mc.store.server.command.SilverCommands
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object PlayerStore : ModInitializer {
    const val MOD_ID: String = "playerstore"
    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        ModBlocks.register()
        ModBlockEntities.register()
        SilverCommands.register()
        logger.info("PlayerStore initialized")
    }
}
