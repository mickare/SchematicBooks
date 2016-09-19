package de.mickare.schematicbooks.commands.books;

import java.util.Optional;
import java.util.Set;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import de.mickare.schematicbooks.Interactions;
import de.mickare.schematicbooks.Permission;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.AbstractCommand;
import de.mickare.schematicbooks.data.SchematicEntity;
import de.mickare.schematicbooks.util.IntRegion;
import de.mickare.schematicbooks.we.WEUtils;

public class CleanCommand extends AbstractCommand<SchematicBooksPlugin> {

  public CleanCommand(SchematicBooksPlugin plugin) {
    super(plugin, "clean", "clean", "Removes all schematic entities in the world edit selection");
    this.addPermission(Permission.CLEAN);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    if (!(sender instanceof Player)) {
      sender.sendMessage("ONLY PLAYERS!");
      return true;
    }

    final Player player = (Player) sender;
    cleanSelection(player, true, true);

    return true;
  }

  public static void cleanSelection(final Player player, boolean intersecting, boolean verbose) {
    final World world = player.getWorld();

    Optional<IntRegion> oregion = WEUtils.getCuboidSelection(player, world);
    if (oregion.isPresent()) {
      IntRegion region = oregion.get();

      int failed = 0;
      int success = 0;

      Set<SchematicEntity> entities;
      if (intersecting) {
        entities = JavaPlugin.getPlugin(SchematicBooksPlugin.class).getEntityManager()
            .getCache(world).getEntitiesIntesecting(region);
      } else {
        entities = JavaPlugin.getPlugin(SchematicBooksPlugin.class).getEntityManager()
            .getCache(world).getEntitiesContaining(region);
      }

      for (SchematicEntity entity : entities) {
        if (Interactions.removeEntity(player, world, entity).isPresent()) {
          success++;
        } else {
          failed++;
        }
      }

      if (success > 0) {
        player.sendMessage("§aRemoved " + success + " schematic entities successfully!");
      }
      if (failed > 0) {
        player.sendMessage("§cFailed to remove " + failed + " schematic entities!");
      }
      if (verbose) {
        if (failed == 0 && success == 0) {
          player.sendMessage("§aNo schematic entity removed!");
        }
      }
    } else if (verbose) {
      player.sendMessage("§cNo World-Edit selection in this world!");
    }
  }

}
