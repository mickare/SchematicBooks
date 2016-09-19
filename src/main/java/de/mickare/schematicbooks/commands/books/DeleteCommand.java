package de.mickare.schematicbooks.commands.books;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import de.mickare.schematicbooks.Permission;
import de.mickare.schematicbooks.SchematicBookInfo;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.AbstractCommand;

public class DeleteCommand extends AbstractCommand<SchematicBooksPlugin> implements TabCompleter {

  public DeleteCommand(SchematicBooksPlugin plugin) {
    super(plugin, "delete", "delete <name>", "Deletes a schematic book");
    this.addPermission(Permission.DELETE);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    if (args.length < 1) {
      sender.sendMessage("§cSchematicItem name missing!");
      return true;
    }
    String key = args[0].toLowerCase();

    try {
      SchematicBookInfo info = getPlugin().getInfoManager().getInfo(key);
      if (info == null) {
        sender.sendMessage("§cSchematic book \"" + key + "\" not found!");
        return true;
      }

      boolean infoFile = Files.deleteIfExists(getPlugin().getInfoManager().getInfoFileOf(info));
      boolean schematicFile =
          Files.deleteIfExists(getPlugin().getInfoManager().getSchematicFileOf(info));
      getPlugin().getInfoManager().invalidateInfo(key);

      if (infoFile || schematicFile) {
        sender.sendMessage("§aSchematic book \"" + key + "\" deleted!");
      }

    } catch (ExecutionException e) {
      getPlugin().getLogger().log(Level.WARNING, "Could not load schematic item info " + key);
      sender.sendMessage("§cERROR! Could not load the info!");
    } catch (IOException e) {
      getPlugin().getLogger().log(Level.WARNING, "Could not delete schematic item files " + key, e);
      sender.sendMessage("§cERROR! Could not delete the files!");
    }

    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias,
      String[] args) {
    if (args.length == 0) {
      return getPlugin().getInfoManager().getAllInfos().stream()//
          .map(e -> e.getKey())//
          .collect(Collectors.toList());
    }
    if (args.length == 1) {
      final String search = args[0].toLowerCase();

      return getPlugin().getInfoManager().getAllInfos().stream()//
          .map(e -> e.getKey())//
          .filter(k -> k.contains(search))//
          .collect(Collectors.toList());
    }
    return null;
  }

}
