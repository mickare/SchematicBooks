package de.mickare.schematicbooks;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BlockIterator;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.session.ClipboardHolder;

import de.mickare.schematicbooks.data.DataStoreException;
import de.mickare.schematicbooks.data.SchematicEntity;
import de.mickare.schematicbooks.util.IntRegion;
import de.mickare.schematicbooks.util.IntVector;
import de.mickare.schematicbooks.util.LocationBlockIterator;
import de.mickare.schematicbooks.util.ParticleUtils;
import de.mickare.schematicbooks.util.Rotation;
import de.mickare.schematicbooks.we.EntityEditSession;
import de.mickare.schematicbooks.we.WEUtils;
import de.mickare.schematicbooks.we.WEUtils.EnhancedBaseItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;

public class Interactions {

  // Instance
  private static @Getter @Setter(AccessLevel.PROTECTED) SchematicBooksPlugin plugin;

  // Variables
  private static final int MAX_BLOCKS = 1000;
  private static final long DEFAULT_TIMEOUT = 1000;
  private static final int DEFAULT_SIGHT_DISTANCE = 5;

  private static final Set<EnhancedBaseItem> IGNORED_MATERIALS = buildIgnoredMaterials();

  @SuppressWarnings("deprecation")
  private static Set<EnhancedBaseItem> buildIgnoredMaterials() {
    ImmutableSet.Builder<EnhancedBaseItem> b = ImmutableSet.builder();
    for (short i = 0; i < 16; ++i) {
      b.add(new EnhancedBaseItem(Material.BARRIER.getId(), i));
    }
    return b.build();
  }

  // Timeouts

  // Helper for streams
  private static final Predicate<? super Entry<EnhancedBaseItem, Integer>> MATERIALS_FILTER =
      e -> (!IGNORED_MATERIALS.contains(e.getKey()) && e.getValue() > 0);
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

  // **************************************************
  // Timeouts
  private static WeakHashMap<Player, Long> playerTimeouts = new WeakHashMap<>();

  public static boolean hasTimeout(final Player player) {
    return playerTimeouts.getOrDefault(player, 0l) > System.currentTimeMillis();
  }

  public static void setTimeout(final Player player, long timeout) {
    playerTimeouts.put(player, System.currentTimeMillis() + timeout);
  }

  public static void setTimeout(final Player player) {
    setTimeout(player, DEFAULT_TIMEOUT);
  }

  // **************************************************
  // Info

  private static boolean checkShowInfo(final Cancellable event, final Player player) {
    Preconditions.checkNotNull(event);
    Preconditions.checkNotNull(player);
    if (player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SPECTATOR) {
      return false;
    }
    if (!player.isSneaking()) {
      return false;
    }

    final ItemStack item = player.getInventory().getItemInMainHand();
    if (item == null || item.getType() != Material.BOOK) {
      return false;
    }

    if (!Permission.INFO.checkPermission(player)) {
      return false;
    }

    if (hasTimeout(player)) {
      // event.setCancelled(true);
      return false;
    }
    setTimeout(player, DEFAULT_TIMEOUT / 2);

    return true;
  }

  public static void showInfo(final Cancellable event, final Player player, final Entity clicked) {
    Preconditions.checkNotNull(clicked);
    if (!checkShowInfo(event, player)) {
      return;
    }

    getPlugin().getEntityManager().getEntityOf(clicked).ifPresent(entity -> {
      doShowInfo(event, player, entity);
    });
  }

  public static void showInfoInSight(final Cancellable event, final Player player) {
    showInfoInSight(event, player, DEFAULT_SIGHT_DISTANCE);
  }

  public static void showInfoInSight(final Cancellable event, final Player player,
      final int distance) {
    Preconditions.checkArgument(distance >= 0);
    if (!checkShowInfo(event, player)) {
      return;
    }

    LocationBlockIterator<SchematicEntity> it = LocationBlockIterator.iterable(
        new BlockIterator(player, distance), getPlugin().getEntityManager()::getEntitiesAt);
    while (it.hasNext()) {
      SchematicEntity entity = it.next();
      doShowInfo(event, player, entity);
      return;
    }
  }

  private static void doShowInfo(final Cancellable event, final Player player,
      final SchematicEntity entity) {

    ParticleUtils.showParticlesForTime(getPlugin(), 20, player.getWorld(), entity, 0, 0, 255);

    ComponentBuilder cb = new ComponentBuilder("§7Schematic Entity Info\n");
    cb.append("§7 Name: §r" + entity.getName() + "\n");
    cb.append("§7 Entities: §r" + entity.getEntities().size() + "\n");

    if (Permission.INFO_OWNER.checkPermission(player)) {
      cb.append("§7 Placed by: §r" + getPlugin().getPlayerName(entity.getOwner()) //
          + " §7(" + DATE_FORMAT.format(new Date(entity.getTimestamp())) + ")\n");
    }

    try {
      SchematicBookInfo info = getPlugin().getInfoManager().getInfo(entity);

      cb.append("§7 Schematic-Creator: §r" + info.getCreator() + "\n");
      if (Permission.INFO_PERMISSION.checkPermission(player)) {
        cb.append("§7 Schematic-Perm:    §r" + info.getPermission() + "\n");
      }
      if (Permission.INFO_GETTER.checkPermission(player)) {
        cb.append("§5Click to copy Schematic Item");
        cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sitem get " + info.getKey()));
      }

    } catch (ExecutionException e) {
      getPlugin().getLogger().log(Level.SEVERE, "Failed to get schematic info", e);
      player.sendMessage("§cFailed to load schematic information! Was it removed?");
    }

    player.spigot().sendMessage(cb.create());
    event.setCancelled(true);
  }

  // **************************************************
  // Pickup

  private static boolean checkPickup(final Cancellable event, final Player player) {
    Preconditions.checkNotNull(event);
    Preconditions.checkNotNull(player);
    if (player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SPECTATOR) {
      return false;
    }
    if (!player.isSneaking()) {
      return false;
    }

    final ItemStack item = player.getInventory().getItemInMainHand();
    if (item == null || item.getType() != Material.BOOK) {
      return false;
    }

    if (hasTimeout(player)) {
      // event.setCancelled(true);
      return false;
    }
    setTimeout(player);

    return true;
  }

  public static void pickupInSight(final Cancellable event, final Player player) {
    pickupInSight(event, player, DEFAULT_SIGHT_DISTANCE);
  }

  public static void pickupInSight(final Cancellable event, final Player player,
      final int distance) {
    Preconditions.checkArgument(distance >= 0);
    if (!checkPickup(event, player)) {
      return;
    }

    LocationBlockIterator<SchematicEntity> it = LocationBlockIterator.iterable(
        new BlockIterator(player, distance), getPlugin().getEntityManager()::getEntitiesAt);
    while (it.hasNext()) {

      SchematicEntity entity = it.next();
      if (doPickup(event, player, it.getWorld(), entity)) {
        event.setCancelled(true);
        return;
      }
    }
  }


  public static void pickup(final Cancellable event, final Player player, final Entity clicked) {
    if (!checkPickup(event, player)) {
      return;
    }

    getPlugin().getEntityManager().getEntityOf(clicked).ifPresent(entity -> {
      if (doPickup(event, player, clicked.getWorld(), entity)) {
        event.setCancelled(true);
        return;
      }
    });
  }

  public static void pickup(final Cancellable event, final Player player,
      final SchematicEntity entity) {
    Preconditions.checkNotNull(entity);
    if (!checkPickup(event, player)) {
      return;
    }

    if (doPickup(event, player, player.getWorld(), entity)) {
      event.setCancelled(true);
      return;
    }
  }


  private static boolean doPickup(final Cancellable event, final Player player, final World world,
      final SchematicEntity entity) {

    if (!Permission.PICKUP.checkPermission(player)) {
      Out.PERMISSION_MISSING_EXTENSION.send(player, "Schematic-Pickup");
      return false;
    }

    if (getPlugin().getPermcheck().canBuild(player, world, entity)) {

      Optional<SchematicBookInfo> oinfo = removeEntity(player, world, entity);
      if (oinfo.isPresent()) {
        SchematicBookInfo info = oinfo.get();

        if (player.getGameMode() != GameMode.CREATIVE) {
          ItemStack item = player.getInventory().getItemInMainHand();
          if (item != null && item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
            player.getInventory().addItem(SchematicBook.createItem(info));
          } else {
            player.getInventory().setItemInMainHand(SchematicBook.createItem(info));
          }
        } else {
          player.getInventory().addItem(SchematicBook.createItem(info));
        }

        Out.SCHEMATIC_PICKED_UP.send(player, info.getKey(), info.getName());

        return true;
      }
    }
    return false;
  }

  private static Optional<SchematicBookInfo> removeEntity(final Player player, final World world,
      final SchematicEntity entity) {
    try {
      SchematicBookInfo info = getPlugin().getInfoManager().getInfo(entity);
      if (!info.hasPermission(player)) {
        Out.PERMISSION_MISSING_EXTENSION.send(player, "Schematic <this schematic>");
        return Optional.empty();
      }
    } catch (ExecutionException e) {
    }
    try {

      entity.getEntities(world, 1).forEach(Entity::remove);

      final EnumSet<Material> materialsToRemove = EnumSet.of(Material.BARRIER);
      entity.getHitBox().positions().map(p -> world.getBlockAt(p.getX(), p.getY(), p.getZ()))//
          .filter(b -> b.getType().isBlock() && materialsToRemove.contains(b.getType()))//
          .forEach(b -> b.setType(Material.AIR));

      getPlugin().getEntityManager().getCache(world).remove(entity);

      ParticleUtils.showParticles(world, entity, 0, 0, 255);

      return Optional.of(getPlugin().getInfoManager().getInfo(entity));
    } catch (DataStoreException e) {
      getPlugin().getLogger().log(Level.SEVERE, "Failed to remove schematic entity from store", e);
      player.sendMessage("§cFailed to remove schematic entity!");
    } catch (ExecutionException e) {
      getPlugin().getLogger().log(Level.SEVERE, "Failed to get schematic info", e);
      player.sendMessage("§cFailed to load schematic information! Was it removed?");
    }
    return Optional.empty();
  }


  // **************************************************
  // Placing

  private static boolean checkPlace(final Cancellable event, final Player player) {
    Preconditions.checkNotNull(event);
    Preconditions.checkNotNull(player);
    if (player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SPECTATOR) {
      return false;
    }

    final ItemStack item = player.getInventory().getItemInMainHand();
    if (item == null || item.getType() != Material.WRITTEN_BOOK) {
      return false;
    }

    if (hasTimeout(player)) {
      // event.setCancelled(true);
      return false;
    }
    setTimeout(player);

    return true;
  }

  public static void place(final PlayerInteractEvent event, final Player player,
      final ItemStack item) {
    if (!checkPlace(event, player) || item == null || item.getType() != Material.WRITTEN_BOOK) {
      return;
    }

    Optional<SchematicBookInfo> oinfo = getPlugin().getInfoManager().getInfo(item);
    if (oinfo.isPresent()) {
      SchematicBookInfo info = oinfo.get();

      BlockFace face = event.getBlockFace();
      Location to =
          event.getClickedBlock().getLocation().add(face.getModX(), face.getModY(), face.getModZ());

      Rotation destRotation = Rotation.fromYaw((int) player.getLocation().getYaw());

      if (doPlace(event, player, info, to, destRotation)) {
        if (player.getGameMode() != GameMode.CREATIVE) {
          if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
          } else {
            player.getInventory().setItemInMainHand(null);
          }
        }
      }

    }
  }


  private static boolean doPlace(final Cancellable event, final Player player,
      final SchematicBookInfo info, Location to, Rotation destRotation) {
    event.setCancelled(true);

    if (!Permission.PLACE.checkPermission(player)) {
      Out.PERMISSION_MISSING_EXTENSION.send(player, "Schematic-Place");
      return false;
    }

    if (!info.hasPermission(player)) {
      Out.PERMISSION_MISSING_EXTENSION.send(player, "Schematic <this schematic>");
      return false;
    }

    Rotation rotation = Rotation.fromYaw(info.getRotation().getYaw() - destRotation.getYaw());

    try {
      File file = info.getSchematicFile(getPlugin().getSchematicFolder());
      if (!file.exists() || !file.isFile()) {
        player.sendMessage("§cFailed to build! Schematic does not exist!");
        return false;
      }

      BukkitWorld world = new BukkitWorld(to.getWorld());
      EntityEditSession editSession = new EntityEditSession(world, MAX_BLOCKS);
      ClipboardHolder holder = WEUtils.readSchematic(world, file);

      WEUtils.rotate(holder, rotation.getYaw());
      holder = new ClipboardHolder(WEUtils.bakeClipboardTransform(holder), world.getWorldData());

      IntVector boxmin = IntVector.from(holder.getClipboard().getMinimumPoint());
      IntVector boxmax = IntVector.from(holder.getClipboard().getMaximumPoint());
      IntVector origin = IntVector.from(holder.getClipboard().getOrigin());
      boxmin.subtract(origin).add(IntVector.from(to));
      boxmax.subtract(origin).add(IntVector.from(to));


      IntRegion box = info.getHitBoxOffset().rotate(rotation).addTo(new IntRegion(boxmin, boxmax));

      if (!getPlugin().getPermcheck().canBuild(player, to.getWorld(), box)) {
        player.sendMessage("§cYou can not build here!");
        return false;
      }

      if (player.getGameMode() != GameMode.CREATIVE) {
        Map<EnhancedBaseItem, Integer> materials = WEUtils.getBlockTypeCount(holder.getClipboard());
        if (!removeFrom(player, materials)) {
          ComponentBuilder cb =
              new ComponentBuilder("§cYou are missing materials!\n§7Materials needed:");
          materials.entrySet().stream().filter(MATERIALS_FILTER).forEach(e -> {

            cb.append("§7 " + e.getKey().getBlockType().getName() + ":§d" + e.getKey().getData()
                + "§7  x  §d" + e.getValue() + "\n");
            cb.event(
                new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                    new ComponentBuilder("{id:" + e.getKey().getType() + ",Damage:"
                        + e.getKey().getData() + ",Count:" + e.getValue().intValue() + "}")
                            .create()));
          });
          player.spigot().sendMessage(cb.create());
          return false;
        }
      }

      WEUtils.placeSchematic(editSession, holder, to, true);
      Set<UUID> entities = editSession.getCreatedEntityUUIDs();

      /*
       * editSession.getCreatedEntities().stream()// .map(e -> WEUtils.getUUID(e))//
       * .filter(Optional::isPresent)// .map(Optional::get)// .collect(Collectors.toSet());'
       */

      try {
        SchematicEntity entity = new SchematicEntity(info.getName(), destRotation,
            box.getMinPoint(), box.getMaxPoint(), entities, player.getUniqueId());

        ParticleUtils.showParticlesForTime(getPlugin(), 20, to.getWorld(), entity, 0, 255, 0);

        getPlugin().getEntityManager().getCache(to.getWorld()).add(entity);
      } catch (Exception e) {
        editSession.undo(new EntityEditSession(world, MAX_BLOCKS));
        throw e;
      }

      Bukkit.getScheduler().runTaskLater(getPlugin(), () -> player.closeInventory(), 1);

      return true;
    } catch (DataStoreException e) {
      getPlugin().getLogger().log(Level.SEVERE, "Failed to save schematic entity to store", e);
      player.sendMessage("§cFailed to save schematic entity!");
    } catch (MaxChangedBlocksException e) {
      getPlugin().getLogger().log(Level.WARNING, "Invalid Schematic: max blocks " + MAX_BLOCKS);
      player.sendMessage("§cInvalid schematic!");
    } catch (IOException e) {
      getPlugin().getLogger().log(Level.SEVERE, "Failed to place schematic", e);
      player.sendMessage("§cFailed to build here!");
    }
    return false;
  }

  // **************************************************
  // Helper


  public static boolean containsAtLeast(Player player, Map<EnhancedBaseItem, Integer> materials) {
    final PlayerInventory inv = player.getInventory();
    return !materials.entrySet().stream().filter(MATERIALS_FILTER)//
        .filter(e -> !inv.containsAtLeast(e.getKey().toItemStack(1), e.getValue())).findAny()
        .isPresent();
  }

  public static boolean removeFrom(Player player, Map<EnhancedBaseItem, Integer> materials) {
    if (!containsAtLeast(player, materials)) {
      return false;
    }

    List<ItemStack> toRemove = materials.entrySet().stream()//
        .filter(MATERIALS_FILTER)//
        .map(e -> e.getKey().toItemStack(e.getValue()))//
        .collect(Collectors.toList());

    if (toRemove.isEmpty()) {
      return true;
    }

    player.getInventory().removeItem(toRemove.toArray(new ItemStack[toRemove.size()]));
    return true;
  }



}