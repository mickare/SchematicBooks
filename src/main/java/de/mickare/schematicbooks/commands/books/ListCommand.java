package de.mickare.schematicbooks.commands.books;

import java.util.Collection;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import de.mickare.schematicbooks.Out;
import de.mickare.schematicbooks.Permission;
import de.mickare.schematicbooks.SchematicBookInfo;
import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.AbstractCommand;
import de.mickare.schematicbooks.commands.MainSchematicItemsCommand;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ListCommand extends AbstractCommand<SchematicBooksPlugin> {

  private static final int PAGE_SIZE = 20;

  public ListCommand(SchematicBooksPlugin plugin) {
    super(plugin, "list", "list [all] <page>", "Lists all books schematics");
    this.addPermission(Permission.LIST);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

    int argOffset = 0;
    boolean isAll = false;

    Collection<SchematicBookInfo> all = getPlugin().getInfoManager().getAllInfos();

    List<SchematicBookInfo> list = Lists.newArrayList();

    if (args.length > argOffset && args[argOffset + 0].equalsIgnoreCase("all")) {
      if (!Permission.LIST_ALL.checkPermission(sender)) {
        Out.PERMISSION_MISSING_EXTENSION.send(sender, "all");
        return true;
      }
      isAll = true;
      list.addAll(all);
      argOffset++;
    } else {
      all.stream().filter(info -> info.checkPermission(sender)).forEach(list::add);
    }


    if (list.isEmpty()) {
      sender.sendMessage("§7Empty list.");
      return true;
    }

    int page = 1;

    if (args.length > argOffset) {
      try {
        page = Integer.parseUnsignedInt(args[argOffset + 0]);
      } catch (NumberFormatException nfe) {
        sender.sendMessage("§cInvalid page number!");
        return true;
      }
    }
    page = Math.max(0, page - 1);
    final int pageLimit = (list.size() - 1) / PAGE_SIZE;
    page = Math.min(page, pageLimit);

    int index_from = page * PAGE_SIZE;
    int index_to = Math.min(index_from + PAGE_SIZE, list.size());

    ComponentBuilder cb = new ComponentBuilder("§6" + (isAll ? "All " : "") + "Schematic Books ("
        + (page + 1) + " / " + (pageLimit + 1) + ")");


    final boolean hasGetPerm = Permission.GET.checkPermission(sender);

    List<SchematicBookInfo> sublist = list.subList(index_from, index_to);
    int keyMaxLength = Math.max(10,
        Math.min(40, sublist.stream().mapToInt(e -> e.getKey().length()).max().orElse(1)));
    for (SchematicBookInfo info : sublist) {

      String permission = info.hasPermission() ? " §cP" : "";
      cb.append("\n   ")
          .append("§2" + Strings.padEnd(info.getKey(), keyMaxLength, ' ') + permission);
      cb.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, createHover(info, hasGetPerm)));
      if (hasGetPerm) {
        cb.event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
            "/" + MainSchematicItemsCommand.CMD + " get " + info.getKey()));
      }
    }

    if (sender instanceof Player) {
      ((Player) sender).spigot().sendMessage(cb.create());
    } else {
      sender.sendMessage(TextComponent.toLegacyText(cb.create()));
    }

    return true;
  }

  private static BaseComponent[] createHover(SchematicBookInfo info, boolean hasGetPerm) {
    ComponentBuilder cb =
        new ComponentBuilder("§6" + info.getName() + " §7by §f" + info.getCreator());
    if (info.hasPermission()) {
      cb.append("\n\n§cPermission required\n§d" + info.getPermission());
    }
    if (hasGetPerm) {
      cb.append("\n\n§e>> §oClick for get command§e <<");
    }
    return cb.create();
  }


}
