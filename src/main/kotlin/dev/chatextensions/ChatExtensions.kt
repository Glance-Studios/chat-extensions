package dev.chatextensions

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack

/**
 * The chat-token engine and the mod's public API. [renderBody] turns a raw chat string into a rich
 * [Component]; it is a plain `@JvmStatic` function so the nametags mod can reach it reflectively
 * with no compile-time dependency. New tokens slot into [tokens] and the [expand] dispatch.
 */
object ChatExtensions {

    private enum class Kind { ITEM, INV }
    private data class Token(val text: String, val kind: Kind)

    @Volatile private var cfg: Config = Config()

    /** All active tokens, longest-first so e.g. `[item]` wins over `[i]` at the same position. */
    @Volatile private var tokens: List<Token> = emptyList()

    init { rebuildTokens() }

    fun configure(config: Config) {
        cfg = config
        rebuildTokens()
    }

    private fun rebuildTokens() {
        val items = cfg.itemTokens.map { Token(it, Kind.ITEM) }
        val invs = cfg.invTokens.map { Token(it, Kind.INV) }
        tokens = (items + invs).filter { it.text.isNotEmpty() }.sortedByDescending { it.text.length }
    }

    /** Cheap pre-check: does [text] contain any token at all? */
    fun containsToken(text: String): Boolean =
        tokens.any { text.contains(it.text, ignoreCase = true) }

    /**
     * Turn the raw chat [text] from [sender] into a rich component, expanding tokens. Returns a plain
     * literal when there's nothing to expand.
     */
    @JvmStatic
    fun renderBody(sender: ServerPlayer, text: String): Component {
        if (tokens.isEmpty() || !containsToken(text)) return Component.literal(text)

        val out: MutableComponent = Component.empty()
        val buf = StringBuilder()
        fun flushLiteral() {
            if (buf.isNotEmpty()) {
                out.append(Component.literal(buf.toString()))
                buf.setLength(0)
            }
        }

        var i = 0
        while (i < text.length) {
            val tok = tokens.firstOrNull { text.regionMatches(i, it.text, 0, it.text.length, ignoreCase = true) }
            if (tok != null) {
                flushLiteral()
                out.append(expand(tok.kind, sender))
                i += tok.text.length
            } else {
                buf.append(text[i])
                i++
            }
        }
        flushLiteral()
        return out
    }

    private fun expand(kind: Kind, sender: ServerPlayer): Component = when (kind) {
        Kind.ITEM -> itemComponent(sender)
        Kind.INV -> invComponent(sender)
    }

    /** The held item as a bracketed, hoverable component (or the configured empty-hand text). */
    private fun itemComponent(sender: ServerPlayer): Component {
        val stack = heldItem(sender)
        if (stack.isEmpty) return Fmt.legacy(cfg.emptyHandText)
        // getDisplayName() is already the bracketed, rarity-coloured name carrying a ShowItem hover.
        // The vanilla client rebuilds the whole tooltip from it: name, lore, enchants, attributes,
        // durability, so the full name, lore and stats come for free with no client mod.
        val comp: MutableComponent = Component.empty().append(stack.displayName)
        if (cfg.showCount && stack.count > 1) {
            comp.append(Component.literal(" x${stack.count}").withStyle(ChatFormatting.GRAY))
        }
        return comp
    }

    /** A clickable link that runs the invsee command targeting the sender. */
    private fun invComponent(sender: ServerPlayer): Component {
        val name = sender.gameProfile.name
        val hover = Fmt.legacy(cfg.invHover.format(name))
        val command = cfg.invCommand.format(name)
        return Fmt.legacy(cfg.invLabel).withStyle {
            it.withHoverEvent(HoverEvent.ShowText(hover))
                .withClickEvent(ClickEvent.RunCommand(command))
        }
    }

    /** Main hand if it holds something, otherwise the offhand. */
    private fun heldItem(sender: ServerPlayer): ItemStack {
        val main = sender.mainHandItem
        return if (!main.isEmpty) main else sender.offhandItem
    }
}
