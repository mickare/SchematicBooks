package de.mickare.schematicbooks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.bukkit.World;
import org.bukkit.entity.Player;

import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.session.ClipboardHolder;

import de.mickare.schematicbooks.util.Rotation;
import de.mickare.schematicbooks.we.EntityEditSession;
import de.mickare.schematicbooks.we.WEUtils;

public class InteractionsUtils {

  private InteractionsUtils() {}


  public static ClipboardHolder loadRotatedClipboard(World bukkitWorld, Rotation rotation,
      SchematicBookInfo info, Player player)
      throws FileNotFoundException, MaxChangedBlocksException, IOException {
    BukkitWorld world = new BukkitWorld(bukkitWorld);
    EntityEditSession editSession = new EntityEditSession(world, Interactions.MAX_BLOCKS);
    return loadRotatedClipboard(editSession, rotation, info, player);
  }

  public static ClipboardHolder loadRotatedClipboard(EntityEditSession editSession,
      Rotation rotation, SchematicBookInfo info, Player player)
      throws FileNotFoundException, MaxChangedBlocksException, IOException {
    File file = info.getSchematicFilePath(Interactions.getPlugin().getSchematicFolder()).toFile();
    if (!file.exists() || !file.isFile()) {
      player.sendMessage("§cFailed to build! Schematic does not exist!");
      return null;
    }
    return loadRotatedClipboard(editSession, rotation, info, file);
  }

  public static ClipboardHolder loadRotatedClipboard(EntityEditSession editSession,
      Rotation rotation, SchematicBookInfo info, File schematicFile)
      throws FileNotFoundException, IOException, MaxChangedBlocksException {

    ClipboardHolder holder = WEUtils.readSchematic(editSession.getWorld(), schematicFile);

    WEUtils.rotate(holder, rotation.getYaw());
    return new ClipboardHolder(WEUtils.bakeClipboardTransform(holder),
        editSession.getWorld().getWorldData());
  }

}
