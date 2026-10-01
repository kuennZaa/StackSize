package dev.stacksize;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class StackSize extends JavaPlugin implements Listener {

    private int max = 99;
    private boolean includeSmallStacks = true;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        max = Math.max(1, Math.min(99, getConfig().getInt("max-stack-size", 99)));
        includeSmallStacks = getConfig().getBoolean("include-16-stack-items", true);
        getServer().getPluginManager().registerEvents(this, this);

        // สแกนช่องเก็บของผู้เล่นทุก 1 วินาที เพื่อปรับของที่ได้มาจากทุกทาง (คราฟต์, /give, ฯลฯ)
        getServer().getScheduler().runTaskTimer(this, () -> {
            for (Player p : getServer().getOnlinePlayers()) {
                fixInventory(p.getInventory());
            }
        }, 20L, 20L);
    }

    /** ตั้ง max_stack_size ให้ไอเท็มที่สแตกได้อยู่แล้ว (ไม่ยุ่งกับของที่สแตกไม่ได้ เช่น ดาบ เกราะ) */
    private boolean fix(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        int def = stack.getType().getMaxStackSize();
        if (def <= 1) return false;
        if (def < 64 && !includeSmallStacks) return false;
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) return false;
        if (meta.hasMaxStackSize()) return false; // มีค่ากำหนดเองอยู่แล้ว (เช่นจากปลั๊กอินอื่น)
        meta.setMaxStackSize(max);
        stack.setItemMeta(meta);
        return true;
    }

    private void fixInventory(Inventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (fix(s)) inv.setItem(i, s);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSpawn(ItemSpawnEvent e) {
        Item item = e.getEntity();
        ItemStack s = item.getItemStack();
        if (fix(s)) item.setItemStack(s);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPickup(EntityPickupItemEvent e) {
        Item item = e.getItem();
        ItemStack s = item.getItemStack();
        if (fix(s)) item.setItemStack(s);
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        fixInventory(e.getInventory());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        fixInventory(e.getPlayer().getInventory());
    }
}
