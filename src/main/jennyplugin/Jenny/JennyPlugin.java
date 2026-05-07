package com.yourname.jenny;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class JennyAdultAction extends JavaPlugin implements Listener {
    
    private ArmorStand jenny;
    private Location spawnLoc;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final UUID jennyUUID = UUID.fromString("1218acb4-2bc0-4368-b219-58e6bae64320");
    
    // Your exact skin data
    private static final String SKIN_VALUE = "ewogICJ0aW1lc3RhbXAiIDogMTc3NzExNzk1Nzk3NSwKICAicHJvZmlsZUlkIiA6ICIxMjE4YWNiNDJiYzA0MzY4YjIxOTU4ZTZiYWU2NDMyMCIsCiAgInByb2ZpbGVOYW1lIiA6ICJQYXRhdGplTUMiLAogICJzaWduYXR1cmVSZXF1aXJlZCIgOiB0cnVlLAogICJ0ZXh0dXJlcyIgOiB7CiAgICAiU0tJTiIgOiB7CiAgICAgICJ1cmwiIDogImh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOTk1N2E0MDU1Y2EzMjMzNTAxZTA4MjRmYTgyYTU5NTZjZGIyZWU3MGFhYWM2ODA5NmQwMjVkZmE3ZjEzZDNkMyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9CiAgfQp9";
    private static final String SKIN_SIGNATURE = "gGpTOwPx5g0I4rsgec/qii6L/I//0adHXwJ3WQnxt/tH6Csj0SM0ySlOtqhvYw6Ll8i9xflfPo39r4gJ0nLKwor8Yv0dtWQ7kXt6lwi5BYRs2Ny5j/1T/FYP8zcCFXaXvdFNzlNwTJVkw20//xtbddHzblTo6WYmQhyJbmj5Ar6FKMXBnkvYNCzFTyg+mKQ3fE9cDG6dbdmimmhCbptnURIM1Gbr9vbjZp+kK+6W78bW8frGvpGwbe9bYec73RodxPwbpbkciI/7B3CgnFlyNEd16Kor2YnmX/fJEdGspJmWazg81gsZ2A0bEyDb7YSvcs+vazXkxmRtICPpdPEmSunM/aVXuBmnHuwcZdQKQpYejIbFOi1pARrpT2BfKJd7CSBMzfT9W41fH9jRTUWARfJ4YvvD4JGNtWwvmsXnDN3/H77JrlWX/0xxG3tvSuyObqEhKxgLvcSDwrQiRiTgWFo3I1daTSpxUa27cw71hDBw26kxsz63CqPqJofEqTTDOKJs7y1GKaRVLGxOkTQ3uJaCmo64q6jhVse+EWpLjbIoMCEkMSuGDz56DSP+NL8WP6vsIps0YEhoHnXvR9I9fLkCMqxfR6G/RQWHRp8Qn3s1ncIbwrC2J3ER8shSepKHPrtSImULcWSkOdyaVtXhPf4I6QENkoakknvZXTrWxLs=";
    
    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("jenny").setExecutor(this);
        
        // Load or spawn Jenny
        World world = Bukkit.getWorlds().get(0);
        spawnLoc = new Location(world, 0, 64, 0);
        spawnJenny(spawnLoc);
        
        getLogger().info("JennyAdultAction enabled — Emerald = special action");
    }
    
    @Override
    public void onDisable() {
        if (jenny != null) jenny.remove();
    }
    
    private void spawnJenny(Location loc) {
        if (jenny != null) jenny.remove();
        
        World world = loc.getWorld();
        jenny = world.spawn(loc, ArmorStand.class);
        jenny.setVisible(true);
        jenny.setGravity(false);
        jenny.setBasePlate(false);
        jenny.setArms(true);
        jenny.setSmall(false);
        jenny.setMarker(false);
        jenny.setInvulnerable(true);
        jenny.setCanPickupItems(false);
        
        // Apply custom skin via Player Head on top
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        applySkinToSkull(meta);
        skull.setItemMeta(meta);
        jenny.getEquipment().setHelmet(skull);
        
        // NO extra clothes — skin already has them
        jenny.getEquipment().setChestplate(null);
        jenny.getEquipment().setLeggings(null);
        jenny.getEquipment().setBoots(null);
        
        jenny.setCustomNameVisible(true);
        jenny.setCustomName(ChatColor.LIGHT_PURPLE + "Jenny " + ChatColor.GRAY + "[Right-click]");
        
        getLogger().info("Jenny spawned with your custom skin at " + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    }
    
    private void applySkinToSkull(SkullMeta meta) {
        try {
            GameProfile profile = new GameProfile(jennyUUID, "Jenny");
            profile.getProperties().put("textures", new Property("textures", SKIN_VALUE, SKIN_SIGNATURE));
            
            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);
        } catch (Exception e) {
            getLogger().warning("Failed to apply skin: " + e.getMessage());
        }
    }
    
    @EventHandler
    public void onRightClick(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand)) return;
        if (!event.getRightClicked().equals(jenny)) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        // Check for emerald
        if (item.getType() != Material.EMERALD) {
            player.sendMessage(ChatColor.RED + "Jenny: " + ChatColor.GRAY + "Give me an emerald if you want... attention.");
            return;
        }
        
        // Cooldown check (30 seconds)
        if (cooldowns.containsKey(player.getUniqueId()) && 
            System.currentTimeMillis() - cooldowns.get(player.getUniqueId()) < 30000) {
            long remaining = (30000 - (System.currentTimeMillis() - cooldowns.get(player.getUniqueId()))) / 1000;
            player.sendMessage(ChatColor.RED + "Jenny needs a rest... " + remaining + " seconds.");
            return;
        }
        
        // Consume the emerald
        item.setAmount(item.getAmount() - 1);
        cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
        
        // Execute the adult action sequence
        performAdultAction(player);
    }
    
    private void performAdultAction(Player player) {
        // Store original locations
        Location playerLoc = player.getLocation().clone();
        Location jennyLoc = jenny.getLocation().clone();
        
        // Make them face each other
        player.teleport(playerLoc);
        jenny.teleport(jennyLoc);
        
        // Phase 1: Approach (move Jenny 0.5 blocks closer)
        Vector direction = playerLoc.toVector().subtract(jennyLoc.toVector()).normalize();
        Location closerLoc = jennyLoc.clone().add(direction.multiply(0.5));
        jenny.teleport(closerLoc);
        
        // Phase 2: The "action" simulation (rapid micro-movements + particles)
        new BukkitRunnable() {
            int tick = 0;
            final int duration = 40; // 2 seconds of motion
            
            @Override
            public void run() {
                if (tick >= duration || jenny == null || jenny.isDead() || !player.isOnline()) {
                    // Final pose and message
                    jenny.setCustomName(ChatColor.LIGHT_PURPLE + "Jenny " + ChatColor.GRAY + "[Satisfied]");
                    player.sendTitle(ChatColor.DARK_PURPLE + "❤ ❤ ❤", ChatColor.LIGHT_PURPLE + "Jenny smiles warmly...", 10, 40, 10);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.5f);
                    
                    // Reset name after 5 seconds
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (jenny != null && !jenny.isDead()) {
                                jenny.setCustomName(ChatColor.LIGHT_PURPLE + "Jenny " + ChatColor.GRAY + "[Right-click]");
                            }
                        }
                    }.runTaskLater(JennyAdultAction.this, 100L);
                    
                    cancel();
                    return;
                }
                
                // Simulate rhythmic movement: Jenny bobs up/down and shakes
                double heightOffset = Math.sin(tick * 0.8) * 0.08;
                Location newLoc = closerLoc.clone().add(0, heightOffset, 0);
                
                // Add horizontal wiggle
                double wiggleX = Math.sin(tick * 1.5) * 0.05;
                double wiggleZ = Math.cos(tick * 1.5) * 0.05;
                newLoc.add(wiggleX, 0, wiggleZ);
                
                jenny.teleport(newLoc);
                
                // Spawn suggestive particles
                // Hearts (intimacy)
                player.getWorld().spawnParticle(Particle.HEART, 
                    jenny.getLocation().add(0, 0.8, 0), 3, 0.3, 0.3, 0.3, 0);
                
                // Smoke/Clouds (motion)
                player.getWorld().spawnParticle(Particle.CLOUD, 
                    jenny.getLocation().add(0, 0.5, 0), 5, 0.2, 0.1, 0.2, 0.01);
                
                // Critical hit particles (retextured to look like... activity)
                player.getWorld().spawnParticle(Particle.CRIT, 
                    jenny.getLocation().add(0, 0.6, 0), 8, 0.2, 0.1, 0.2, 0);
                
                // Sound effects at specific ticks
                if (tick == 5) {
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.6f, 1.2f);
                }
                if (tick == 15) {
                    player.playSound(player.getLocation(), Sound.BLOCK_SLIME_BLOCK_SQUISH, 0.7f, 1.1f);
                }
                if (tick == 25) {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 0.5f, 1.3f);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_BURP, 0.4f, 1.0f);
                }
                if (tick == 35) {
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.8f);
                }
                
                // Player also gets a slight "reaction" shake
                Location playerShake = player.getLocation();
                playerShake.add(Math.sin(tick * 2) * 0.03, 0, Math.cos(tick * 2) * 0.03);
                player.teleport(playerShake);
                
                tick++;
            }
        }.runTaskTimer(this, 0L, 1L); // Run every tick (20x per second)
        
        // Phase 3: After-action glow and message
        new BukkitRunnable() {
            @Override
            public void run() {
                if (jenny != null && !jenny.isDead()) {
                    // Glow effect using name color
                    jenny.setCustomName(ChatColor.LIGHT_PURPLE + "✨ Jenny ✨");
                    
                    // Final particle burst
                    player.getWorld().spawnParticle(Particle.HEART, jenny.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
                    player.getWorld().spawnParticle(Particle.FIREWORKS_SPARK, jenny.getLocation().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0);
                    
                    player.sendMessage(ChatColor.LIGHT_PURPLE + "Jenny: " + ChatColor.GRAY + "That was... wonderful, " + player.getName() + ". ❤");
                    player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.8f, 1.4f);
                    
                    // Give a small reward (1 heart healing)
                    if (player.getHealth() < player.getMaxHealth()) {
                        player.setHealth(Math.min(player.getMaxHealth(), player.getHealth() + 2));
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "You feel rejuvenated...");
                    }
                }
            }
        }.runTaskLater(this, 50L); // After 2.5 seconds
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        
        Player player = (Player) sender;
        
        if (args.length == 0) {
            player.sendMessage(ChatColor.GOLD + "=== Jenny Commands ===");
            player.sendMessage(ChatColor.YELLOW + "/jenny spawn" + ChatColor.GRAY + " - Spawn Jenny at your location");
            player.sendMessage(ChatColor.YELLOW + "/jenny remove" + ChatColor.GRAY + " - Remove Jenny");
            player.sendMessage(ChatColor.YELLOW + "/jenny here" + ChatColor.GRAY + " - Teleport Jenny to you");
            player.sendMessage(ChatColor.YELLOW + "/jenny follow" + ChatColor.GRAY + " - Make Jenny follow you");
            player.sendMessage(ChatColor.YELLOW + "/jenny stop" + ChatColor.GRAY + " - Stop following");
            return true;
        }
        
        switch (args[0].toLowerCase()) {
            case "spawn":
                spawnJenny(player.getLocation());
                player.sendMessage(ChatColor.GREEN + "Jenny spawned at your location.");
                break;
            case "remove":
                if (jenny != null) jenny.remove();
                jenny = null;
                player.sendMessage(ChatColor.RED + "Jenny removed.");
                break;
            case "here":
                if (jenny == null) {
                    player.sendMessage(ChatColor.RED + "Jenny isn't spawned yet. Use /jenny spawn");
                } else {
                    jenny.teleport(player.getLocation());
                    player.sendMessage(ChatColor.GREEN + "Jenny teleported to you.");
                }
                break;
            case "follow":
                if (jenny == null) {
                    player.sendMessage(ChatColor.RED + "Jenny isn't spawned yet.");
                } else {
                    startFollowing(player);
                    player.sendMessage(ChatColor.GREEN + "Jenny is now following you.");
                }
                break;
            case "stop":
                stopFollowing();
                player.sendMessage(ChatColor.YELLOW + "Jenny stopped following.");
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown command. Use /jenny for help.");
        }
        
        return true;
    }
    
    // Simple following movement (the "other" kind of movement)
    private BukkitRunnable followTask = null;
    
    private void startFollowing(Player player) {
        if (followTask != null) followTask.cancel();
        
        followTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (jenny == null || jenny.isDead() || !player.isOnline()) {
                    cancel();
                    return;
                }
                
                Location targetLoc = player.getLocation();
                Location jennyLoc = jenny.getLocation();
                
                double distance = jennyLoc.distance(targetLoc);
                if (distance > 2.5) {
                    // Move Jenny toward player
                    Vector direction = targetLoc.toVector().subtract(jennyLoc.toVector()).normalize();
                    Location newLoc = jennyLoc.clone().add(direction.multiply(Math.min(0.3, distance - 2)));
                    newLoc.setYaw(targetLoc.getYaw());
                    jenny.teleport(newLoc);
                    
                    // Make her look at player
                    jenny.setRotation(targetLoc.getYaw(), 0);
                }
            }
        };
        followTask.runTaskTimer(this, 0L, 5L);
    }
    
    private void stopFollowing() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
    }
}
