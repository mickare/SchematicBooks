package de.mickare.schematicbooks.util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.inventory.meta.BookMeta;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import de.mickare.schematicbooks.reflection.ReflectUtils;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;

public class BetterBookMeta {

  private static Class<?> CraftMetaBook;
  private static Field CraftMetaBook_pages;
  private static Class<?> IChatBaseComponent;
  private static Class<?> ChatSerializer;
  private static Method ChatSerializer_toJson;
  private static Method ChatSerializer_fromJson;

  static {
    try {
      CraftMetaBook = BukkitReflect.getCraftBukkitClass("inventory.CraftMetaBook");
      CraftMetaBook_pages = CraftMetaBook.getField("pages");
      CraftMetaBook_pages.setAccessible(true);
      IChatBaseComponent = BukkitReflect.getNMSClass("IChatBaseComponent");
      ChatSerializer = BukkitReflect.getNMSClass("IChatBaseComponent.ChatSerializer");

      ChatSerializer_toJson =
          ReflectUtils.getFirstStaticMethod(ChatSerializer, String.class, IChatBaseComponent);
      ChatSerializer_fromJson =
          ReflectUtils.getFirstStaticMethod(ChatSerializer, IChatBaseComponent, String.class);

    } catch (ClassNotFoundException | NoSuchFieldException | SecurityException e) {
      throw new RuntimeException();
    }
  }

  public static List<String> getJsonPages(BookMeta meta)
      throws IllegalArgumentException, IllegalAccessException {
    if (!CraftMetaBook.isInstance(meta)) {
      throw new UnsupportedOperationException();
    }
    List<?> pages = (List<?>) CraftMetaBook_pages.get(meta);
    List<String> result = Lists.newArrayListWithExpectedSize(pages.size());
    for (Object page : pages) {
      result.add(toJson(page));
    }
    return result;
  }

  private static <T> void setJsonPages(BookMeta meta, List<String> pages, Class<T> c)
      throws IllegalArgumentException, IllegalAccessException {
    if (!CraftMetaBook.isInstance(meta)) {
      throw new UnsupportedOperationException();
    }
    List<T> result = Lists.newArrayListWithExpectedSize(pages.size());
    for (String page : pages) {
      result.add(fromJson(page, c));
    }
    CraftMetaBook_pages.set(meta, result);
  }

  public static void setJsonPages(BookMeta meta, List<String> pages)
      throws IllegalArgumentException, IllegalAccessException {
    setJsonPages(meta, pages, IChatBaseComponent);
  }


  public static void setPages(BookMeta meta, BaseComponent[]... pages)
      throws IllegalArgumentException, IllegalAccessException {
    List<String> json = Lists.newArrayListWithExpectedSize(pages.length);
    for (BaseComponent[] page : pages) {
      json.add(ComponentSerializer.toString(page));
    }
    setJsonPages(meta, json);
  }

  private static String toJson(Object page) {
    Preconditions.checkNotNull(page);
    Preconditions.checkArgument(IChatBaseComponent.isInstance(page));
    try {
      return (String) ChatSerializer_toJson.invoke(null, page);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }


  @SuppressWarnings("unchecked")
  private static <T> T fromJson(String page, Class<T> c) {
    Preconditions.checkNotNull(page);
    try {
      return (T) ChatSerializer_fromJson.invoke(null, page);
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }


}
