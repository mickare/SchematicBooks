package de.mickare.schematicbooks.listener;

import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;

import de.mickare.armortools.event.ArmorMoveEvent;
import de.mickare.armortools.event.ArmorRotateEvent;
import de.mickare.armortools.event.ArmorstandModifyEvent;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.data.SchematicEntity;

public class ArmorToolListener extends AbstractListener {

  public ArmorToolListener(SchematicBooksPlugin plugin) {
    super(plugin);
  }

  @EventHandler
  public void onArmorModify(ArmorstandModifyEvent event) {

    switch (event.getAction().getType()) {
      case MOVE:

        if (event.getEntities().stream()//
            .map(e -> getPlugin().getEntityManager().getEntityOf(e))//
            .filter(e -> e.isPresent()).map(e -> e.get())//
            .anyMatch(e -> !e.isMovable())) {
          event.setCancelled(true);
        }

        break;
      case ROTATE:

        if (event.getEntities().stream()//
            .map(e -> getPlugin().getEntityManager().getEntityOf(e))//
            .filter(e -> e.isPresent()).map(e -> e.get())//
            .anyMatch(e -> !e.isRotatable())) {
          event.setCancelled(true);
        }

        break;
      default:
        if (event.getEntities().stream()//
            .map(e -> getPlugin().getEntityManager().getEntityOf(e))//
            .anyMatch(e -> e.isPresent())) {
          event.setCancelled(true);
        }
    }

  }

  @EventHandler
  public void onArmorMove(ArmorMoveEvent event) {

    Set<SchematicEntity> entities = event.getAction().getArmorstands().stream()//
        .map(e -> getPlugin().getEntityManager().getEntityOf(e))//
        .filter(e -> e.isPresent()).map(e -> e.get())//
        .collect(Collectors.toSet());

    for (SchematicEntity e : entities) {
      Vector moved = e.getMoved().clone().add(event.getMoved());
      if (moved.lengthSquared() >= 1) {
        event.setCancelled(true);
        break;
      }
    }

    for (SchematicEntity e : entities) {
      e.getMoved().add(event.getMoved());
      e.dirty();
    }

  }

  @EventHandler
  public void onArmorRotate(ArmorRotateEvent event) {

  }
}
