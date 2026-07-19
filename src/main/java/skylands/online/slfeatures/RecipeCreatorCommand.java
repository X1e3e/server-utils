package skylands.online.slfeatures;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class RecipeCreatorCommand implements CommandExecutor, TabCompleter, Listener {
    private final SLfeatures plugin;
    private final Map<UUID, String> pendingRecipes = new HashMap<>();

    // GUI Slots
    private static final int SAVE_SLOT = 15;
    private static final int CANCEL_SLOT = 16;
    private static final int RESULT_SLOT = 24;
    private static final Set<Integer> GRID_SLOTS = Set.of(10, 11, 12, 19, 20, 21, 28, 29, 30);

    public RecipeCreatorCommand(SLfeatures plugin) {
        this.plugin = plugin;
    }

    private String getMsg(String key, String def) {
        return plugin.getLangConfig().getString(key, def);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(getMsg("only_players", "Только игроки могут использовать эту команду."), NamedTextColor.RED));
            return true;
        }

        if (!player.hasPermission("serverutils.admin")) {
            player.sendMessage(Component.text(getMsg("no_permission", "❌ Недостаточно прав!"), NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Использование: /recipecreator <название рецепта>", NamedTextColor.RED));
            return true;
        }

        String recipeName = args[0].replaceAll("[^a-zA-Z0-9_]", "").toLowerCase();
        if (recipeName.isEmpty()) {
            player.sendMessage(Component.text("❌ Некорректное название рецепта!", NamedTextColor.RED));
            return true;
        }

        pendingRecipes.put(player.getUniqueId(), recipeName);
        openRecipeCreatorGui(player, recipeName);
        return true;
    }

    private void openRecipeCreatorGui(Player player, String recipeName) {
        Inventory inv = Bukkit.createInventory(player, 45, Component.text("Создание рецепта: " + recipeName));

        // Fill background
        ItemStack bg = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta bgMeta = bg.getItemMeta();
        if (bgMeta != null) {
            bgMeta.displayName(Component.text(" "));
            bg.setItemMeta(bgMeta);
        }

        for (int i = 0; i < inv.getSize(); i++) {
            if (!GRID_SLOTS.contains(i) && i != RESULT_SLOT && i != SAVE_SLOT && i != CANCEL_SLOT) {
                inv.setItem(i, bg);
            }
        }

        // Save Button
        ItemStack saveBtn = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta saveMeta = saveBtn.getItemMeta();
        if (saveMeta != null) {
            saveMeta.displayName(Component.text("§a[Сохранить рецепт]"));
            saveMeta.lore(List.of(
                Component.text("§7Нажмите для сохранения рецепта"),
                Component.text("§7в config.yml и активации на сервере.")
            ));
            saveBtn.setItemMeta(saveMeta);
        }
        inv.setItem(SAVE_SLOT, saveBtn);

        // Cancel Button
        ItemStack cancelBtn = new ItemStack(Material.RED_STAINED_GLASS_PANE);
        ItemMeta cancelMeta = cancelBtn.getItemMeta();
        if (cancelMeta != null) {
            cancelMeta.displayName(Component.text("§c[Отмена]"));
            cancelMeta.lore(List.of(
                Component.text("§7Закрыть окно без сохранения.")
            ));
            cancelBtn.setItemMeta(cancelMeta);
        }
        inv.setItem(CANCEL_SLOT, cancelBtn);

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!pendingRecipes.containsKey(uuid)) return;

        Inventory inv = event.getInventory();
        // Prevent click in custom inventory upper slots if it's not a grid slot or result slot
        if (event.getRawSlot() < inv.getSize()) {
            int slot = event.getSlot();
            if (!GRID_SLOTS.contains(slot) && slot != RESULT_SLOT) {
                event.setCancelled(true);

                if (slot == SAVE_SLOT) {
                    saveRecipe(player, inv);
                } else if (slot == CANCEL_SLOT) {
                    player.closeInventory();
                    player.sendMessage(Component.text("❌ Создание рецепта отменено.", NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!pendingRecipes.containsKey(uuid)) return;

        Inventory inv = event.getInventory();
        // Give back grid items and result items to player
        List<ItemStack> itemsToReturn = new ArrayList<>();
        for (int slot : GRID_SLOTS) {
            ItemStack item = inv.getItem(slot);
            if (item != null && item.getType() != Material.AIR) {
                itemsToReturn.add(item);
            }
        }
        ItemStack resultItem = inv.getItem(RESULT_SLOT);
        if (resultItem != null && resultItem.getType() != Material.AIR) {
            itemsToReturn.add(resultItem);
        }

        for (ItemStack item : itemsToReturn) {
            HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(item);
            for (ItemStack left : leftOver.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), left);
            }
        }

        pendingRecipes.remove(uuid);
    }

    private void saveRecipe(Player player, Inventory inv) {
        String recipeName = pendingRecipes.get(player.getUniqueId());
        if (recipeName == null) return;

        ItemStack result = inv.getItem(RESULT_SLOT);
        if (result == null || result.getType() == Material.AIR) {
            player.sendMessage(Component.text("❌ Вы не положили результат крафта!", NamedTextColor.RED));
            return;
        }

        // Get grid items in order
        ItemStack[] grid = new ItemStack[9];
        int idx = 0;
        // Keep order matching 3x3 rows
        List<Integer> sortedGridSlots = new ArrayList<>(GRID_SLOTS);
        Collections.sort(sortedGridSlots);
        for (int slot : sortedGridSlots) {
            grid[idx++] = inv.getItem(slot);
        }

        // Find unique materials to map to characters
        Map<Material, Character> materialCharMap = new HashMap<>();
        char nextChar = 'A';
        for (ItemStack item : grid) {
            if (item == null || item.getType() == Material.AIR) continue;
            Material mat = item.getType();
            if (!materialCharMap.containsKey(mat)) {
                materialCharMap.put(mat, nextChar);
                nextChar++;
            }
        }

        // Construct shape lines
        List<String> shapeLines = new ArrayList<>();
        for (int row = 0; row < 3; row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < 3; col++) {
                ItemStack item = grid[row * 3 + col];
                if (item == null || item.getType() == Material.AIR) {
                    line.append(" ");
                } else {
                    line.append(materialCharMap.get(item.getType()));
                }
            }
            shapeLines.add(line.toString());
        }

        // Save to config
        String path = "custom-recipes." + recipeName;
        plugin.getConfig().set(path + ".enabled", true);
        plugin.getConfig().set(path + ".result.type", result.getType().name());
        plugin.getConfig().set(path + ".result.amount", result.getAmount());

        if (result.hasItemMeta()) {
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                if (meta.hasDisplayName() && meta.displayName() != null) {
                    String displayName = LegacyComponentSerializer.legacyAmpersand().serialize(meta.displayName());
                    plugin.getConfig().set(path + ".result.name", displayName);
                }
                if (meta.hasLore() && meta.lore() != null) {
                    List<String> loreStrings = new ArrayList<>();
                    for (Component loreLine : meta.lore()) {
                        loreStrings.add(LegacyComponentSerializer.legacyAmpersand().serialize(loreLine));
                    }
                    plugin.getConfig().set(path + ".result.lore", loreStrings);
                }
            }
        }

        plugin.getConfig().set(path + ".shape", shapeLines);

        for (Map.Entry<Material, Character> entry : materialCharMap.entrySet()) {
            plugin.getConfig().set(path + ".ingredients." + entry.getValue(), entry.getKey().name());
        }

        plugin.saveConfig();

        // Remove UUID from map before closing to prevent item return logic
        pendingRecipes.remove(player.getUniqueId());

        // Close inventory
        player.closeInventory();

        // Reload plugin to dynamically register the recipe
        plugin.reloadPlugin();

        player.sendMessage(Component.text("✔ Рецепт '" + recipeName + "' успешно сохранен в конфиг и зарегистрирован!", NamedTextColor.GREEN));
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return Collections.emptyList();
    }
}
