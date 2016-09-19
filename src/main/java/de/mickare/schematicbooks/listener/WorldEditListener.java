package de.mickare.schematicbooks.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.books.CleanCommand;

public class WorldEditListener extends AbstractListener {


  public WorldEditListener(SchematicBooksPlugin plugin) {
    super(plugin);
  }

  @EventHandler
  public void onSetCommand(PlayerCommandPreprocessEvent event) {
    String msg = event.getMessage().toLowerCase();
    if (msg.startsWith("//set ")) {
      CleanCommand.cleanSelection(event.getPlayer(), false, false);
    }
  }

}
