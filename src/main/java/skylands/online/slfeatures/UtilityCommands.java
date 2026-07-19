package skylands.online.slfeatures;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UtilityCommands implements CommandExecutor, TabCompleter {
    private final SLfeatures plugin;
    private final NamespacedKey ownerKey;

    public UtilityCommands(SLfeatures plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "lock_owner");
    }

    private String getMsg(String key, String def) {
        return plugin.getLangConfig().getString(key, def);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();

        if (cmdName.equals("scale")) {
            return handleScale(sender, args);
        } else if (cmdName.equals("lock") || cmdName.equals("unlock")) {
            return handleLockUnlock(sender, cmdName, args);
        } else if (cmdName.equals("serverutils")) {
            return handleServerUtils(sender, args);
        }

        return false;
    }

    private boolean handleServerUtils(CommandSender sender, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("serverutils.admin")) {
                sender.sendMessage(Component.text(getMsg("no_permission", "§cНедостаточно прав!"), NamedTextColor.RED));
                return true;
            }
            plugin.reloadPlugin();
            sender.sendMessage(Component.text(getMsg("reload_success", "§aКонфигурация плагина успешно перезагружена!"), NamedTextColor.GREEN));
            return true;
        }
        sender.sendMessage(Component.text("Использование: /serverutils reload", NamedTextColor.AQUA));
        return true;
    }

    private boolean handleScale(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(getMsg("only_players", "Только игроки могут использовать эту команду."), NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text(getMsg("scale_usage", "Использование: /scale <размер от 0.9 до 1.1>"), NamedTextColor.AQUA));
            return true;
        }

        try {
            double scale = Double.parseDouble(args[0]);
            if (scale < 0.9 || scale > 1.1) {
                player.sendMessage(Component.text(getMsg("scale_limit", "Размер должен быть в диапазоне от 0.9 до 1.1!"), NamedTextColor.RED));
                return true;
            }

            Attribute scaleAttr = null;
            try {
                scaleAttr = Attribute.valueOf("GENERIC_SCALE");
            } catch (Exception ignored) {}

            if (scaleAttr != null) {
                var attr = player.getAttribute(scaleAttr);
                if (attr != null) {
                    attr.setBaseValue(scale);
                    player.sendMessage(Component.text(getMsg("scale_success", "✔ Ваш размер успешно изменен на %scale%").replace("%scale%", String.valueOf(scale)), NamedTextColor.GREEN));
                } else {
                    player.sendMessage(Component.text(getMsg("scale_attribute_unsupported", "❌ Атрибут масштабирования недоступен на этой версии сервера."), NamedTextColor.RED));
                }
            } else {
                player.sendMessage(Component.text(getMsg("scale_unsupported", "❌ Масштабирование не поддерживается на этой версии сервера."), NamedTextColor.RED));
            }
        } catch (NumberFormatException e) {
            player.sendMessage(Component.text("Некорректное число.", NamedTextColor.RED));
        }
        return true;
    }

    private boolean handleLockUnlock(CommandSender sender, String cmdName, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(getMsg("only_players", "Только игроки могут использовать эту команду."), NamedTextColor.RED));
            return true;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() == Material.AIR) {
            player.sendMessage(Component.text(getMsg("lock_item_required", "❌ Вы должны держать предмет в руке!"), NamedTextColor.RED));
            return true;
        }

        Material type = item.getType();
        boolean lockable = type == Material.FILLED_MAP || 
                           type == Material.WRITTEN_BOOK || 
                           type.name().endsWith("_SMITHING_TEMPLATE");
        if (!lockable) {
            player.sendMessage(Component.text(getMsg("lock_type_invalid", "❌ Заблокировать можно только арты (карты), книги или шаблоны!"), NamedTextColor.RED));
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return true;
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (cmdName.equals("lock")) {
            if (pdc.has(ownerKey, PersistentDataType.STRING)) {
                String owner = pdc.get(ownerKey, PersistentDataType.STRING);
                if (player.getUniqueId().toString().equals(owner)) {
                    player.sendMessage(Component.text(getMsg("lock_already_self", "❌ Этот предмет уже заблокирован вами!"), NamedTextColor.RED));
                } else {
                    player.sendMessage(Component.text(getMsg("lock_already_other", "❌ Этот предмет заблокирован другим игроком!"), NamedTextColor.RED));
                }
                return true;
            }

            pdc.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());

            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();
            lore.add(Component.text("[🔒 Заблокировано: " + player.getName() + "]", NamedTextColor.GRAY));
            meta.lore(lore);

            item.setItemMeta(meta);
            player.sendMessage(Component.text(getMsg("lock_success", "✔ Предмет успешно заблокирован от копирования другими игроками!"), NamedTextColor.GREEN));
        } else if (cmdName.equals("unlock")) {
            if (!pdc.has(ownerKey, PersistentDataType.STRING)) {
                player.sendMessage(Component.text(getMsg("unlock_not_locked", "❌ Этот предмет не заблокирован!"), NamedTextColor.RED));
                return true;
            }

            String owner = pdc.get(ownerKey, PersistentDataType.STRING);
            if (!player.getUniqueId().toString().equals(owner)) {
                player.sendMessage(Component.text(getMsg("unlock_not_owner", "❌ Вы не являетесь владельцем этого предмета!"), NamedTextColor.RED));
                return true;
            }

            pdc.remove(ownerKey);

            List<Component> lore = meta.lore();
            if (lore != null) {
                lore.removeIf(line -> {
                    String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                    return plain.contains("Заблокировано:");
                });
                meta.lore(lore);
            }

            item.setItemMeta(meta);
            player.sendMessage(Component.text(getMsg("unlock_success", "✔ Предмет успешно разблокирован!"), NamedTextColor.GREEN));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        String cmdName = command.getName().toLowerCase();
        if (cmdName.equals("scale") && args.length == 1) {
            List<String> completions = new ArrayList<>();
            if ("0.9".startsWith(args[0])) completions.add("0.9");
            if ("1.0".startsWith(args[0])) completions.add("1.0");
            if ("1.1".startsWith(args[0])) completions.add("1.1");
            return completions;
        } else if (cmdName.equals("serverutils") && args.length == 1) {
            if (sender.hasPermission("serverutils.admin")) {
                if ("reload".startsWith(args[0].toLowerCase())) {
                    return Collections.singletonList("reload");
                }
            }
        }
        return Collections.emptyList();
    }
}
