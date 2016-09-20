package de.mickare.schematicbooks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.util.BlockIterator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicLongMap;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BlockID;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.session.ClipboardHolder;

import de.mickare.schematicbooks.commands.MainSchematicItemsCommand;
import de.mickare.schematicbooks.data.DataStoreException;
import de.mickare.schematicbooks.data.SchematicEntity;
import de.mickare.schematicbooks.event.EventFactory;
import de.mickare.schematicbooks.event.PickupSchematicEvent;
import de.mickare.schematicbooks.event.PlaceSchematicEvent;
import de.mickare.schematicbooks.util.BiIntFunction;
import de.mickare.schematicbooks.util.BukkitReflect;
import de.mickare.schematicbooks.util.IntRegion;
import de.mickare.schematicbooks.util.IntVector;
import de.mickare.schematicbooks.util.ParticleUtils;
import de.mickare.schematicbooks.util.Rotation;
import de.mickare.schematicbooks.we.EntityEditSession;
import de.mickare.schematicbooks.we.ExcludeBlockMask;
import de.mickare.schematicbooks.we.PasteOperation;
import de.mickare.schematicbooks.we.WEUtils;
import de.mickare.schematicbooks.we.WEUtils.EnhancedBaseItem;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Interactions {

  private static @Getter @Setter boolean createBookEntityAnytime = false;

  // Instance
  private static @Getter @Setter(AccessLevel.PROTECTED) SchematicBooksPlugin plugin;

  // Variables
  public static final int MAX_BLOCKS = 1000;
  private static final long DEFAULT_TIMEOUT = 500;
  private static final int DEFAULT_SIGHT_DISTANCE = 5;


  public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

  public static Stream<SchematicEntity> entityIterate(LivingEntity entity, int distance) {
    return blockIterate(entity, distance)//
        .flatMap(block -> getPlugin().getEntityManager().getEntitiesAt(block).stream());
  }

  public static Stream<Block> blockIterate(LivingEntity entity, int distance) {
    return stream(new BlockIterator(entity, distance));
  }

  public static <T> Stream<T> stream(Iterator<T> iterator) {
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
        false);
  }

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
      doShowInfo(event, player, player.getWorld(), entity);
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

    final World world = player.getWorld();
    Iterator<SchematicEntity> it = entityIterate(player, distance).iterator();

    while (it.hasNext()) {
      SchematicEntity entity = it.next();
      doShowInfo(event, player, world, entity);
      return;
    }
  }

  private static void doShowInfo(final Cancellable event, final Player player, final World world,
      final SchematicEntity entity) {

    if (EventFactory.callInfoEvent(player, world, event, entity).isCancelled()) {
      return;
    }

    ParticleUtils.showParticlesForTime(getPlugin(), 20, world, entity, 0, 0, 255);

    ComponentBuilder cb = new ComponentBuilder("§6Schematic Entity Info");
    cb.append("\n§7 Name: §r" + entity.getName());
    cb.append("\n§7 Entities: §r" + entity.getEntities().size());

    if (Permission.INFO_OWNER.checkPermission(player)) {
      cb.append("\n§7 Placed by: §r");
      String playerName = getPlugin().getPlayerName(entity.getOwner());
      if (playerName != null) {
        cb.append(playerName);
        if (Permission.INFO_UUID.checkPermission(player)) {
          cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
              TextComponent.fromLegacyText("§d" + entity.getOwner())));
        }
      } else {
        cb.append("§d" + entity.getOwner());
      }

      cb.append("§7 (" + DATE_FORMAT.format(new Date(entity.getTimestamp())) + ")",
          FormatRetention.NONE);
    }

    try {
      SchematicBookInfo info = getPlugin().getInfoManager().getInfo(entity);

      cb.append("\n§7 Created by: §r");
      UUID creatorUUID = getPlugin().getPlayerUUID(info.getCreator());
      cb.append(info.getCreator());
      if (creatorUUID != null && Permission.INFO_UUID.checkPermission(player)) {
        cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            TextComponent.fromLegacyText("§d" + creatorUUID.toString())));
      }
      cb.append("", FormatRetention.NONE);
      if (info.hasPermission() && Permission.INFO_PERMISSION.checkPermission(player)) {
        cb.append("\n §c Permission required\n§d " + info.getPermission());
      }
      if (Permission.INFO_GETTER.checkPermission(player)) {
        cb.append("\n §e>> §oClick to get schematic book§e <<");
        cb.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
            "/" + MainSchematicItemsCommand.CMD + " get " + info.getKey()));
        cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            TextComponent.fromLegacyText("§e>> §oClick§e <<")));
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

    final World world = player.getWorld();
    Iterator<SchematicEntity> it = entityIterate(player, distance).iterator();

    while (it.hasNext()) {

      SchematicEntity entity = it.next();
      if (doPickup(event, player, world, entity)) {
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

    PickupSchematicEvent pickupEvent = EventFactory.callPickupEvent(player, world, event, entity);
    if (pickupEvent.isCancelled()) {
      return false;
    }

    if (!Permission.PICKUP.checkPermission(player)) {
      Out.PERMISSION_MISSING_EXTENSION.send(player, "Schematic-Pickup");
      return false;
    }

    if (getPlugin().getPermcheck().canBuild(player, world, entity)) {

      Optional<SchematicBookInfo> oinfo = removeEntity(player, world, entity);
      if (oinfo.isPresent()) {
        SchematicBookInfo info = oinfo.get();

        ItemStack schematicBook = SchematicBook.createItem(info);
        if (player.getGameMode() != GameMode.CREATIVE) {
          ItemStack item = player.getInventory().getItemInMainHand();
          if (item != null && item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
            player.getInventory().addItem(schematicBook);
          } else {
            player.getInventory().setItemInMainHand(schematicBook);
          }
        } else {
          if (!player.getInventory().containsAtLeast(schematicBook, 1)) {
            player.getInventory().addItem(schematicBook);
          }
        }

        Out.SCHEMATIC_PICKED_UP.send(player, info.getKey(), info.getName());

        return true;
      }
    }
    return false;
  }

  public static Optional<SchematicBookInfo> removeEntity(final Player player, final World world,
      final SchematicEntity entity) {
    try {
      SchematicBookInfo info = getPlugin().getInfoManager().getInfo(entity);
      if (!info.checkPermission(player)) {
        Out.PERMISSION_MISSING_EXTENSION.send(player, "Schematic <this schematic>");
        return Optional.empty();
      }
    } catch (ExecutionException e) {
    }

    try {

      entity.getEntityObjects(world).forEach(Entity::remove);

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

      player.sendMessage("§cFailed to load schematic information! Was it removed?");
      if (e.getCause() != null) {
        if (e.getCause() instanceof FileNotFoundException) {
          getPlugin().getLogger().log(Level.WARNING,
              "Failed to get schematic info: " + e.getCause().getMessage());
        } else {
          getPlugin().getLogger().log(Level.SEVERE, "Failed to get schematic info", e.getCause());
        }
      } else {
        getPlugin().getLogger().log(Level.SEVERE, "Failed to get schematic info", e);
      }

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

    Optional<String> key = SchematicBook.getSchematicKey(item);
    if (!key.isPresent()) {
      return;
    }

    if (!getPlugin().getInfoManager().infoExists(key.get())) {
      player.sendMessage("§cSchematic book does not exist");
      return;
    }

    Optional<SchematicBookInfo> oinfo = getPlugin().getInfoManager().getInfoOptional(key.get());
    if (oinfo.isPresent()) {
      SchematicBookInfo info = oinfo.get();

      BlockFace face = event.getBlockFace();
      Location to =
          event.getClickedBlock().getLocation().add(face.getModX(), face.getModY(), face.getModZ());

      Rotation destRotation = Rotation.fromYaw((int) player.getLocation().getYaw());

      // Place it!
      PlaceResult place = doPlace(event, player, info, to, destRotation);
      if (place.isSuccess()) {

        if (player.getGameMode() != GameMode.CREATIVE && !place.isPlainPaste()) {
          if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItemInMainHand(item);
          } else {
            player.getInventory().setItemInMainHand(null);
          }
        }

      }

    } else {
      player.sendMessage("§cInvalid schematic book!");
    }
  }


  @SuppressWarnings("deprecation")
  private static BaseComponent[] createItemInfo(int id, int count, short damage) {
    return new ComponentBuilder(
        BukkitReflect.convertItemStackToJson(new ItemStack(id, count, damage))).create();
  }

  private static List<BaseBlock> BLOCKS_REPLACE_EXCLUDED = Lists.newArrayList(//
      new BaseBlock(BlockType.BEDROCK.getID(), -1) //
  );

  private static List<BaseBlock> BLOCKS_PLACE_PLAIN_MASK = Lists.newArrayList(//
      new BaseBlock(Material.BARRIER.getId(), -1) //
  );

  @Getter
  @RequiredArgsConstructor
  public static class PlaceResult {
    public static final PlaceResult FAILED = new PlaceResult(false, false, false, 0);
    public static final PlaceResult CANCELLED = new PlaceResult(false, true, false, 0);

    public static PlaceResult success(boolean plainPaste, int entitiesCount) {
      return new PlaceResult(true, false, plainPaste, entitiesCount);
    }

    private final boolean success;
    private final boolean cancelled;
    private final boolean plainPaste;
    private final int entitiesCount;
  }

  private static PlaceResult doPlace(final Cancellable event, final Player player,
      SchematicBookInfo info, Location to, Rotation destRotation) {

    PlaceSchematicEvent placeEvent =
        EventFactory.callPlaceEvent(player, to, event, info, destRotation);
    if (placeEvent.isCancelled()) {
      return PlaceResult.CANCELLED;
    }
    info = placeEvent.getInfo();
    to = placeEvent.getTo();
    destRotation = placeEvent.getDestinationRotation();

    final World bukkitWorld = to.getWorld();

    event.setCancelled(true);

    if (!Permission.PLACE.checkPermission(player)) {
      Out.PERMISSION_MISSING_EXTENSION.send(player, "Schematic-Place");
      return PlaceResult.FAILED;
    }

    if (!info.checkPermission(player)) {
      Out.PERMISSION_MISSING_EXTENSION.send(player, "Schematic <this schematic>");
      return PlaceResult.FAILED;
    }

    // Negative rotation, because we rotate in the other look direction as the player!
    Rotation rotation = Rotation.fromYaw(info.getRotation().getYaw() - destRotation.getYaw());

    try {
      File file = getPlugin().getInfoManager().getSchematicFileOf(info).toFile();
      if (!file.exists() || !file.isFile()) {
        player.sendMessage("§cFailed to build! Schematic does not exist!");
        return PlaceResult.FAILED;
      }

      BukkitWorld world = new BukkitWorld(bukkitWorld);
      EntityEditSession editSession = new EntityEditSession(world, MAX_BLOCKS);
      editSession.setMask(new ExcludeBlockMask(world, BLOCKS_REPLACE_EXCLUDED));

      ClipboardHolder holder = WEUtils.readSchematic(world, file);

      WEUtils.rotate(holder, rotation.getYaw());
      holder = new ClipboardHolder(WEUtils.bakeClipboardTransform(holder), world.getWorldData());

      IntVector boxmin = IntVector.from(holder.getClipboard().getMinimumPoint());
      IntVector boxmax = IntVector.from(holder.getClipboard().getMaximumPoint());
      IntVector origin = IntVector.from(holder.getClipboard().getOrigin());
      boxmin.subtract(origin).add(IntVector.from(to));
      boxmax.subtract(origin).add(IntVector.from(to));

      IntRegion box =
          info.getHitBoxOffset().rotate(-rotation.getYaw()).addTo(new IntRegion(boxmin, boxmax));

      if (!getPlugin().getPermcheck().canBuild(player, bukkitWorld, box)) {
        player.sendMessage("§cYou can not build here!");
        return PlaceResult.FAILED;
      }



      if (player.getGameMode() != GameMode.CREATIVE) {
        AtomicLongMap<EnhancedBaseItem> materials =
            filterMaterials(WEUtils.getBlockTypeCount(holder.getClipboard()));
        if (!removeFrom(player, materials.asMap())) {
          ComponentBuilder cb =
              new ComponentBuilder("§cYou are missing materials!\n§7Materials needed:");
          materials.asMap().entrySet().forEach(e -> {

            cb.append("\n");
            cb.append("§7 " + e.getKey().getBlockType().getName() + ":" + e.getKey().getData()
                + "§7  x  §d" + e.getValue() + "");
            cb.event(new HoverEvent(HoverEvent.Action.SHOW_ITEM,
                createItemInfo(e.getKey().getId(), e.getValue().intValue(), e.getKey().getData())));

          });
          player.spigot().sendMessage(cb.create());
          return PlaceResult.FAILED;
        }
      }

      final boolean plainPaste = holder.getClipboard().getEntities().isEmpty()
          ? Permission.PLACE_PLAIN.checkPermission(player) : false;



      OptionalInt maxiumumEntities = box.getChunks().stream()//
          .map(cpos -> bukkitWorld.getChunkAt(cpos.getX(), cpos.getZ()))//
          .mapToInt(chunk -> chunk.getEntities().length)//
          .max();
      if (maxiumumEntities.orElse(0) + holder.getClipboard().getEntities().size() > getPlugin()
          .getEntityLimitPerChunk(player)) {
        player.sendMessage("§cEntity limit!");
        return PlaceResult.FAILED;
      }


      PasteOperation paste = WEUtils.newPaste(editSession, holder)//
          .ignoreAirBlocks(true).location(to);
      if (!plainPaste || !Permission.PLACE_PLAIN_UNMASK.checkPermission(player)) {
        paste.addSourceMask(b -> new ExcludeBlockMask(b.clipboard(), BLOCKS_PLACE_PLAIN_MASK));
      }
      paste.buildAndPaste();

      Set<UUID> entities = editSession.getCreatedEntityUUIDs();

      if (entities.size() > 0) {
        List<LivingEntity> entitiesObjects = bukkitWorld.getLivingEntities().stream()
            .filter(e -> entities.contains(e.getUniqueId())).collect(Collectors.toList());
        entitiesObjects.forEach(e -> e.setInvulnerable(true));
      }


      /*
       * editSession.getCreatedEntities().stream()// .map(e -> WEUtils.getUUID(e))//
       * .filter(Optional::isPresent)// .map(Optional::get)// .collect(Collectors.toSet());'
       */

      try {
        SchematicEntity entity = new SchematicEntity(info.getName(), destRotation,
            box.getMinPoint(), box.getMaxPoint(), entities, player.getUniqueId());

        ParticleUtils.showParticlesForTime(getPlugin(), 20, bukkitWorld, entity, 0, 255, 0);

        if (createBookEntityAnytime || !plainPaste) {
          getPlugin().getEntityManager().getCache(bukkitWorld).add(entity);
        }
      } catch (Exception e) {
        editSession.undo(new EntityEditSession(world, MAX_BLOCKS));
        throw e;
      }

      Bukkit.getScheduler().runTaskLater(getPlugin(), () -> player.closeInventory(), 1);

      return PlaceResult.success(plainPaste, entities.size());
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
    return PlaceResult.FAILED;
  }

  // **************************************************
  // Helper


  @RequiredArgsConstructor
  public static class DataConverter {
    private @NonNull final BiIntFunction idFunc;
    private @NonNull final BiIntFunction dataFunc;
    private @NonNull final BiIntFunction amountFunc;


    public DataConverter(Material material, int data) {
      this(material, d -> data, a -> a);
    }

    public DataConverter(Material material, IntUnaryOperator dataFunc,
        IntUnaryOperator amountFunc) {
      this(i -> material.getId(), dataFunc, amountFunc);
    }


    public DataConverter(IntUnaryOperator idFunc, IntUnaryOperator dataFunc,
        IntUnaryOperator amountFunc) {
      this((i, d) -> idFunc.applyAsInt(i), (i, d) -> dataFunc.applyAsInt(d),
          (d, a) -> amountFunc.applyAsInt(a));
    }

    public void apply(EnhancedBaseItem item, int amount, AtomicLongMap<EnhancedBaseItem> map) {
      map.addAndGet(
          new EnhancedBaseItem(idFunc.apply(item.getId(), item.getData()),
              dataFunc.apply(item.getId(), item.getData())),
          amountFunc.apply(item.getData(), amount));
    }
  }

  private static final Map<Integer, DataConverter> dataFilters = Maps.newHashMap();
  static {

    DataConverter SUBID_ZERO = new DataConverter(i -> i, d -> 0, a -> a);

    // WOOD, LOG, LEAVES
    dataFilters.put(BlockID.LEAVES, new DataConverter(i -> i, d -> d % 4, a -> a));
    dataFilters.put(BlockID.LEAVES2, new DataConverter(i -> i, d -> d % 4, a -> a));
    dataFilters.put(BlockID.LOG, new DataConverter(i -> i, d -> d % 4, a -> a));
    dataFilters.put(BlockID.LOG2, new DataConverter(i -> i, d -> d % 4, a -> a));

    // Blocks
    dataFilters.put(BlockID.QUARTZ_BLOCK,
        new DataConverter(i -> i, d -> ((d >= 2) || d <= 4) ? 2 : d, a -> a));


    dataFilters.put(BlockID.ACTIVATOR_RAIL, SUBID_ZERO);
    dataFilters.put(BlockID.DETECTOR_RAIL, SUBID_ZERO);
    dataFilters.put(BlockID.POWERED_RAIL, SUBID_ZERO);
    dataFilters.put(BlockID.MINECART_TRACKS, SUBID_ZERO);

    dataFilters.put(BlockID.CHEST, SUBID_ZERO);
    dataFilters.put(BlockID.ENDER_CHEST, SUBID_ZERO);
    dataFilters.put(BlockID.TRAPPED_CHEST, SUBID_ZERO);

    dataFilters.put(BlockID.FURNACE, SUBID_ZERO);
    dataFilters.put(BlockID.BURNING_FURNACE,
        new DataConverter(i -> BlockID.FURNACE, d -> 0, a -> a));

    dataFilters.put(BlockID.LADDER, SUBID_ZERO);

    // STAIRSt
    dataFilters.put(BlockID.OAK_WOOD_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.COBBLESTONE_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.BRICK_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.STONE_BRICK_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.NETHER_BRICK_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.SANDSTONE_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.SPRUCE_WOOD_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.BIRCH_WOOD_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.JUNGLE_WOOD_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.QUARTZ_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.ACACIA_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.DARK_OAK_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.RED_SANDSTONE_STAIRS, SUBID_ZERO);
    dataFilters.put(BlockID.PURPUR_STAIRS, SUBID_ZERO);

    // STEPS
    DataConverter DOUBLE_STEP = new DataConverter(i -> i + 1, d -> d % 8, a -> 2 * a);
    DataConverter STEP = new DataConverter(i -> i, d -> d % 8, a -> a);
    dataFilters.put(BlockID.DOUBLE_STEP, DOUBLE_STEP);
    dataFilters.put(BlockID.STEP, STEP);
    dataFilters.put(BlockID.DOUBLE_WOODEN_STEP, DOUBLE_STEP);
    dataFilters.put(BlockID.WOODEN_STEP, STEP);
    dataFilters.put(BlockID.DOUBLE_STEP2, DOUBLE_STEP);
    dataFilters.put(BlockID.STEP2, STEP);
    dataFilters.put(BlockID.PURPUR_DOUBLE_SLAB, DOUBLE_STEP);
    dataFilters.put(BlockID.PURPUR_SLAB, STEP);

    // WATER & LAVA
    dataFilters.put(BlockID.WATER, new DataConverter(Material.WATER_BUCKET, 0));
    dataFilters.put(BlockID.STATIONARY_WATER, new DataConverter(Material.WATER_BUCKET, 0));
    dataFilters.put(BlockID.LAVA, new DataConverter(Material.LAVA_BUCKET, 0));
    dataFilters.put(BlockID.STATIONARY_LAVA, new DataConverter(Material.LAVA_BUCKET, 0));

    // Gate & Doors
    dataFilters.put(BlockID.ACACIA_FENCE_GATE, new DataConverter(Material.ACACIA_FENCE_GATE, 0));
    dataFilters.put(BlockID.BIRCH_FENCE_GATE, new DataConverter(Material.BIRCH_FENCE_GATE, 0));
    dataFilters.put(BlockID.DARK_OAK_FENCE_GATE,
        new DataConverter(Material.DARK_OAK_FENCE_GATE, 0));
    dataFilters.put(BlockID.FENCE_GATE, new DataConverter(Material.FENCE_GATE, 0));
    dataFilters.put(BlockID.JUNGLE_FENCE_GATE, new DataConverter(Material.JUNGLE_FENCE_GATE, 0));
    dataFilters.put(BlockID.SPRUCE_FENCE_GATE, new DataConverter(Material.SPRUCE_FENCE_GATE, 0));

    dataFilters.put(BlockID.TRAP_DOOR, new DataConverter(Material.TRAP_DOOR, 0));
    dataFilters.put(BlockID.IRON_TRAP_DOOR, new DataConverter(Material.IRON_TRAPDOOR, 0));

    dataFilters.put(BlockID.WOODEN_DOOR, new DataConverter(Material.WOOD_DOOR, 0));
    dataFilters.put(BlockID.IRON_DOOR, new DataConverter(Material.IRON_DOOR, 0));
    dataFilters.put(BlockID.SPRUCE_DOOR, new DataConverter(Material.SPRUCE_DOOR_ITEM, 0));
    dataFilters.put(BlockID.BIRCH_DOOR, new DataConverter(Material.BIRCH_DOOR_ITEM, 0));
    dataFilters.put(BlockID.JUNGLE_DOOR, new DataConverter(Material.JUNGLE_DOOR_ITEM, 0));
    dataFilters.put(BlockID.ACACIA_DOOR, new DataConverter(Material.ACACIA_DOOR_ITEM, 0));
    dataFilters.put(BlockID.DARK_OAK_DOOR, new DataConverter(Material.DARK_OAK_DOOR_ITEM, 0));

    // Double Flowers
    dataFilters.put(BlockID.DOUBLE_PLANT, new DataConverter(i -> i, d -> d % 8, a -> a));

    // Redstone
    dataFilters.put(BlockID.REDSTONE_WIRE, new DataConverter(Material.REDSTONE, 0));
    dataFilters.put(BlockID.REDSTONE_TORCH_OFF, new DataConverter(Material.REDSTONE_TORCH_ON, 0));
    dataFilters.put(BlockID.REDSTONE_LAMP_ON, new DataConverter(Material.REDSTONE_LAMP_OFF, 0));
    dataFilters.put(BlockID.REDSTONE_REPEATER_OFF, new DataConverter(Material.DIODE, 0));
    dataFilters.put(BlockID.REDSTONE_REPEATER_ON, new DataConverter(Material.DIODE, 0));

    dataFilters.put(BlockID.COMPARATOR_OFF, new DataConverter(Material.REDSTONE_COMPARATOR, 0));
    dataFilters.put(BlockID.COMPARATOR_ON, new DataConverter(Material.REDSTONE_COMPARATOR, 0));

    dataFilters.put(BlockID.PISTON_BASE, new DataConverter(Material.PISTON_BASE, 0));
    dataFilters.put(BlockID.PISTON_STICKY_BASE, new DataConverter(Material.PISTON_STICKY_BASE, 0));
    dataFilters.put(BlockID.PISTON_EXTENSION, new DataConverter(i -> i, d -> d, a -> 0));
    dataFilters.put(BlockID.PISTON_MOVING_PIECE, new DataConverter(i -> i, d -> d, a -> 0));

    dataFilters.put(BlockID.DISPENSER, SUBID_ZERO);
    dataFilters.put(BlockID.DROPPER, SUBID_ZERO);
    dataFilters.put(BlockID.HOPPER, SUBID_ZERO);
    dataFilters.put(BlockID.LEVER, SUBID_ZERO);
    dataFilters.put(BlockID.TRIPWIRE_HOOK, SUBID_ZERO);
    dataFilters.put(BlockID.TRIPWIRE, new DataConverter(Material.STRING, 0));

    dataFilters.put(BlockID.STONE_BUTTON, SUBID_ZERO);
    dataFilters.put(BlockID.WOODEN_BUTTON, SUBID_ZERO);
    dataFilters.put(BlockID.STONE_PRESSURE_PLATE, SUBID_ZERO);
    dataFilters.put(BlockID.WOODEN_PRESSURE_PLATE, SUBID_ZERO);
    dataFilters.put(BlockID.PRESSURE_PLATE_HEAVY, SUBID_ZERO);
    dataFilters.put(BlockID.PRESSURE_PLATE_LIGHT, SUBID_ZERO);
  }


  private static final Set<EnhancedBaseItem> IGNORED_MATERIALS = Sets.newHashSet();
  static {
    for (short i = 0; i < 16; ++i) {
      IGNORED_MATERIALS.add(new EnhancedBaseItem(Material.BARRIER.getId(), i));
    }
  }
  private static final Predicate<? super Entry<EnhancedBaseItem, Integer>> MATERIALS_FILTER =
      e -> (e.getKey().getId() != 0 && !IGNORED_MATERIALS.contains(e.getKey()) && e.getValue() > 0);

  private static AtomicLongMap<EnhancedBaseItem> filterMaterials(
      Map<EnhancedBaseItem, Integer> mat) {
    AtomicLongMap<EnhancedBaseItem> filtered = AtomicLongMap.create();

    mat.entrySet().stream().filter(MATERIALS_FILTER).forEach(e -> {

      DataConverter converter = dataFilters.get(e.getKey().getType());
      if (converter != null) {
        converter.apply(e.getKey(), e.getValue(), filtered);
      } else {
        filtered.addAndGet(e.getKey(), e.getValue());
      }

    });

    filtered.removeAllZeros();
    return filtered;
  }

  public static boolean containsAtLeast(Player player, Map<EnhancedBaseItem, Long> materials) {
    final PlayerInventory inv = player.getInventory();
    return !materials.entrySet().stream()
        .filter(e -> !inv.containsAtLeast(e.getKey().toItemStack(1), e.getValue().intValue()))
        .findAny().isPresent();
  }

  public static boolean removeFrom(Player player, Map<EnhancedBaseItem, Long> materials) {
    if (!containsAtLeast(player, materials)) {
      return false;
    }

    List<ItemStack> toRemove = materials.entrySet().stream()//
        .map(e -> e.getKey().toItemStack(e.getValue().intValue()))//
        .collect(Collectors.toList());

    if (toRemove.isEmpty()) {
      return true;
    }

    player.getInventory().removeItem(toRemove.toArray(new ItemStack[toRemove.size()]));
    return true;
  }



}
