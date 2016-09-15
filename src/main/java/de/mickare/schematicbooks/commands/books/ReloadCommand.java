package de.mickare.schematicbooks.commands.books;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import de.mickare.schematicbooks.Permission;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.AbstractCommand;

public class ReloadCommand extends AbstractCommand<SchematicBooksPlugin> {

  public ReloadCommand(SchematicBooksPlugin plugin) {
    super(plugin, "reload", "reload", "Reloads all books and schematic entities");
    this.addPermission(Permission.RELOAD);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    getPlugin().getEntityManager().invalidateAll();
    getPlugin().getInfoManager().invalidateAll();
    getPlugin().getInfoManager().loadAllInfoFiles();
    getPlugin().preloadLoadedChunks();

    sender.sendMessage("§aReloaded!");

    return true;
  }


}
