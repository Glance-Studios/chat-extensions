package dev.chatextensions

import com.google.gson.GsonBuilder
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

/**
 * User-editable config, stored at `config/chat-extensions.json`. Messages use `&` legacy colour codes.
 */
data class Config(
    // Tokens that expand to the item currently in the sender's hand. Matched case-insensitively.
    var itemTokens: List<String> = listOf("[item]", "[i]"),
    // Shown in place of the token when the sender's hands are empty. `&` codes allowed.
    var emptyHandText: String = "&7[nothing]",
    // Append " x<count>" after the item name when the held stack has more than one item.
    var showCount: Boolean = true,

    // Tokens that expand to a clickable "view my inventory" link (handled by the invsee mod).
    var invTokens: List<String> = listOf("[inv]"),
    // The clickable label shown in place of an inv token. `&` codes allowed.
    var invLabel: String = "&b&n[Inventory]",
    // Hover text on the inv link. `%s` = the sender's name.
    var invHover: String = "&7Click to view &f%s&7's inventory",
    // Command run on click. `%s` = the sender's (real) name. Must match the invsee command.
    var invCommand: String = "/invsee %s",
) {
    companion object {
        private val LOG = LoggerFactory.getLogger("chat-extensions")
        private val GSON = GsonBuilder().setPrettyPrinting().create()

        private fun path(): Path =
            FabricLoader.getInstance().configDir.resolve("chat-extensions.json")

        fun load(): Config {
            val p = path()
            return try {
                if (Files.exists(p)) {
                    val cfg = Files.newBufferedReader(p).use { GSON.fromJson(it, Config::class.java) }
                        ?: Config()
                    cfg.save() // rewrite so newly added fields get their defaults
                    cfg
                } else {
                    Config().also {
                        it.save()
                        LOG.info("Wrote default config to {}", p)
                    }
                }
            } catch (e: Exception) {
                LOG.error("Failed to read {}, using defaults.", p, e)
                Config()
            }
        }

        private fun Config.save() {
            val p = path()
            try {
                Files.createDirectories(p.parent)
                Files.newBufferedWriter(p).use { GSON.toJson(this, it) }
            } catch (e: Exception) {
                LOG.error("Failed to write {}", p, e)
            }
        }
    }
}
