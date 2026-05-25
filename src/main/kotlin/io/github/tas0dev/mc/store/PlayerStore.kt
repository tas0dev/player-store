package io.github.tas0dev.mc.store

import io.github.tas0dev.mc.store.net.SilverSync
import io.github.tas0dev.mc.store.registry.ModBlockEntities
import io.github.tas0dev.mc.store.registry.ModBlocks
import io.github.tas0dev.mc.store.server.command.BetCommand
import io.github.tas0dev.mc.store.server.command.SilverCommands
import io.github.tas0dev.mc.store.server.command.StoreTableOwnerDebugCommands

import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback

import org.slf4j.LoggerFactory

object PlayerStore : ModInitializer {

    const val MOD_ID = "playerstore"
    private val logger = LoggerFactory.getLogger(MOD_ID)

    override fun onInitialize() {
        ModBlocks.register()
        ModBlockEntities.register()
        SilverCommands.register()
        StoreTableOwnerDebugCommands.register()
        BetCommand.register()
        SilverSync.register()

        logger.info("PlayerStore initialized")
    }
}