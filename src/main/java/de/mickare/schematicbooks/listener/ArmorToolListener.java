package de.mickare.schematicbooks.listener;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;

import de.mickare.armortools.event.ArmorMoveEvent;
import de.mickare.armortools.event.ArmorRotateEvent;
import de.mickare.armortools.event.ArmorstandModifyEvent;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.data.SchematicEntity;
import net.md_5.bungee.api.ChatColor;

public class ArmorToolListener extends AbstractListener {

  public ArmorToolListener(SchematicBooksPlugin plugin) {
    super(plugin);
  }

  @EventHandler
  public void onArmorModify(ArmorstandModifyEvent event) {

    switch (event.getAction().getType()) {
      case MOVE:
        handleMoveModifyEvent(event);
        break;
      case ROTATE:
        handleRotateModifyEvent(event);
        break;
      default:
        if (event.getEntities().values().stream()//
            .map(e -> getPlugin().getEntityManager().getEntityOf(e))//
            .anyMatch(e -> e.isPresent())) {
          event.setCancelled(true);
          return;
        }
    }

  }

  private void handleMoveModifyEvent(ArmorstandModifyEvent event) {
    final Set<SchematicEntity> entities = event.getEntities().values().stream()//
        .map(e -> getPlugin().getEntityManager().getEntityOf(e))//
        .filter(e -> e.isPresent()).map(e -> e.get()).collect(Collectors.toSet());

    if (entities.stream().anyMatch(e -> !e.isMovable())) {
      event.setCancelled(true);
      return;
    }

    final Set<UUID> entityUUIDs =
        entities.stream().flatMap(e -> e.getEntities().stream()).collect(Collectors.toSet());

    final World world = event.getPlayer().getWorld();
    final Set<ArmorStand> armorstands = world.getEntitiesByClass(ArmorStand.class).stream()//
        .filter(a -> entityUUIDs.contains(a.getUniqueId()))//
        .collect(Collectors.toSet());

    event.addAllEntities(armorstands);
  }

  private void handleRotateModifyEvent(ArmorstandModifyEvent event) {
    final Set<SchematicEntity> entities = event.getEntities().values().stream()//
        .map(e -> getPlugin().getEntityManager().getEntityOf(e))//
        .filter(e -> e.isPresent()).map(e -> e.get()).collect(Collectors.toSet());

    if (entities.stream().anyMatch(e -> !e.isRotatable())) {
      event.setCancelled(true);
      return;
    }

    final Set<UUID> entityUUIDs =
        entities.stream().flatMap(e -> e.getEntities().stream()).collect(Collectors.toSet());

    final World world = event.getPlayer().getWorld();
    final Set<ArmorStand> armorstands = world.getEntitiesByClass(ArmorStand.class).stream()//
        .filter(a -> entityUUIDs.contains(a.getUniqueId()))//
        .collect(Collectors.toSet());

    event.addAllEntities(armorstands);
  }

  @EventHandler
  public void onArmorMove(ArmorMoveEvent event) {

    Set<SchematicEntity> entities = event.getAction().getArmorstands().stream()//
        .map(e -> getPlugin().getEntityManager().getEntityOf(e))//
        .filter(e -> e.isPresent()).map(e -> e.get())//
        .collect(Collectors.toSet());

    for (SchematicEntity e : entities) {
      Vector moved = e.getMoved().clone().add(event.getMoved());
      event.getPlayer().sendMessage(ChatColor.RED + "Moved: " + moved.toString());
      if (moved.lengthSquared() > 1) {
        event.setResult(ArmorMoveEvent.Result.CANCEL);
        return;
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
