package de.mickare.schematicbooks.listener;

import org.bukkit.event.EventHandler;

import de.mickare.armortools.event.ArmorstandModifyEvent;
import de.mickare.schematicbooks.SchematicBooksPlugin;

public class ArmorToolListener extends AbstractListener {

  public ArmorToolListener(SchematicBooksPlugin plugin) {
    super(plugin);
  }

  @EventHandler
  public void onArmorModify(ArmorstandModifyEvent event) {
    if (getPlugin().getEntityManager().getEntityOf(event.getEntity()).isPresent()) {
      event.setCancelled(true);
    }
  }


}
