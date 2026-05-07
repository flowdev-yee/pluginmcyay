package com.crimsonwarpedcraft.exampleplugin;

import io.papermc.lib.PaperLib;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

/**
 * Kinetic Enchantments prototype plugin.
 *
 * @author Copyright (c) Levi Muniz. All Rights Reserved.
 */
public class KineticEnchantments extends JavaPlugin implements Listener {

  private static final String RECOIL_BOOTS = "recoil boots";
  private static final String TETHER_BOW = "tether bow";
  private static final String WIND_SWEPT_BLADE = "wind-swept blade";
  private static final String BLINK_PEARL = "blink pearl";
  private static final long TETHER_WINDOW_MILLIS = 3_000L;
  private static final double RECOIL_RADIUS = 6.0D;
  private static final double RECOIL_VERTICAL_VELOCITY = 1.25D;
  private static final double WIND_SWEEP_HORIZONTAL_VELOCITY = 2.4D;
  private static final double TETHER_ENTITY_VELOCITY = 1.4D;

  private final Map<UUID, TetherAnchor> tetherAnchors = new HashMap<>();
  private final Set<UUID> blinkPearlUsers = new HashSet<>();
  private NamespacedKey blinkPearlKey;
  private NamespacedKey tetherArrowKey;

  @Override
  public void onEnable() {
    PaperLib.suggestPaper(this);

    saveDefaultConfig();
    blinkPearlKey = new NamespacedKey(this, "blink_pearl");
    tetherArrowKey = new NamespacedKey(this, "tether_arrow");
    getServer().getPluginManager().registerEvents(this, this);
  }

  /**
   * Converts sneaking fall damage into a Recoil Boots shockwave.
   *
   * @param event fall damage event
   */
  @EventHandler(ignoreCancelled = true)
  public void onFall(final EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player player)
        || event.getCause() != EntityDamageEvent.DamageCause.FALL
        || !player.isSneaking()
        || !hasNamedItem(player.getInventory().getBoots(), RECOIL_BOOTS)) {
      return;
    }

    event.setCancelled(true);
    player.setFallDistance(0.0F);
    Location center = player.getLocation();
    center.getWorld().spawnParticle(Particle.EXPLOSION, center, 1);
    center.getWorld().playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 1.4F);

    for (Entity nearbyEntity
        : player.getNearbyEntities(RECOIL_RADIUS, RECOIL_RADIUS, RECOIL_RADIUS)) {
      if (nearbyEntity instanceof LivingEntity && !nearbyEntity.equals(player)) {
        Vector velocity = nearbyEntity.getVelocity();
        velocity.setY(RECOIL_VERTICAL_VELOCITY);
        nearbyEntity.setVelocity(velocity);
      }
    }
  }

  /**
   * Marks arrows fired from a Tether Bow so impact handling can link anchors.
   *
   * @param event bow shoot event
   */
  @EventHandler(ignoreCancelled = true)
  public void onBowShot(final EntityShootBowEvent event) {
    if (!(event.getEntity() instanceof Player player)
        || !(event.getProjectile() instanceof AbstractArrow arrow)
        || !hasNamedItem(event.getBow(), TETHER_BOW)) {
      return;
    }

    arrow.getPersistentDataContainer().set(tetherArrowKey, PersistentDataType.STRING,
        player.getUniqueId().toString());
  }

  /**
   * Applies the Wind-Swept Blade's low-damage, high-knockback hit.
   *
   * @param event melee damage event
   */
  @EventHandler(ignoreCancelled = true)
  public void onWindSweptBladeHit(final EntityDamageByEntityEvent event) {
    if (!(event.getDamager() instanceof Player player)
        || !(event.getEntity() instanceof LivingEntity victim)
        || !hasNamedItem(player.getInventory().getItemInMainHand(), WIND_SWEPT_BLADE)) {
      return;
    }

    event.setDamage(Math.min(event.getDamage(), 1.0D));
    Vector direction = victim.getLocation().toVector().subtract(player.getLocation().toVector());
    direction.setY(0.0D);
    if (direction.lengthSquared() == 0.0D) {
      direction = player.getLocation().getDirection();
      direction.setY(0.0D);
    }

    victim.setVelocity(direction.normalize().multiply(WIND_SWEEP_HORIZONTAL_VELOCITY).setY(0.25D));
    victim.getWorld().spawnParticle(Particle.GUST, victim.getLocation().add(0.0D, 1.0D, 0.0D), 8);
    victim.getWorld().playSound(victim.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 0.8F, 1.3F);
  }

  /**
   * Throws a custom Blink Pearl projectile instead of a vanilla teleport pearl.
   *
   * @param event player interaction event
   */
  @EventHandler(ignoreCancelled = true)
  public void onBlinkPearlUse(final PlayerInteractEvent event) {
    if (event.getHand() != EquipmentSlot.HAND || !isRightClick(event.getAction())) {
      return;
    }

    ItemStack item = event.getItem();
    if (!hasNamedItem(item, BLINK_PEARL)) {
      return;
    }

    event.setCancelled(true);
    Player player = event.getPlayer();
    EnderPearl pearl = player.launchProjectile(EnderPearl.class);
    pearl.getPersistentDataContainer().set(blinkPearlKey, PersistentDataType.STRING,
        player.getUniqueId().toString());
    blinkPearlUsers.add(player.getUniqueId());
    consumeOnePearl(player, item);
  }

  /**
   * Cancels the vanilla ender pearl teleport caused by custom Blink Pearls.
   *
   * @param event player teleport event
   */
  @EventHandler(ignoreCancelled = true)
  public void onBlinkPearlTeleport(final PlayerTeleportEvent event) {
    if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL
        && blinkPearlUsers.remove(event.getPlayer().getUniqueId())) {
      event.setCancelled(true);
    }
  }

  /**
   * Dispatches custom projectile impacts for Blink Pearls and Tether Bow arrows.
   *
   * @param event projectile impact event
   */
  @EventHandler(ignoreCancelled = true)
  public void onProjectileHit(final ProjectileHitEvent event) {
    PersistentDataContainer container = event.getEntity().getPersistentDataContainer();
    if (container.has(blinkPearlKey, PersistentDataType.STRING)) {
      handleBlinkPearlHit(event, container.get(blinkPearlKey, PersistentDataType.STRING));
      return;
    }

    if (container.has(tetherArrowKey, PersistentDataType.STRING)) {
      handleTetherArrowHit(event, container.get(tetherArrowKey, PersistentDataType.STRING));
    }
  }

  private void handleBlinkPearlHit(final ProjectileHitEvent event, final String playerId) {
    final Entity hitEntity = event.getHitEntity();
    Player player = getServer().getPlayer(UUID.fromString(playerId));
    if (player == null) {
      return;
    }

    event.getEntity().remove();
    getServer().getScheduler().runTask(this, () -> blinkPearlUsers.remove(player.getUniqueId()));
    if (hitEntity == null || hitEntity.equals(player)) {
      return;
    }

    Location playerLocation = player.getLocation();
    Location hitLocation = hitEntity.getLocation();
    player.teleport(hitLocation);
    hitEntity.teleport(playerLocation);
    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 1.0F);
    hitEntity.getWorld().playSound(hitEntity.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT,
        1.0F, 1.0F);
  }

  private void handleTetherArrowHit(final ProjectileHitEvent event, final String playerId) {
    UUID playerUuid = UUID.fromString(playerId);
    TetherAnchor currentAnchor = TetherAnchor.from(event);
    if (currentAnchor == null) {
      return;
    }

    long now = System.currentTimeMillis();
    TetherAnchor firstAnchor = tetherAnchors.get(playerUuid);
    if (firstAnchor == null || now - firstAnchor.createdAtMillis() > TETHER_WINDOW_MILLIS) {
      tetherAnchors.put(playerUuid, currentAnchor.withCreatedAt(now));
      currentAnchor.playPrimeEffects();
      return;
    }

    tetherAnchors.remove(playerUuid);
    pullTogether(firstAnchor, currentAnchor);
  }

  private void pullTogether(final TetherAnchor firstAnchor, final TetherAnchor secondAnchor) {
    Location firstLocation = firstAnchor.location();
    Location secondLocation = secondAnchor.location();
    Location midpoint = firstLocation.clone().add(secondLocation).multiply(0.5D);

    firstAnchor.pullToward(midpoint);
    secondAnchor.pullToward(midpoint);
    firstLocation.getWorld().spawnParticle(Particle.ENCHANT, firstLocation, 16);
    secondLocation.getWorld().spawnParticle(Particle.ENCHANT, secondLocation, 16);
    midpoint.getWorld().playSound(midpoint, Sound.BLOCK_CHAIN_PLACE, 1.0F, 1.6F);
  }

  private void consumeOnePearl(final Player player, final ItemStack item) {
    if (player.getGameMode() == GameMode.CREATIVE) {
      return;
    }

    if (item.getAmount() <= 1) {
      player.getInventory().setItemInMainHand(null);
    } else {
      item.setAmount(item.getAmount() - 1);
    }
  }

  private boolean isRightClick(final Action action) {
    return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
  }

  private boolean hasNamedItem(final ItemStack item, final String expectedName) {
    if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
      return false;
    }

    ItemMeta meta = item.getItemMeta();
    return meta.hasDisplayName()
        && meta.getDisplayName().toLowerCase(Locale.ROOT).contains(expectedName);
  }

  private record TetherAnchor(Entity entity, Location blockLocation, long createdAtMillis) {

    private static TetherAnchor from(final ProjectileHitEvent event) {
      if (event.getHitEntity() != null) {
        return new TetherAnchor(event.getHitEntity(), null, System.currentTimeMillis());
      }

      Block hitBlock = event.getHitBlock();
      if (hitBlock != null) {
        return new TetherAnchor(null, hitBlock.getLocation().add(0.5D, 0.5D, 0.5D),
            System.currentTimeMillis());
      }

      return null;
    }

    private TetherAnchor withCreatedAt(final long createdAtMillis) {
      return new TetherAnchor(entity, blockLocation, createdAtMillis);
    }

    private Location location() {
      if (entity != null) {
        return entity.getLocation();
      }
      return blockLocation.clone();
    }

    private void pullToward(final Location target) {
      if (entity == null) {
        return;
      }

      Vector pull = target.toVector().subtract(entity.getLocation().toVector());
      if (pull.lengthSquared() == 0.0D) {
        return;
      }

      entity.setVelocity(pull.normalize().multiply(TETHER_ENTITY_VELOCITY).setY(0.35D));
    }

    private void playPrimeEffects() {
      Location location = location();
      location.getWorld().spawnParticle(Particle.ENCHANT, location, 12);
      location.getWorld().playSound(location, Sound.BLOCK_CHAIN_HIT, 0.7F, 1.8F);
    }
  }
}
