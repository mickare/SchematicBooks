package de.mickare.schematicbooks.util;

import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class BukkitReflect {

  public static final String VERSION = Bukkit.getServer().getClass().getName().split("\\.")[3];

  public static String getNMSClassName(String name) {
    return "net.minecraft.server." + VERSION + "." + name;
  }

  public static Class<?> getCraftBukkitClass(String name) throws ClassNotFoundException {
    return Class.forName("org.bukkit.craftbukkit." + VERSION + "." + name);
  }


  public static Class<?> getNMSClass(String name) throws ClassNotFoundException {
    return Class.forName(getNMSClassName(name));
  }

  /**
   * Converts an {@link org.bukkit.inventory.ItemStack} to a Json string for sending with
   * {@link net.md_5.bungee.api.chat.BaseComponent}'s.
   *
   * @param itemStack the item to convert
   * @return the Json string representation of the item
   */
  public static String convertItemStackToJson(ItemStack itemStack) {

    try {
      // ItemStack methods to get a net.minecraft.server.ItemStack object for serialization
      Class<?> craftItemStackClazz = getCraftBukkitClass("inventory.CraftItemStack");
      Method asNMSCopyMethod = craftItemStackClazz.getMethod("asNMSCopy", ItemStack.class);

      // NMS Method to serialize a net.minecraft.server.ItemStack to a valid Json string
      Class<?> nmsItemStackClazz = getNMSClass("ItemStack");
      Class<?> nbtTagCompoundClazz = getNMSClass("NBTTagCompound");
      Method saveNmsItemStackMethod = nmsItemStackClazz.getMethod("save", nbtTagCompoundClazz);

      Object nmsNbtTagCompoundObj; // This will just be an empty NBTTagCompound instance to invoke
                                   // the
                                   // saveNms method
      Object nmsItemStackObj; // This is the net.minecraft.server.ItemStack object received from the
                              // asNMSCopy method
      Object itemAsJsonObject; // This is the net.minecraft.server.ItemStack after being put through
                               // saveNmsItem method

      nmsNbtTagCompoundObj = nbtTagCompoundClazz.newInstance();
      nmsItemStackObj = asNMSCopyMethod.invoke(null, itemStack);
      itemAsJsonObject = saveNmsItemStackMethod.invoke(nmsItemStackObj, nmsNbtTagCompoundObj);

      // Return a string representation of the serialized object
      return itemAsJsonObject.toString();
    } catch (Throwable t) {
      Bukkit.getLogger().log(Level.SEVERE, "failed to serialize itemstack to nms item", t);
      return null;
    }

  }

}
