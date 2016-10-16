package de.mickare.schematicbooks.commands.books;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.session.ClipboardHolder;

import de.mickare.schematicbooks.Interactions;
import de.mickare.schematicbooks.Permission;
import de.mickare.schematicbooks.SchematicBook;
import de.mickare.schematicbooks.SchematicBookInfo;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.AbstractCommand;
import de.mickare.schematicbooks.util.IntVector;
import de.mickare.schematicbooks.util.Rotation;
import de.mickare.schematicbooks.we.WEUtils;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

public class SaveCommand extends AbstractCommand<SchematicBooksPlugin> implements Listener {

  private static final AtomicInteger COUNTER = new AtomicInteger((int) (Math.random() * 10000));
  private static final int UUID_RADIX = Character.MAX_RADIX - 1;
  private final Cache<Player, BookEdit> sessions =
      CacheBuilder.newBuilder().weakKeys().expireAfterWrite(10, TimeUnit.MINUTES).build();

  @Data
  @RequiredArgsConstructor
  private static class BookEdit {
    private @NonNull final UUID playerUUID;
    private @NonNull final Location location;
    private final long timestamp = System.currentTimeMillis();
    private @NonNull final String loreKey;
    private final String permission;
  }


  public SaveCommand(SchematicBooksPlugin plugin) {
    super(plugin, "save", "save <permission>", "Saves a new schematic item");
    this.addPermission(Permission.SAVE);
    registerListener();

  }

  public SaveCommand registerListener() {
    Bukkit.getPluginManager().registerEvents(this, getPlugin());
    return this;
  }

  private ClipboardHolder getClipboard(Player player) {
    try {
      LocalSession session = JavaPlugin.getPlugin(WorldEditPlugin.class).getSession(player);
      if (session != null) {
        ClipboardHolder holder = session.getClipboard();
        if (holder != null) {
          return holder;
        }
      }
    } catch (EmptyClipboardException e) {
    }
    player.sendMessage("§cEmpty clipboard!");
    return null;
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    if (!(sender instanceof Player)) {
      sender.sendMessage("ONLY PLAYERS!");
      return true;
    }

    final Player player = (Player) sender;

    // check if clipbord
    if (getClipboard(player) == null) {
      return true;
    }

    String permission = null;
    if (args.length > 0) {
      permission = args[0];
    }

    giveEditBook(player, permission);
    return true;
  }

  private static String newLoreKey(Player player) {
    String number =
        Integer.toUnsignedString(COUNTER.getAndAdd((int) (13 * Math.random() + 5)) % 0xFFFFFF, 16);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < number.length(); ++i) {
      sb.append("§").append(number.charAt(i));
    }
    return sb.toString();
  }

  public void giveEditBook(final Player player, final String permission) {

    final String loreKey = newLoreKey(player);
    BookEdit edit = new BookEdit(player.getUniqueId(), player.getLocation(), loreKey, permission);

    ItemStack book = new ItemStack(Material.BOOK_AND_QUILL);
    BookMeta meta = (BookMeta) book.getItemMeta();

    meta.setDisplayName("§cSchematic Save");

    meta.setLore(Lists.newArrayList(//
        loreKey, //
        Long.toUnsignedString(player.getUniqueId().getMostSignificantBits(), UUID_RADIX), //
        Long.toUnsignedString(player.getUniqueId().getLeastSignificantBits(), UUID_RADIX)));

    StringBuilder sb1 = new StringBuilder();
    sb1.append("&6==============\n");
    sb1.append("&6 SCHEMATIC \n");
    sb1.append("&6==============\n");
    sb1.append("&9{name}\n");
    sb1.append("&8by\n");
    sb1.append("&9{creator}\n");
    sb1.append("&8{timestamp}\n");
    sb1.append("&8{permission}\n");
    sb1.append("&8Direction: {direction}\n");
    sb1.append("&8Size: {size}\n");

    meta.setPages(Lists.newArrayList(sb1.toString()));

    book.setItemMeta(meta);
    if (player.getInventory().addItem(book).size() == 0) {
      sessions.put(player, edit);
    } else {
      player.sendMessage("§cInventory full!");
    }
  }

  @EventHandler
  public void onEvent(PlayerEditBookEvent event) {
    final Player player = event.getPlayer();
    final BookEdit edit = sessions.getIfPresent(player);
    if (edit == null) {
      return;
    }
    final BookMeta old = event.getPreviousBookMeta();
    final BookMeta meta = event.getNewBookMeta();
    if (isValidEditMeta(player, old, edit)) {

      ClipboardHolder holder = getClipboard(player);
      if (holder == null) {

        meta.setLore(Lists.newArrayList());
        meta.setDisplayName(meta.getTitle());
        event.setNewBookMeta(meta);
        event.setSigning(false);

        sessions.invalidate(player);
        return;
      }

      if (!event.isSigning()) {
        player.sendMessage("§cYou need to sign the book!");
        return;
      }

      String name = meta.getTitle().trim();

      if (name.replaceAll("\\s", "").length() <= 3) {
        player.sendMessage("§cName longer than 3 required!");
        event.setSigning(false);
        return;
      }

      String creator = player.getName();
      Rotation rotation = Rotation.fromYaw(edit.getLocation().getYaw());
      String permission = edit.getPermission();

      List<String> description =
          meta.hasPages() ? Lists.newArrayList(meta.getPages()) : Lists.newArrayList();

      // Format pages
      for (int i = 0; i < description.size(); ++i) {
        String page = description.get(i);
        page = ChatColor.translateAlternateColorCodes('&', page);
        page = page.replace("{name}", name);
        page = page.replace("{creator}", creator);
        page = page.replace("{timestamp}",
            Interactions.DATE_FORMAT.format(new Date(System.currentTimeMillis())));
        page = page.replace("{permission}", (permission == null) ? "" : permission);
        page = page.replace("{direction}", rotation.name());
        page = page.replace("{size}",
            IntVector.from(holder.getClipboard().getDimensions()).toString());
        description.set(i, page);
      }

      SchematicBookInfo info =
          new SchematicBookInfo(name, creator, rotation, description, permission, false, false);

      try {
        File schematicFile = getPlugin().getInfoManager().getSchematicFileOf(info).toFile();
        WEUtils.writeSchematic(holder, schematicFile);
        getPlugin().getInfoManager().saveInfo(info);
        getPlugin().getLogger()
            .info("Player " + player.getName() + " saved a new schematic item \"" + name + "\"");
      } catch (IOException | MaxChangedBlocksException e) {

        meta.setLore(Lists.newArrayList());
        meta.setDisplayName(meta.getTitle());
        event.setNewBookMeta(meta);
        event.setSigning(false);

        player.sendMessage("§cERROR!");
        getPlugin().getLogger().log(Level.SEVERE, "Could not save schematic info", e);

        sessions.invalidate(player);
        return;
      }

      SchematicBook.setBookMeta(info, meta);
      event.setNewBookMeta(meta);
    }
  }

  private boolean isValidEditMeta(Player player, BookMeta meta, BookEdit edit) {
    List<String> lore = meta.getLore();
    if (lore != null && lore.size() == 3) {
      if (edit.getLoreKey().equals(lore.get(0))) {
        boolean result = true;
        result &= Long.toUnsignedString(player.getUniqueId().getMostSignificantBits(), UUID_RADIX)
            .equals(lore.get(1));
        result &= Long.toUnsignedString(player.getUniqueId().getLeastSignificantBits(), UUID_RADIX)
            .equals(lore.get(2));
        return result;
      }
    }
    return false;
  }

}
