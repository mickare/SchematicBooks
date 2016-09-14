package de.mickare.schematicbooks.listener;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import de.mickare.schematicbooks.Interactions;
import de.mickare.schematicbooks.SchematicBooksPlugin;

public class InteractListener extends AbstractListener {

  public InteractListener(final SchematicBooksPlugin plugin) {
    super(plugin);
  }


  @EventHandler(priority = EventPriority.HIGH)
  public void onDamage(final EntityDamageEvent event) {
    getPlugin().getEntityManager().getEntityOf(event.getEntity())
        .ifPresent(g -> event.setCancelled(true));
  }

  @EventHandler
  public void onInteractBookPlayerDamage(final EntityDamageByEntityEvent event) {
    if (event.getDamager() instanceof Player) {
      final Player player = (Player) event.getDamager();

      Interactions.pickup(event, player, event.getEntity());
    }
  }

  @EventHandler
  public void onInteractBook(final PlayerInteractAtEntityEvent event) {
    Interactions.showInfo(event, event.getPlayer(), event.getRightClicked());
  }

  @EventHandler
  public void onInteractBook(final PlayerInteractEntityEvent event) {
    Interactions.showInfo(event, event.getPlayer(), event.getRightClicked());
  }



  @EventHandler
  public void onClick(PlayerInteractEvent event) {

    final Player player = event.getPlayer();
    if (player.getGameMode() == GameMode.ADVENTURE || player.getGameMode() == GameMode.SPECTATOR) {
      return;
    }
    ItemStack item = player.getInventory().getItemInMainHand();
    if (item != null) {

      if (item.getType() == Material.BOOK) {

        if (event.getAction() == Action.RIGHT_CLICK_AIR //
            || event.getAction() == Action.RIGHT_CLICK_BLOCK) {

          Interactions.showInfoInSight(event, player);

        } else if (event.getAction() == Action.LEFT_CLICK_AIR //
            || event.getAction() == Action.LEFT_CLICK_BLOCK) {

          Interactions.pickupInSight(event, player);

        }

      } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK
          && item.getType() == Material.WRITTEN_BOOK) {

        Interactions.place(event, player, item);

      }
    }
  }

}
