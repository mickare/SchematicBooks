package de.mickare.schematicbooks.commands.books;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import de.mickare.schematicbooks.Out;
import de.mickare.schematicbooks.Permission;
import de.mickare.schematicbooks.SchematicBookInfo;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.AbstractCommand;
import de.mickare.schematicbooks.data.SchematicEntity;
import de.mickare.schematicbooks.event.InfoSchematicEvent;
import de.mickare.schematicbooks.event.PlaceSchematicEvent;
import de.mickare.schematicbooks.util.IntRegion;
import de.mickare.schematicbooks.util.IntVector;
import de.mickare.schematicbooks.util.IntVectorAxis;
import de.mickare.schematicbooks.util.ParticleUtils;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class OffsetCommand extends AbstractCommand<SchematicBooksPlugin> implements Listener {

  private final Cache<Player, OffsetAction> actions = CacheBuilder.newBuilder()//
      .weakKeys().expireAfterWrite(30, TimeUnit.SECONDS).build();

  public OffsetCommand(SchematicBooksPlugin plugin) {
    super(plugin, "offset", "offset [<x+> <x-> <y+> <y-> <z+> <z->]",
        "Sets the offset of an schematic book");
    this.addPermission(Permission.OFFSET);
  }

  public OffsetCommand register() {
    Bukkit.getPluginManager().registerEvents(this, getPlugin());
    return this;
  }

  private int parseInt(Player player, String name, String arg) throws NumberFormatException {
    try {
      return Integer.parseInt(arg);
    } catch (NumberFormatException nfe) {
      Out.ARG_INVALID_INT_ONLY.send(player, name, arg);
      throw nfe;
    }
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    if (!(sender instanceof Player)) {
      sender.sendMessage("ONLY PLAYERS!");
      return true;
    }

    final Player player = (Player) sender;

    IntVector positiveAxis = new IntVector();
    IntVector negativeAxis = new IntVector();
    if (args.length == 6) {
      try {
        positiveAxis.setX(parseInt(player, "<x+>", args[0]));
        negativeAxis.setX(-parseInt(player, "<x->", args[1]));

        positiveAxis.setY(parseInt(player, "<y+>", args[2]));
        negativeAxis.setY(-parseInt(player, "<y->", args[3]));

        positiveAxis.setZ(parseInt(player, "<z+>", args[4]));
        negativeAxis.setZ(-parseInt(player, "<z->", args[5]));
      } catch (NumberFormatException nfe) {
        return true;
      }

    } else if (args.length != 0) {
      player.sendMessage("§cAll 6 parameters needed!");
      return true;
    }


    this.actions.put(player, new OffsetAction(new IntVectorAxis(negativeAxis, positiveAxis)));

    player.sendMessage(
        "§aTo apply offset click on an existing entity (like info) or place a new one (like place)");

    return true;
  }

  @RequiredArgsConstructor
  private static class OffsetAction {
    private @Getter @NonNull final IntVectorAxis offset;
  }

  @EventHandler
  public void onOtherCommand(PlayerCommandPreprocessEvent event) {
    this.actions.invalidate(event.getPlayer());
  }

  private IntVectorAxis setOffset(SchematicBookInfo info, final IntVectorAxis offset)
      throws IOException {
    final IntVectorAxis world_offset_old = info.getHitBoxOffset(); // in world coordinates

    IntVectorAxis info_offset_diff = offset.subtract(world_offset_old);

    info.setHitBoxOffset(offset);
    try {
      getPlugin().getInfoManager().saveInfo(info);
    } catch (Exception e) {
      info.setHitBoxOffset(world_offset_old);
      throw e;
    }

    return info_offset_diff;
  }

  private void setEntityOffset(final Player player, final World world, final OffsetAction action,
      final SchematicEntity entity) {

    try {
      final SchematicBookInfo info = getPlugin().getInfoManager().getInfo(entity);
      final int rotation = info.getRotation().getYaw() - entity.getRotation().getYaw();
      final IntRegion world_hitbox_old = entity.getHitBox().copy();
      try {

        final IntVectorAxis offset = action.getOffset();
        IntVectorAxis world_offset_diff = setOffset(info, offset.rotate(rotation));

        entity.setHitBox(world_offset_diff.rotate(-rotation).addTo(entity.getHitBox()));
        entity.dirty();
      } catch (IOException e) {
        // UNDO
        entity.setHitBox(world_hitbox_old);
        getPlugin().getLogger().log(Level.SEVERE, "Failed to save schematic info", e);
        player.sendMessage("§cFailed to save schematic information!");

      }

      player.sendMessage("§aNew Offset for §6" + info.getKey() + "\n §aOffset: §d"
          + info.getHitBoxOffset().toString() + "\n §aHitbox: §d" + entity.getHitBox().toString());
      particles(world, entity);

    } catch (ExecutionException e) {
      getPlugin().getLogger().log(Level.SEVERE, "Failed to get schematic info", e);
      player.sendMessage("§cFailed to load schematic information! Was it removed?");
    }
  }

  @EventHandler
  public void onInfoEvent(InfoSchematicEvent event) {
    final Player player = event.getPlayer();
    if (!Permission.OFFSET.checkPermission(player)) {
      return;
    }
    final OffsetAction action = this.actions.getIfPresent(player);

    if (action != null) {
      this.actions.invalidate(player);
      event.setCancelled(true);
      setEntityOffset(player, event.getWorld(), action, event.getEntity());
    }
  }

  @EventHandler
  public void onPlaceEvent(PlaceSchematicEvent event) {
    final Player player = event.getPlayer();
    if (!Permission.OFFSET.checkPermission(player)) {
      return;
    }
    final OffsetAction action = this.actions.getIfPresent(player);

    if (action != null) {
      this.actions.invalidate(player);
      event.setCancelled(true);
      event.getOrigin().setCancelled(true);
      Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
        player.closeInventory();
      }, 1);


      final SchematicBookInfo info = event.getInfo();
      final IntVectorAxis world_offset = action.getOffset();


      // Negative rotation, because we rotate in the other look direction as the player!
      int rotation = info.getRotation().getYaw() - event.getDestinationRotation().getYaw();

      try {
        setOffset(info, world_offset.rotate(rotation));

        player.sendMessage("§aNew Offset for §6" + info.getKey() + "\n §aOffset: §d"
            + info.getHitBoxOffset().toString());

      } catch (IOException e) {
        getPlugin().getLogger().log(Level.SEVERE, "Failed to save schematic info", e);
        player.sendMessage("§cFailed to save schematic information!");
      }
    }
  }


  private void particles(World world, SchematicEntity entity) {
    ParticleUtils.showParticlesForTime(getPlugin(), 20, world, entity, 0, 0, 255);
  }

}
