# Chat Extensions

Tokens in chat expand into rich components. Type `[item]` and the thing in your hand goes into the
message as a hoverable `[Netherite Pickaxe]` carrying its full tooltip - name, lore, enchantments,
attributes, durability.

The tooltip comes for free. The component carries the item stack itself, and the vanilla client
rebuilds the whole tooltip from it, so there is nothing to serialise and nothing to install on a
client.

## Tokens

| Token | Expands to |
| --- | --- |
| `[item]`, `[i]` | the item in the sender's hand, as a hoverable name |
| `[inv]` | a clickable link that runs `/invsee <sender>` |

Both lists are config, so `[hand]` or `[bag]` are a rename away.

## Chat ownership

Only one mod can own the `ALLOW_CHAT_MESSAGE` event - two of them fight. If
[nametags](https://github.com/Glance-Studios/nametags) is installed it owns the chat line, because
it is the one rewriting the whole message, and it calls this mod for the message body. When nametags
is absent this mod registers its own handler instead, and only takes over a line that actually uses
a token; everything else goes through vanilla untouched.

The seam is a plain `@JvmStatic` function, so nametags reaches it reflectively and neither mod has a
compile-time dependency on the other.

## Config (`config/chat-extensions.json`)

```json
{
  "itemTokens": ["[item]", "[i]"],
  "emptyHandText": "&7[nothing]",
  "showCount": true,
  "invTokens": ["[inv]"],
  "invLabel": "&b&n[Inventory]",
  "invHover": "&7Click to view &f%s&7's inventory",
  "invCommand": "/invsee %s"
}
```

`invCommand` is what the `[inv]` link runs, so it points at whatever your inventory viewer is called
rather than assuming. `showCount` prefixes a stack size when there is more than one.

## Adding a token

New tokens slot into the `tokens` list and the `expand` dispatch. The matching loop does not change.

## Server-side only - players install nothing

Rich components are a vanilla feature. Any client renders them.

## Requirements

Drop these into your **server's** `mods/` folder:

| Mod | Version |
| --- | --- |
| `chat-extensions-0.1.0.jar` | this mod |
| [Fabric API](https://modrinth.com/mod/fabric-api) | `0.155.2+26.2` (or compatible) |
| [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) | `1.13.12+kotlin.2.4.0` (or compatible) |

Minecraft **26.2**, Fabric Loader **0.19.3+**, **Java 25**.

## Limits

A token is replaced wherever it appears, including inside a word. The `[inv]` token does not check
whether an inventory viewer is installed - if nothing answers `invCommand`, the link is a dead
click.
