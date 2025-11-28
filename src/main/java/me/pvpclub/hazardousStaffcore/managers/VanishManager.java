package me.pvpclub.hazardousStaffcore.managers;

import me.pvpclub.hazardousStaffcore.HazardousStaffcore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VanishManager {

    private final HazardousStaffcore plugin;
    // Map storing vanished players and their vanish level. only initialized once so it can be final
    private final Map<UUID, Integer> vanishedPlayers;

    public VanishManager(HazardousStaffcore plugin) {
        this.plugin = plugin;
        this.vanishedPlayers = new HashMap<>();
    }

    public void toggleVanish(Player player) {
        toggleVanish(player, 1);
    }

    public void toggleVanish(Player player, int level) {
        UUID uuid = player.getUniqueId();

        if (vanishedPlayers.containsKey(uuid)) {
            unvanish(player);
        } else {
            vanish(player, level);
        }
    }

    public void vanish(Player player, int level) {
        UUID uuid = player.getUniqueId();
        vanishedPlayers.put(uuid, level);

        // Apply permanent Night Vision while vanished
        applyNightVision(player);

        updateVanishState(player);
        player.sendMessage("§aYou are now vanished at level " + level + ".");
    }

    public void unvanish(Player player) {
        UUID uuid = player.getUniqueId();
        vanishedPlayers.remove(uuid);

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.showPlayer(plugin, player);
        }

        // Remove Night Vision when no longer vanished
        removeNightVision(player);

        player.sendMessage("§cYou are no longer vanished.");
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.containsKey(player.getUniqueId());
    }

    public int getVanishLevel(Player player) {
        return vanishedPlayers.getOrDefault(player.getUniqueId(), 0);
    }

    public void updateVanishState(Player vanished) {
        int level = vanishedPlayers.get(vanished.getUniqueId());

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(vanished)) continue;

            if (canSeeVanished(online, level)) {
                online.showPlayer(plugin, vanished);
            } else {
                online.hidePlayer(plugin, vanished);
            }
        }
    }

    public boolean canSeeVanished(Player viewer, int vanishLevel) {
        int maxLevel = plugin.getConfig().getInt("vanish.max-level", 5);

        for (int i = vanishLevel; i <= maxLevel; i++) {
            if (viewer.hasPermission("staffcore.vanish.see.level" + i)) {
                return true;
            }
        }

        return false;
    }

    public void handleJoin(Player player) {
        // If the joining player is vanished, ensure Night Vision is active
        if (isVanished(player)) {
            applyNightVision(player);
        }

        for (UUID uuid : vanishedPlayers.keySet()) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished != null) {
                int level = vanishedPlayers.get(uuid);
                if (!canSeeVanished(player, level)) {
                    player.hidePlayer(plugin, vanished);
                }
            }
        }
    }

    /**
     * Apply infinite night vision effect to the player while vanished.
     */
    private void applyNightVision(Player player) {
        PotionEffect effect = new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, true, false, false);
        player.addPotionEffect(effect);
    }

    /**
     * Remove the Night Vision effect if present.
     */
    private void removeNightVision(Player player) {
        player.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }
}