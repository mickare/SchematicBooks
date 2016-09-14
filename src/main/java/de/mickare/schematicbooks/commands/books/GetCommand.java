package de.mickare.schematicbooks.commands.books;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.google.common.collect.Lists;

import de.mickare.schematicbooks.Permission;
import de.mickare.schematicbooks.SchematicBook;
import de.mickare.schematicbooks.SchematicBookInfo;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.AbstractCommand;

public class GetCommand extends AbstractCommand<SchematicBooksPlugin> implements TabCompleter {

  public GetCommand(SchematicBooksPlugin plugin) {
    super(plugin, "get", "get <name> <amount>", "Gets an existing schematic item");
    this.addPermission(Permission.GET);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    if (!(sender instanceof Player)) {
      sender.sendMessage("ONLY PLAYERS!");
      return true;
    }

    final Player player = (Player) sender;

    if (args.length < 1) {
      player.sendMessage("§cSchematicItem name missing!");
      return true;
    }
    String key = args[0].toLowerCase();


    int amount = 1;
    if (args.length >= 2) {
      try {
        amount = Integer.parseInt(args[1]);
      } catch (NumberFormatException nfe) {
        player.sendMessage("§cAmount wrong format!");
        return true;
      }
    }

    try {
      SchematicBookInfo info = getPlugin().getInfoManager().getInfo(key);
      ItemStack item = SchematicBook.createItem(info);
      item.setAmount(amount);
      player.getInventory().addItem(item);
    } catch (ExecutionException e) {
      getPlugin().getLogger().log(Level.WARNING, "Could not load schematic item info " + key, e);
      player.sendMessage("§cERROR! Could not load the info!");
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias,
      String[] args) {
    if (args.length == 0) {
      return Lists.newArrayList(getPlugin().getInfoManager().getInfos().keySet());
    }
    if (args.length == 1) {
      final String search = args[0].toLowerCase();
      return getPlugin().getInfoManager().getInfos().keySet().stream()
          .filter(k -> k.contains(search)).collect(Collectors.toList());
    }
    return null;
  }

}
