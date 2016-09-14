package de.mickare.schematicbooks;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

public class SchematicBook {

  public static int LORE_SIZE = 4;

  private SchematicBook() {}

  private static final BigInteger SIGNATURE_KEY = new BigInteger("36812721879382", 10);
  private static final String LORE_SIGNATURE = "§e§c§0§r§3§a§2§1";

  private static void xorOn(final byte[] out, final byte[] value, final int length) {
    final int len = Math.min(length, value.length);
    for (int i = 0; i < len; ++i) {
      out[i % out.length] ^= value[i];
    }
  }

  private static void xorOn(final byte[] out, final byte[] value) {
    xorOn(out, value, value.length);
  }

  public static final String createSignature(SchematicBookInfo info) {
    String key = Integer.toUnsignedString(
        Objects.hashCode(info.getName(), info.getCreator(), SIGNATURE_KEY) % 0xFFFFFFFF, 16);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < key.length(); ++i) {
      sb.append("§").append(key.charAt(i));
    }
    return sb.toString();
  }

  public static final Optional<String> getSignature(BookMeta meta) {
    List<String> lore = meta.getLore();
    if (lore != null && lore.size() == LORE_SIZE) {
      return Optional.of(lore.get(2));
    }
    return Optional.empty();
  }

  public static Optional<String> getSchematicKey(ItemStack item) {
    if (item.getType() == Material.WRITTEN_BOOK) {
      return getSchematicKey((BookMeta) item.getItemMeta());
    }
    return Optional.empty();
  }


  public static Optional<String> getSchematicKey(BookMeta meta) {
    if (isSchematicBook(meta) && meta.hasTitle() && meta.hasAuthor() && meta.hasPages()) {
      List<String> lore = meta.getLore();
      if (lore.get(1).length() > 0) {
        return Optional.of(lore.get(1));
      }
    }
    return Optional.empty();
  }

  public static boolean isSchematicBook(ItemStack item) {
    if (item.getType() == Material.WRITTEN_BOOK) {
      return isSchematicBook((BookMeta) item.getItemMeta());
    }
    return false;
  }

  public static boolean isSchematicBook(BookMeta meta) {
    List<String> lore = meta.getLore();
    if (lore != null && lore.size() == LORE_SIZE) {
      return LORE_SIGNATURE.equals(lore.get(3));
    }
    return false;
  }

  public static boolean isValidItem(ItemStack item, SchematicBookInfo info) {
    if (item.getType() == Material.WRITTEN_BOOK) {
      return isValidItem((BookMeta) item.getItemMeta(), info);
    }
    return false;
  }

  public static boolean isValidItem(BookMeta meta, SchematicBookInfo info) {
    if (!isSchematicBook(meta)) {
      return false;
    }
    Optional<String> signature = getSignature(meta);
    if (signature.isPresent() && signature.get().equals(createSignature(info))) {
      String key = meta.getLore().get(1);
      return key != null ? key.length() > 0 : false;
    }
    return false;
  }

  public static void setBookMeta(SchematicBookInfo info, BookMeta meta) {
    meta.setTitle("§6" + info.getName());
    meta.setAuthor(info.getCreator());

    meta.setDisplayName("§6" + info.getName());
    List<String> lore = Lists.newArrayList(//
        "Schematic", //
        info.getKey(), //
        createSignature(info), //
        LORE_SIGNATURE);
    meta.setLore(lore);

    List<String> pages = Lists.newArrayList(info.getDescription());
    meta.setPages(pages);
  }

  public static ItemStack createItem(SchematicBookInfo info) {
    ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
    BookMeta meta = (BookMeta) book.getItemMeta();
    setBookMeta(info, meta);
    book.setItemMeta(meta);
    return book;
  }

}
