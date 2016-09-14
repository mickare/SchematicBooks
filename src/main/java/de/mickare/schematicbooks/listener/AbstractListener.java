package de.mickare.schematicbooks.listener;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import de.mickare.schematicbooks.SchematicBooksPlugin;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AbstractListener implements Listener {


  private @Getter @NonNull final SchematicBooksPlugin plugin;

  public void register() {
    Bukkit.getPluginManager().registerEvents(this, plugin);
  }

}
