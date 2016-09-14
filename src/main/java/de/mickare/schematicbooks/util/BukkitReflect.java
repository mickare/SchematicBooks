package de.mickare.schematicbooks.util;

import org.bukkit.Bukkit;

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


}
