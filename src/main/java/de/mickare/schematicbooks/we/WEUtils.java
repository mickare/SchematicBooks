package de.mickare.schematicbooks.we;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.sk89q.jnbt.CompoundTag;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.blocks.BaseBlock;
import com.sk89q.worldedit.blocks.BaseItem;
import com.sk89q.worldedit.blocks.BlockType;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.entity.BaseEntity;
import com.sk89q.worldedit.entity.Entity;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardWriter;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import com.sk89q.worldedit.world.registry.WorldData;

import de.mickare.schematicbooks.reflection.ReflectUtils;

public class WEUtils {

  private WEUtils() {}

  public static Clipboard bakeClipboardTransform(ClipboardHolder holder)
      throws MaxChangedBlocksException {
    return bakeClipboardTransform(holder.getClipboard(), holder.getWorldData(),
        holder.getTransform());
  }

  public static Clipboard bakeClipboardTransform(Clipboard clipboard, WorldData worldData,
      Transform transform) throws MaxChangedBlocksException {
    if (!transform.isIdentity()) {
      Clipboard target;
      FlattenedClipboardTransform result =
          FlattenedClipboardTransform.transform(clipboard, transform, worldData);
      target = new BlockArrayClipboard(result.getTransformedRegion());
      target.setOrigin(clipboard.getOrigin());
      Operations.completeLegacy(result.copyTo(target));
      return target;
    } else {
      return clipboard;
    }
  }

  private static BufferedOutputStream openWriteFile(File file) throws IOException {
    File parent = file.getParentFile();
    if (parent != null && !parent.exists()) {
      if (!parent.mkdirs()) {
        throw new IOException("Could not create parent folder");
      }
    }
    return new BufferedOutputStream(new FileOutputStream(file));
  }

  public static void writeSchematic(ClipboardHolder holder, File file)
      throws IOException, MaxChangedBlocksException {
    try (BufferedOutputStream bos = openWriteFile(file)) {
      writeSchematic(holder, bos);
    }
  }

  public static void writeSchematic(ClipboardHolder holder, OutputStream out)
      throws MaxChangedBlocksException, IOException {
    writeSchematic(bakeClipboardTransform(holder), holder.getWorldData(), out);
  }


  public static void writeSchematic(Clipboard clipboard, WorldData worldData, File file)
      throws IOException {
    try (BufferedOutputStream bos = openWriteFile(file)) {
      writeSchematic(clipboard, worldData, bos);
    }
  }

  public static void writeSchematic(Clipboard clipboard, WorldData worldData, OutputStream out)
      throws IOException {
    try (ClipboardWriter writer = ClipboardFormat.SCHEMATIC.getWriter(out)) {
      writer.write(clipboard, worldData);
    }
  }

  public static ClipboardHolder readSchematic(BukkitWorld world, InputStream in)
      throws IOException {
    ClipboardReader reader = ClipboardFormat.SCHEMATIC.getReader(in);
    WorldData worldData = world.getWorldData();
    Clipboard clipboard = reader.read(worldData);
    return new ClipboardHolder(clipboard, worldData);
  }

  public static ClipboardHolder readSchematic(BukkitWorld world, File file)
      throws FileNotFoundException, IOException {
    try (FileInputStream fis = new FileInputStream(file); //
        BufferedInputStream bis = new BufferedInputStream(fis)) {
      return readSchematic(world, bis);
    }
  }

  public static void rotate(ClipboardHolder holder, int rotationY) {
    Preconditions.checkArgument(rotationY % 90 == 0);
    AffineTransform transform = new AffineTransform();
    transform = transform.rotateY(rotationY);
    transform = transform.rotateX(0);
    transform = transform.rotateZ(0);
    holder.setTransform(holder.getTransform().combine(transform));
  }

  public static void placeSchematic(EditSession editSession, ClipboardHolder holder, Location to,
      boolean ignoreAirBlocks) throws MaxChangedBlocksException {

    Vector toVec = new Vector(to.getBlockX(), to.getBlockY(), to.getBlockZ());
    Operation operation = holder.createPaste(editSession, editSession.getWorld().getWorldData())
        .to(toVec).ignoreAirBlocks(ignoreAirBlocks).build();
    Operations.completeLegacy(operation);
  }

  /*
   * public static Optional<UUID> getUUID(Entity entity) { Optional<UUID> result =
   * getUUID(entity.getState()); if (result.isPresent()) { return result; } Method m =
   * ReflectUtils.getFirstMethod(entity.getClass(), UUID.class); if (m != null) { try { UUID uuid =
   * (UUID) m.invoke(entity); if (uuid != null) { return Optional.of(uuid); } } catch
   * (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) { } } return
   * Optional.empty(); }
   */

  public static Optional<UUID> getUUID(BaseEntity entity) {
    if (entity.hasNbtData()) {
      CompoundTag main = entity.getNbtData();
      if (main.containsKey("UUIDMost") && main.containsKey("UUIDLeast")) {
        return Optional.of(new UUID(main.getLong("UUIDMost"), main.getLong("UUIDLeast")));
      } else if (main.containsKey("UUID")) {
        return Optional.of(UUID.fromString(main.getString("UUID")));
      }
    }
    return Optional.empty();
  }

  public static class EnhancedBaseItem extends BaseItem {

    public EnhancedBaseItem(int id) {
      super(id);
    }

    public EnhancedBaseItem(int id, short data) {
      super(id, data);
    }

    public EnhancedBaseItem(BaseItem other) {
      super(other.getType(), other.getData());
    }

    @Override
    public int hashCode() {
      return Objects.hash(this.getType(), this.getData());
    }

    @Override
    public boolean equals(Object obj) {
      if (obj != null && obj instanceof BaseItem) {
        BaseItem other = (BaseItem) obj;
        return other.getType() == this.getType() && other.getData() == this.getData();
      }
      return false;
    }

    @SuppressWarnings("deprecation")
    public ItemStack toItemStack(int amount) {
      return new ItemStack(this.getType(), amount, this.getData());
    }

    public BlockType getBlockType() {
      return BlockType.fromID(this.getType());
    }


  }

  // Id -> Count
  public static Map<EnhancedBaseItem, Integer> getBlockTypeCount(final Clipboard clipboard) {
    final Map<EnhancedBaseItem, Integer> result = Maps.newHashMap();
    final Vector min = clipboard.getMinimumPoint();
    final Vector max = clipboard.getMaximumPoint();
    for (int x = min.getBlockX(); x <= max.getBlockX(); x++) {
      for (int y = min.getBlockY(); y <= max.getBlockY(); y++) {
        for (int z = min.getBlockZ(); z <= max.getBlockZ(); z++) {
          BaseBlock block = clipboard.getBlock(new Vector(x, y, z));

          if (!block.isAir()) {
            // BaseBlock key = new BaseBlock(block.getId(), block.getData());
            BaseItem item = BlockType.getBlockBagItem(block.getId(), block.getData());
            if (item != null) {
              result.compute(new EnhancedBaseItem(item), (b, v) -> v != null ? v + 1 : 1);
            }
          }
        }
      }
    }
    return result;
  }

}
