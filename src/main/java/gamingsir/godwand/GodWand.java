package gamingsir.godwand;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

import java.util.*;

public class GodWand extends JavaPlugin implements Listener {

    private NamespacedKey wandKey;
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    @Override
    public void onEnable() {
        wandKey = new NamespacedKey(this, "god_wand");
        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("God Wand chalu ho gaya!");
    }

    public ItemStack createWand() {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("GOD WAND", NamedTextColor.GOLD, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Right Click: Lightning Strike", NamedTextColor.YELLOW),
                Component.text("Shift + Right Click: Divine Blessing", NamedTextColor.GREEN),
                Component.text("F: Wind Burst", NamedTextColor.AQUA),
                Component.text("Q: Freeze Zone", NamedTextColor.BLUE),
                Component.text("Shift + F: JUDGEMENT DAY", NamedTextColor.DARK_RED, TextDecoration.BOLD)
        ));
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
        meta.setCustomModelData(7777);
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(wandKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    private boolean isWand(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(wandKey, PersistentDataType.BYTE);
    }

    private boolean onCooldown(Player p, String ability, int seconds) {
        long now = System.currentTimeMillis();
        Map<String, Long> map = cooldowns.computeIfAbsent(p.getUniqueId(), k -> new HashMap<>());
        long last = map.getOrDefault(ability, 0L);
        if (now - last < seconds * 1000L) {
            long left = (seconds * 1000L - (now - last)) / 1000 + 1;
            p.sendActionBar(Component.text(ability + " cooldown: " + left + "s", NamedTextColor.RED));
            return true;
        }
        map.put(ability, now);
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("godwand.admin") && !sender.isOp()) {
            sender.sendMessage("Permission nahi hai.");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("Use: /godwand give <player> ya /godwand random");
            return true;
        }
        if (args[0].equalsIgnoreCase("give") && args.length >= 2) {
            Player t = Bukkit.getPlayer(args[1]);
            if (t == null) { sender.sendMessage("Player nahi mila"); return true; }
            t.getInventory().addItem(createWand());
            sender.sendMessage("God Wand de di " + t.getName() + " ko");
            return true;
        }
        if (args[0].equalsIgnoreCase("random")) {
            List<Player> list = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (list.isEmpty()) { sender.sendMessage("Koi online nahi"); return true; }
            Player t = list.get(new Random().nextInt(list.size()));
            t.getInventory().addItem(createWand());
            Bukkit.broadcast(Component.text(t.getName() + " ko GOD WAND mil gayi!", NamedTextColor.GOLD, TextDecoration.BOLD));
            t.getWorld().strikeLightningEffect(t.getLocation());
            return true;
        }
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (e.getHand() != org.bukkit.inventory.EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!isWand(p.getInventory().getItemInMainHand())) return;
        e.setCancelled(true);

        if (p.isSneaking()) {
            if (onCooldown(p, "Blessing", 15)) return;
            p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 20 * 10, 2));
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 10, 3));
            p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20 * 10, 1));
            p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 10, 2));
            p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, p.getLocation().add(0, 1, 0), 80, 0.5, 1, 0.5, 0.3);
            p.getWorld().playSound(p.getLocation(), Sound.ITEM_TOTEM_USE, 1f, 1f);
        } else {
            if (onCooldown(p, "Lightning", 3)) return;
            Location target = p.getTargetBlockExact(60) != null
                    ? p.getTargetBlockExact(60).getLocation().add(0.5, 1, 0.5)
                    : p.getEyeLocation().add(p.getEyeLocation().getDirection().multiply(30));
            for (int i = 0; i < 3; i++) target.getWorld().strikeLightning(target);
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent e) {
        Player p = e.getPlayer();
        if (!isWand(e.getOffHandItem()) && !isWand(e.getMainHandItem())) return;
        e.setCancelled(true);

        if (p.isSneaking()) {
            if (onCooldown(p, "Judgement", 45)) return;
            Location c = p.getLocation();
            c.getWorld().playSound(c, Sound.ENTITY_WITHER_SPAWN, 2f, 0.6f);
            Bukkit.broadcast(Component.text(p.getName() + " ne JUDGEMENT DAY chala diya!", NamedTextColor.DARK_RED, TextDecoration.BOLD));
            for (Entity en : p.getNearbyEntities(25, 25, 25)) {
                if (en instanceof LivingEntity le && !(en.equals(p))) {
                    le.getWorld().strikeLightningEffect(le.getLocation());
                    le.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, le.getLocation(), 1);
                    le.setHealth(0);
                }
            }
        } else {
            if (onCooldown(p, "WindBurst", 6)) return;
            Location c = p.getLocation();
            c.getWorld().spawnParticle(Particle.GUST_EMITTER_LARGE, c, 1);
            c.getWorld().playSound(c, Sound.ENTITY_WIND_CHARGE_WIND_BURST, 2f, 1f);
            for (Entity en : p.getNearbyEntities(8, 8, 8)) {
                if (en instanceof LivingEntity le) {
                    Vector v = le.getLocation().toVector().subtract(c.toVector()).normalize().multiply(2.5).setY(1.4);
                    le.setVelocity(v);
                }
            }
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent e) {
        Player p = e.getPlayer();
        if (!isWand(e.getItemDrop().getItemStack())) return;
        e.setCancelled(true);
        if (onCooldown(p, "Freeze", 12)) return;
        Location c = p.getLocation();
        c.getWorld().playSound(c, Sound.BLOCK_GLASS_BREAK, 2f, 0.5f);
        c.getWorld().spawnParticle(Particle.SNOWFLAKE, c.add(0, 1, 0), 200, 4, 1, 4, 0.05);
        for (Entity en : p.getNearbyEntities(10, 10, 10)) {
            if (en instanceof LivingEntity le) {
                le.setFreezeTicks(le.getMaxFreezeTicks() + 100);
                le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 20 * 6, 6));
                le.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 20 * 6, 3));
            }
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!isWand(p.getInventory().getItemInMainHand())) return;
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        e.setDamage(10000);
        victim.getWorld().strikeLightningEffect(victim.getLocation());
    }

    @EventHandler
    public void onDeath(org.bukkit.event.entity.PlayerDeathEvent e) {
        e.getDrops().removeIf(this::isWand);
    }
}
