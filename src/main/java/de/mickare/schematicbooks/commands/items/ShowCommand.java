package de.mickare.schematicbooks.commands.items;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.collect.Sets;

import de.mickare.schematicbooks.Permission;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.AbstractCommand;
import de.mickare.schematicbooks.data.SchematicEntity;
import de.mickare.schematicbooks.data.WorldSchematicEntityCache;
import de.mickare.schematicbooks.util.ParticleUtils;

public class ShowCommand extends AbstractCommand<SchematicBooksPlugin> {

  public ShowCommand(SchematicBooksPlugin plugin) {
    super(plugin, "show", "show [radiusChunks]", "Shows all schematic entities in a chunk radius");
    this.addPermission(Permission.SHOW);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    if (!(sender instanceof Player)) {
      sender.sendMessage("ONLY PLAYERS!");
      return true;
    }

    final Player player = (Player) sender;

    int _radius = 1;
    if (args.length > 0) {
      try {
        _radius = Integer.parseUnsignedInt(args[0]);
      } catch (NumberFormatException nfe) {
        player.sendMessage("§cRadius wrong format!");
        return true;
      }
    }
    if (_radius > 6) {
      player.sendMessage("§cRadius too big!");
      return true;
    }

    final int radius = _radius;

    final Location location = player.getLocation();

    final World world = location.getWorld();
    int chunkX = location.getChunk().getX();
    int chunkZ = location.getChunk().getZ();


    WorldSchematicEntityCache cache = getPlugin().getEntityManager().getCache(world);
    Set<SchematicEntity> entities = Sets.newHashSet();
    for (int x = chunkX - radius; x <= chunkX + radius; ++x) {
      for (int z = chunkZ - radius; z <= chunkZ + radius; ++z) {
        entities.addAll(cache.getChunk(x, z).getEntities().values());
      }
    }
    entities.forEach(e -> ParticleUtils.showParticlesForTime(getPlugin(), 20 * 5, world, e));

    return true;
  }


}
