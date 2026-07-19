package skylands.online.slfeatures;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.util.List;

public final class SLfeatures extends JavaPlugin implements Listener {

    private NamespacedKey invisibleFrameKey;
    private BukkitTask tabTask;
    private String lang;
    private org.bukkit.configuration.file.FileConfiguration langConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        
        lang = getConfig().getString("lang", "ru").toLowerCase();
        loadLangConfig();
        invisibleFrameKey = new NamespacedKey(this, "invisible");
        
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new FeaturesListener(this), this);
        
        if (getConfig().getBoolean("features.hide-nametags", true)) {
            setupScoreboard();
        }
        if (getConfig().getBoolean("features.recipes.invisible-frame", true)) {
            registerInvisibleFrameRecipe();
        }
        if (getConfig().getBoolean("features.recipes.invisible-light", true)) {
            registerInvisibleLightRecipe();
        }
        if (getConfig().getBoolean("features.recipes.debug-stick", true)) {
            registerDebugStickRecipe();
        }
        
        loadCustomRecipes();
        
        UtilityCommands utilCmds = new UtilityCommands(this);
        if (getCommand("scale") != null) {
            getCommand("scale").setExecutor(utilCmds);
            getCommand("scale").setTabCompleter(utilCmds);
        }
        if (getCommand("lock") != null) {
            getCommand("lock").setExecutor(utilCmds);
        }
        if (getCommand("unlock") != null) {
            getCommand("unlock").setExecutor(utilCmds);
        }
        if (getCommand("serverutils") != null) {
            getCommand("serverutils").setExecutor(utilCmds);
            getCommand("serverutils").setTabCompleter(utilCmds);
        }
        
        if (getConfig().getBoolean("features.tab-formatter", true)) {
            startTabFormatter();
        }
        
        getLogger().info("SLfeatures has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        if (tabTask != null) {
            tabTask.cancel();
        }
    }

    private void setupScoreboard() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("hidden_names");
        if (team == null) {
            team = scoreboard.registerNewTeam("hidden_names");
        }
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        
        // Add all online players to the team
        for (Player player : Bukkit.getOnlinePlayers()) {
            team.addEntry(player.getName());
        }
    }

    private void registerInvisibleFrameRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(this, "invisible_item_frame");
        
        ItemStack result = new ItemStack(Material.ITEM_FRAME);
        ItemMeta meta = result.getItemMeta();
        if (meta != null) {
            String frameName = getLangConfig().getString("invisible_frame_name", "Невидимая рамка");
            meta.displayName(net.kyori.adventure.text.Component.text(frameName, net.kyori.adventure.text.format.NamedTextColor.GOLD));
            meta.getPersistentDataContainer().set(invisibleFrameKey, PersistentDataType.BYTE, (byte) 1);
            result.setItemMeta(meta);
        }
        
        ShapelessRecipe recipe = new ShapelessRecipe(recipeKey, result);
        recipe.addIngredient(Material.ITEM_FRAME);
        recipe.addIngredient(Material.POTION); // Will be validated in PrepareItemCraftEvent
        
        Bukkit.addRecipe(recipe);
    }

    private void registerInvisibleLightRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(this, "invisible_light");
        
        ItemStack result = new ItemStack(Material.LIGHT, 4);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(
            " G ",
            "GLG",
            " G "
        );
        recipe.setIngredient('G', Material.GLOWSTONE_DUST);
        recipe.setIngredient('L', Material.GLASS);
        
        Bukkit.addRecipe(recipe);
    }

    private void registerDebugStickRecipe() {
        NamespacedKey recipeKey = new NamespacedKey(this, "debug_stick_recipe");
        
        ItemStack result = new ItemStack(Material.DEBUG_STICK, 1);
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, result);
        recipe.shape(
            "  B",
            " D ",
            "S  "
        );
        recipe.setIngredient('B', Material.DIAMOND_BLOCK);
        recipe.setIngredient('D', Material.DIAMOND);
        recipe.setIngredient('S', Material.STICK);
        
        Bukkit.addRecipe(recipe);
    }

    private void startTabFormatter() {
        tabTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
            int max = Bukkit.getMaxPlayers();

            String title = getLangConfig().getString("tab_title", "Minecraft");
            net.kyori.adventure.text.format.TextColor blueColor = net.kyori.adventure.text.format.TextColor.color(0x3b82f6);
            net.kyori.adventure.text.Component header = net.kyori.adventure.text.Component.text()
                .append(net.kyori.adventure.text.Component.text("\n   ", blueColor))
                .append(net.kyori.adventure.text.Component.text(title, blueColor, net.kyori.adventure.text.format.TextDecoration.BOLD))
                .append(net.kyori.adventure.text.Component.text("   \n", blueColor))
                .build();

            for (Player player : Bukkit.getOnlinePlayers()) {
                String worldName = player.getWorld().getName().toLowerCase();
                String icon = getWorldSymbol(worldName);
                net.kyori.adventure.text.format.TextColor iconColor = getWorldColor(worldName);

                // Update nickname in Tab list (keep [V] prefix if player is vanished)
                boolean isVanished = player.hasMetadata("vanished") && !player.getMetadata("vanished").isEmpty() && player.getMetadata("vanished").get(0).asBoolean();
                
                net.kyori.adventure.text.Component listName;
                if (isVanished) {
                    listName = net.kyori.adventure.text.Component.text()
                        .append(net.kyori.adventure.text.Component.text("[V] ", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                        .append(net.kyori.adventure.text.Component.text(icon + " ", iconColor))
                        .append(net.kyori.adventure.text.Component.text(player.getName(), net.kyori.adventure.text.format.NamedTextColor.WHITE))
                        .build();
                } else {
                    listName = net.kyori.adventure.text.Component.text()
                        .append(net.kyori.adventure.text.Component.text(icon + " ", iconColor))
                        .append(net.kyori.adventure.text.Component.text(player.getName(), net.kyori.adventure.text.format.NamedTextColor.WHITE))
                        .build();
                }
                player.playerListName(listName);

                // Update Header and Footer
                String role = getPlayerRole(player);
                int ping = player.getPing();

                // Count visible players for this specific player
                int visibleOnlineCount = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    boolean pVanished = p.hasMetadata("vanished") && !p.getMetadata("vanished").isEmpty() && p.getMetadata("vanished").get(0).asBoolean();
                    if (!pVanished || player.hasPermission("vanish.see") || player.equals(p)) {
                        visibleOnlineCount++;
                    }
                }

                String labelOnline = getLangConfig().getString("tab_online", "Онлайн: ");
                String labelRole = getLangConfig().getString("tab_role", "Ваша роль: ");
                String labelPing = getLangConfig().getString("tab_ping", "Пинг: ");
                String labelPingMs = getLangConfig().getString("tab_ping_ms", " мс");

                net.kyori.adventure.text.Component footer = net.kyori.adventure.text.Component.text()
                    .append(net.kyori.adventure.text.Component.text("\n", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                    .append(net.kyori.adventure.text.Component.text(labelOnline, net.kyori.adventure.text.format.NamedTextColor.GRAY))
                    .append(net.kyori.adventure.text.Component.text(visibleOnlineCount + " / " + max + "\n", net.kyori.adventure.text.format.NamedTextColor.YELLOW))
                    .append(net.kyori.adventure.text.Component.text(labelRole, net.kyori.adventure.text.format.NamedTextColor.GRAY))
                    .append(net.kyori.adventure.text.Component.text(role + "\n", net.kyori.adventure.text.format.NamedTextColor.WHITE))
                    .append(net.kyori.adventure.text.Component.text(labelPing, net.kyori.adventure.text.format.NamedTextColor.GRAY))
                    .append(net.kyori.adventure.text.Component.text(ping + labelPingMs + "\n", net.kyori.adventure.text.format.NamedTextColor.AQUA))
                    .build();

                player.sendPlayerListHeaderAndFooter(header, footer);
            }
        }, 0L, 40L);
    }

    private String getWorldSymbol(String worldName) {
        String baseWorld = "default";
        if (worldName.equals("farms") || worldName.equals("farms_nether")) {
            baseWorld = "farms";
        } else if (worldName.equals("arts")) {
            baseWorld = "arts";
        } else if (worldName.equals("banished") || worldName.equals("banished_nether")) {
            baseWorld = "banished";
        }
        return getLangConfig().getString("world_prefixes." + baseWorld, "●");
    }

    private net.kyori.adventure.text.format.TextColor getWorldColor(String worldName) {
        if (worldName.equals("farms") || worldName.equals("farms_nether")) {
            return net.kyori.adventure.text.format.NamedTextColor.GOLD;
        } else if (worldName.equals("arts")) {
            return net.kyori.adventure.text.format.NamedTextColor.LIGHT_PURPLE;
        } else if (worldName.equals("banished") || worldName.equals("banished_nether")) {
            return net.kyori.adventure.text.format.NamedTextColor.RED;
        } else {
            return net.kyori.adventure.text.format.NamedTextColor.WHITE;
        }
    }

    private String getPlayerRole(Player player) {
        // 1. Check custom configured permission mapping first
        org.bukkit.configuration.ConfigurationSection rolePerms = getConfig().getConfigurationSection("role-permissions");
        if (rolePerms != null) {
            for (String roleKey : rolePerms.getKeys(false)) {
                String perm = rolePerms.getString(roleKey);
                if (perm != null && !perm.isEmpty() && player.hasPermission(perm)) {
                    String mappedRole = getLangConfig().getString("roles." + roleKey.toLowerCase());
                    if (mappedRole != null) {
                        return mappedRole;
                    }
                }
            }
        }

        // 2. Fallback to blockcommand config integration
        org.bukkit.plugin.Plugin blockcommand = Bukkit.getPluginManager().getPlugin("blockcommand");
        if (blockcommand != null) {
            File playersFile = new File(blockcommand.getDataFolder(), "players.yml");
            if (playersFile.exists()) {
                org.bukkit.configuration.file.YamlConfiguration config = 
                    org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(playersFile);
                List<String> roles = config.getStringList("players." + player.getUniqueId().toString() + ".roles");
                if (roles != null && !roles.isEmpty()) {
                    String firstRole = roles.get(0).toLowerCase();
                    String mappedRole = getLangConfig().getString("roles." + firstRole);
                    if (mappedRole != null) {
                        return mappedRole;
                    }
                }
            }
        }
        return getLangConfig().getString("roles.default", "§7Житель");
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null && event.getRecipe().getResult().getType() == Material.ITEM_FRAME) {
            if (event.getRecipe() instanceof ShapelessRecipe) {
                ShapelessRecipe shapeless = (ShapelessRecipe) event.getRecipe();
                if (shapeless.getKey().getKey().equals("invisible_item_frame")) {
                    boolean hasInvisibilityPotion = false;
                    for (ItemStack item : event.getInventory().getMatrix()) {
                        if (item != null && item.getType() == Material.POTION) {
                            PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
                            if (potionMeta != null && potionMeta.getBasePotionType() != null) {
                                String typeName = potionMeta.getBasePotionType().getKey().getKey();
                                if (typeName.contains("invisibility")) {
                                    hasInvisibilityPotion = true;
                                }
                            }
                        }
                    }
                    if (!hasInvisibilityPotion) {
                        event.getInventory().setResult(null); // Deny craft if not potion of invisibility
                    }
                }
            }
        }
    }

    @EventHandler
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getEntity() instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) event.getEntity();
            ItemStack item = event.getItemStack();
            if (item != null && item.hasItemMeta()) {
                if (item.getItemMeta().getPersistentDataContainer().has(invisibleFrameKey, PersistentDataType.BYTE)) {
                    frame.setVisible(false);
                }
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getEntity() instanceof ItemFrame) {
            ItemFrame frame = (ItemFrame) event.getEntity();
            if (!frame.isVisible()) {
                if (event.getDamager() instanceof Player) {
                    Player player = (Player) event.getDamager();
                    if (player.getGameMode() != org.bukkit.GameMode.CREATIVE) {
                        event.setCancelled(true);
                        
                        ItemStack invisibleFrame = new ItemStack(Material.ITEM_FRAME);
                        ItemMeta meta = invisibleFrame.getItemMeta();
                        if (meta != null) {
                            String frameName = getLangConfig().getString("invisible_frame_name", "Невидимая рамка");
                            meta.displayName(net.kyori.adventure.text.Component.text(frameName, net.kyori.adventure.text.format.NamedTextColor.GOLD));
                            meta.getPersistentDataContainer().set(invisibleFrameKey, PersistentDataType.BYTE, (byte) 1);
                            invisibleFrame.setItemMeta(meta);
                        }
                        
                        frame.getLocation().getWorld().dropItemNaturally(frame.getLocation(), invisibleFrame);
                        frame.remove();
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.getRightClicked() instanceof Player) {
            Player clicked = (Player) event.getRightClicked();
            Player clicker = event.getPlayer();
            
            net.kyori.adventure.text.Component message = net.kyori.adventure.text.Component.text()
                .append(net.kyori.adventure.text.Component.text("> ", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text(clicked.getName(), net.kyori.adventure.text.format.NamedTextColor.GRAY))
                .append(net.kyori.adventure.text.Component.text(" <", net.kyori.adventure.text.format.NamedTextColor.GRAY))
                .build();
            
            clicker.sendActionBar(message);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("hidden_names");
        if (team != null) {
            team.addEntry(event.getPlayer().getName());
        }
        Bukkit.getScheduler().runTaskLater(this, this::updateAllTabVisibility, 1L);
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        updateAllTabVisibility();
    }

    private boolean isBanished(Player player) {
        String worldName = player.getWorld().getName().toLowerCase();
        return worldName.equals("banished") || worldName.equals("banished_nether");
    }

    private void updateAllTabVisibility() {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            boolean viewerBanished = isBanished(viewer);
            for (Player target : Bukkit.getOnlinePlayers()) {
                if (viewer.equals(target)) continue;
                boolean targetBanished = isBanished(target);
                if (viewerBanished != targetBanished) {
                    viewer.hidePlayer(this, target);
                } else {
                    viewer.showPlayer(this, target);
                }
            }
        }
    }

    public void loadLangConfig() {
        java.io.File langFolder = new java.io.File(getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        java.io.File langFile = new java.io.File(langFolder, "messages_" + lang + ".yml");
        if (!langFile.exists()) {
            try {
                saveResource("lang/messages_ru.yml", false);
            } catch (Exception ignored) {}
            try {
                saveResource("lang/messages_en.yml", false);
            } catch (Exception ignored) {}
        }
        langConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.File(langFolder, "messages_" + lang + ".yml"));
    }

    public org.bukkit.configuration.file.FileConfiguration getLangConfig() {
        if (langConfig == null) {
            loadLangConfig();
        }
        return langConfig;
    }

    private final java.util.List<NamespacedKey> customRecipeKeys = new java.util.ArrayList<>();

    private void loadCustomRecipes() {
        // Unregister old custom recipes on reload
        for (NamespacedKey key : customRecipeKeys) {
            try {
                Bukkit.removeRecipe(key);
            } catch (Exception ignored) {}
        }
        customRecipeKeys.clear();

        org.bukkit.configuration.ConfigurationSection customSection = getConfig().getConfigurationSection("custom-recipes");
        if (customSection == null) return;

        for (String key : customSection.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection recipeSec = customSection.getConfigurationSection(key);
            if (recipeSec == null || !recipeSec.getBoolean("enabled", true)) continue;

            // Result
            String resultTypeStr = recipeSec.getString("result.type");
            if (resultTypeStr == null) continue;
            Material resultMat = Material.getMaterial(resultTypeStr.toUpperCase());
            if (resultMat == null) {
                getLogger().warning("Invalid material for custom recipe result: " + resultTypeStr);
                continue;
            }

            int amount = recipeSec.getInt("result.amount", 1);
            ItemStack result = new ItemStack(resultMat, amount);

            // Display name & Lore
            ItemMeta meta = result.getItemMeta();
            if (meta != null) {
                String displayName = recipeSec.getString("result.name");
                if (displayName != null && !displayName.isEmpty()) {
                    meta.displayName(net.kyori.adventure.text.Component.text(
                        org.bukkit.ChatColor.translateAlternateColorCodes('&', displayName)
                    ));
                }
                List<String> loreLines = recipeSec.getStringList("result.lore");
                if (loreLines != null && !loreLines.isEmpty()) {
                    List<net.kyori.adventure.text.Component> lore = new java.util.ArrayList<>();
                    for (String line : loreLines) {
                        lore.add(net.kyori.adventure.text.Component.text(
                            org.bukkit.ChatColor.translateAlternateColorCodes('&', line)
                        ));
                    }
                    meta.lore(lore);
                }
                result.setItemMeta(meta);
            }

            // Shape
            List<String> shapeLines = recipeSec.getStringList("shape");
            if (shapeLines == null || shapeLines.isEmpty()) continue;

            NamespacedKey namespacedKey = new NamespacedKey(this, "custom_" + key.toLowerCase());
            ShapedRecipe recipe = new ShapedRecipe(namespacedKey, result);
            recipe.shape(shapeLines.toArray(new String[0]));

            // Ingredients
            org.bukkit.configuration.ConfigurationSection ingredientsSec = recipeSec.getConfigurationSection("ingredients");
            if (ingredientsSec == null) continue;

            boolean validIngredients = true;
            for (String chStr : ingredientsSec.getKeys(false)) {
                if (chStr.isEmpty()) continue;
                char ingredientChar = chStr.charAt(0);
                String ingMatStr = ingredientsSec.getString(chStr);
                if (ingMatStr == null) continue;

                Material ingMat = Material.getMaterial(ingMatStr.toUpperCase());
                if (ingMat == null) {
                    getLogger().warning("Invalid material for custom recipe ingredient: " + ingMatStr);
                    validIngredients = false;
                    break;
                }
                recipe.setIngredient(ingredientChar, ingMat);
            }

            if (validIngredients) {
                Bukkit.addRecipe(recipe);
                customRecipeKeys.add(namespacedKey);
                getLogger().info("Successfully registered custom recipe: " + key);
            }
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        lang = getConfig().getString("lang", "ru").toLowerCase();
        loadLangConfig();
        
        loadCustomRecipes();

        if (tabTask != null) {
            tabTask.cancel();
            tabTask = null;
        }
        if (getConfig().getBoolean("features.tab-formatter", true)) {
            startTabFormatter();
        }
    }
}

