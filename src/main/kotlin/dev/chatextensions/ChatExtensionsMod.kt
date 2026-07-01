package dev.chatextensions

import com.mojang.brigadier.Command
import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.ChatFormatting
import net.minecraft.commands.Commands
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import org.slf4j.LoggerFactory

/**
 * Server-side chat extensions over the token engine in [ChatExtensions]. When the nametags mod is
 * present it owns the chat line and calls [ChatExtensions.renderBody], so no chat handler is
 * registered here: two owners of ALLOW_CHAT_MESSAGE would fight.
 */
object ChatExtensionsMod : DedicatedServerModInitializer {

    private val LOG = LoggerFactory.getLogger("chat-extensions")

    override fun onInitializeServer() {
        ChatExtensions.configure(Config.load())

        if (FabricLoader.getInstance().isModLoaded("nametags")) {
            LOG.info("nametags detected, it owns the chat line and calls renderBody(); standalone chat handler disabled.")
        } else {
            registerStandaloneChat()
            LOG.info("Standalone chat handler enabled (no nametags present).")
        }

        registerCommand()
        LOG.info("Chat Extensions ready.")
    }

    /** Only registered when nametags is absent. Takes over a line only if it uses a token. */
    private fun registerStandaloneChat() {
        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(ServerMessageEvents.AllowChatMessage { message, sender, _ ->
            val server = (sender.level() as? ServerLevel)?.getServer() ?: return@AllowChatMessage true
            val text = message.signedContent()
            if (!ChatExtensions.containsToken(text)) return@AllowChatMessage true // leave plain chat to vanilla
            val line = Component.empty()
                .append(sender.displayName)
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(ChatExtensions.renderBody(sender, text))
            server.playerList.players.forEach { it.sendSystemMessage(line) }
            false // we broadcast our own line; cancel vanilla's
        })
    }

    private fun registerCommand() {
        CommandRegistrationCallback.EVENT.register { dispatcher, _, _ ->
            dispatcher.register(
                Commands.literal("chatext")
                    .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                    .then(
                        Commands.literal("reload").executes { ctx ->
                            ChatExtensions.configure(Config.load())
                            ctx.source.sendSuccess({ Fmt.legacy("&aReloaded chat-extensions config.") }, false)
                            Command.SINGLE_SUCCESS
                        },
                    ),
            )
        }
    }
}
