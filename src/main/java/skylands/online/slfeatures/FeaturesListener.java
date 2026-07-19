package skylands.online.slfeatures;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.CartographyInventory;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FeaturesListener implements Listener {

    private final SLfeatures plugin;
    private final NamespacedKey ownerKey;
    private final Map<UUID, Long> lastKnock = new HashMap<>();

    public FeaturesListener(SLfeatures plugin) {
        this.plugin = plugin;
        this.ownerKey = new NamespacedKey(plugin, "lock_owner");
    }

    private String getMsg(String key) {
        return plugin.getLangConfig().getString(key, "");
    }

    // 1. Ограничение элитр в Энде
    @EventHandler
    public void onGlideToggle(EntityToggleGlideEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
                if (event.isGliding()) {
                    event.setCancelled(true);
                    player.setGliding(false);
                    player.sendActionBar(Component.text(getMsg("elytra_end_block"), NamedTextColor.RED));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (player.isGliding() && player.getWorld().getEnvironment() == World.Environment.THE_END) {
            player.setGliding(false);
            player.sendActionBar(Component.text(getMsg("elytra_end_block"), NamedTextColor.RED));
        }
    }

    // 2. Ремонт наковальни железным блоком
    @EventHandler
    public void onAnvilRepair(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        org.bukkit.block.Block block = event.getClickedBlock();
        if (block == null) return;

        Material type = block.getType();
        if (type != Material.CHIPPED_ANVIL && type != Material.DAMAGED_ANVIL) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack handItem = player.getInventory().getItemInMainHand();
        if (handItem.getType() != Material.IRON_BLOCK) return;

        // Предотвращаем открытие UI наковальни
        event.setCancelled(true);

        Material newType = (type == Material.CHIPPED_ANVIL) ? Material.ANVIL : Material.CHIPPED_ANVIL;
        block.setType(newType);

        handItem.setAmount(handItem.getAmount() - 1);

        block.getWorld().playSound(block.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.0f, 1.0f);
        block.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, block.getLocation().add(0.5, 1.0, 0.5), 10, 0.3, 0.3, 0.3, 0.0);

        player.sendMessage(Component.text(getMsg("anvil_repaired"), NamedTextColor.GREEN));
    }

    // 3. Тук в двери
    @EventHandler
    public void onDoorKnock(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        org.bukkit.block.Block block = event.getClickedBlock();
        if (block == null) return;

        String typeName = block.getType().name();
        if (!typeName.endsWith("_DOOR")) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        long now = System.currentTimeMillis();
        long last = lastKnock.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 3000) {
            event.setCancelled(true);
            return;
        }
        lastKnock.put(player.getUniqueId(), now);

        event.setCancelled(true);

        block.getWorld().playSound(block.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.0f, 1.0f);

        double radiusSquared = 15 * 15;
        Component msg = Component.text(getMsg("knock_knock"), NamedTextColor.YELLOW);

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getWorld().equals(block.getWorld()) && online.getLocation().distanceSquared(block.getLocation()) <= radiusSquared) {
                online.sendMessage(Component.text()
                        .append(Component.text(player.getName(), NamedTextColor.GREEN))
                        .append(Component.text(getMsg("knocked_door"), NamedTextColor.GRAY))
                        .append(msg)
                        .build());
            }
        }
    }

    // 4. Эндермены не могут брать блоки
    @EventHandler
    public void onEndermanPickup(EntityChangeBlockEvent event) {
        if (event.getEntityType() == org.bukkit.entity.EntityType.ENDERMAN) {
            event.setCancelled(true);
        }
    }

    // 5. Подпись предметов пером (Shift + ПКМ)
    @EventHandler
    public void onItemSign(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;

        ItemStack mainHand = player.getInventory().getItemInMainHand();
        ItemStack offHand = player.getInventory().getItemInOffHand();

        if (mainHand.getType() == Material.FEATHER) {
            if (offHand.getType() == Material.AIR || offHand.getType() == Material.FEATHER) return;

            event.setCancelled(true);

            ItemMeta meta = offHand.getItemMeta();
            if (meta == null) return;
            List<Component> lore = meta.hasLore() ? meta.lore() : new ArrayList<>();
            if (lore == null) lore = new ArrayList<>();

            boolean alreadySigned = false;
            String signCheck = getMsg("signed_by").replace(" ", "").replace(":", "");
            for (Component line : lore) {
                String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(line);
                if (plain.replace(" ", "").replace(":", "").contains(signCheck)) {
                    alreadySigned = true;
                    break;
                }
            }

            if (alreadySigned) {
                player.sendMessage(Component.text(getMsg("item_signed_already"), NamedTextColor.RED));
                return;
            }

            lore.add(Component.text(getMsg("signed_by"), NamedTextColor.WHITE)
                    .append(Component.text(player.getName(), NamedTextColor.AQUA)));
            meta.lore(lore);
            offHand.setItemMeta(meta);

            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_VILLAGER_WORK_CARTOGRAPHER, 1.0f, 1.2f);
            player.sendMessage(Component.text(getMsg("item_signed_success"), NamedTextColor.GREEN));
        }
    }

    // 6. Защита заблокированных предметов от копирования (Crafting Table)
    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        CraftingInventory inventory = event.getInventory();
        ItemStack[] matrix = inventory.getMatrix();

        ItemStack lockedSource = null;
        String lockOwner = null;

        for (ItemStack item : matrix) {
            if (item == null || item.getType() == Material.AIR) continue;

            if (item.hasItemMeta()) {
                PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
                if (pdc.has(ownerKey, PersistentDataType.STRING)) {
                    lockOwner = pdc.get(ownerKey, PersistentDataType.STRING);
                    lockedSource = item;
                    break;
                }
            }
        }

        if (lockedSource != null) {
            if (!event.getViewers().isEmpty() && event.getViewers().get(0) instanceof Player player) {
                String playerUuid = player.getUniqueId().toString();
                if (!playerUuid.equals(lockOwner)) {
                    inventory.setResult(null);
                }
            } else {
                inventory.setResult(null);
            }
        }
    }

    // 7. Защита заблокированных предметов от копирования (Cartography Table)
    @EventHandler
    public void onCartographyClick(InventoryClickEvent event) {
        Inventory inventory = event.getInventory();
        if (inventory instanceof CartographyInventory) {
            if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.RESULT) {
                ItemStack map = inventory.getItem(0);
                if (map != null && map.hasItemMeta()) {
                    PersistentDataContainer pdc = map.getItemMeta().getPersistentDataContainer();
                    if (pdc.has(ownerKey, PersistentDataType.STRING)) {
                        String lockOwner = pdc.get(ownerKey, PersistentDataType.STRING);
                        if (event.getWhoClicked() instanceof Player player) {
                            String playerUuid = player.getUniqueId().toString();
                            if (!playerUuid.equals(lockOwner)) {
                                event.setCancelled(true);
                                player.sendMessage(Component.text(getMsg("art_copy_deny"), NamedTextColor.RED));
                            }
                        } else {
                            event.setCancelled(true);
                        }
                    }
                }
            }
        }
    }
}
