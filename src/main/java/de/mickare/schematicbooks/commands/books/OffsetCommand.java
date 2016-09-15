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
        negativeAxis.setX(parseInt(player, "<x->", args[0]));

        positiveAxis.setY(parseInt(player, "<y+>", args[0]));
        negativeAxis.setY(parseInt(player, "<y->", args[0]));

        positiveAxis.setZ(parseInt(player, "<z+>", args[4]));
        negativeAxis.setZ(parseInt(player, "<z->", args[5]));
      } catch (NumberFormatException nfe) {
        return true;
      }

    } else if (args.length != 0) {
      player.sendMessage("§cAll 6 parameters needed!");
      return true;
    }


    this.actions.put(player, new OffsetAction(positiveAxis, negativeAxis));

    player.sendMessage("§aClick an existing entity with a book. §7(As if for Info)");

    return true;
  }

  @RequiredArgsConstructor
  private static class OffsetAction {
    private @Getter @NonNull final IntVector positiveAxis;
    private @Getter @NonNull final IntVector negativeAxis;
  }

  @EventHandler
  public void onOtherCommand(PlayerCommandPreprocessEvent event) {
    this.actions.invalidate(event.getPlayer());
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
      try {
        SchematicBookInfo info = getPlugin().getInfoManager().getInfo(event.getEntity());

        final IntVectorAxis infoOldOffset = info.getHitBoxOffset();
        IntVectorAxis infoNewOffset =
            new IntVectorAxis(action.getNegativeAxis(), action.getPositiveAxis());
        IntVectorAxis infoDiffOffset = infoNewOffset.subtract(infoOldOffset);

        final IntRegion entityOldHitBox = event.getEntity().getHitBox().copy();


        infoDiffOffset.addTo(event.getEntity().getHitBox());
        event.getEntity().dirty();
        info.setHitBoxOffset(infoNewOffset);

        try {
          getPlugin().getInfoManager().saveInfo(info);
        } catch (IOException e) {
          info.setHitBoxOffset(infoOldOffset);
          event.getEntity().setHitBox(entityOldHitBox);
          getPlugin().getLogger().log(Level.SEVERE, "Failed to save schematic info", e);
          player.sendMessage("§cFailed to save schematic information!");

        }

        player.sendMessage("§aOffset: " + info.getHitBoxOffset().toString() + "\n§aHitbox: "
            + event.getEntity().getHitBox().toString());
        particles(event.getWorld(), event.getEntity());

      } catch (ExecutionException e) {
        getPlugin().getLogger().log(Level.SEVERE, "Failed to get schematic info", e);
        player.sendMessage("§cFailed to load schematic information! Was it removed?");
      }
    }
  }

  private void particles(World world, SchematicEntity entity) {
    ParticleUtils.showParticlesForTime(getPlugin(), 20, world, entity, 0, 0, 255);
  }

}
