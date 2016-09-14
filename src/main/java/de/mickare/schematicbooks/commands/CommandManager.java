package de.mickare.schematicbooks.commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.Plugin;

import de.mickare.schematicbooks.SchematicBooksPlugin;

public class CommandManager {

  public static CommandMap getBukkitCommandMap() {
    try {
      final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
      bukkitCommandMap.setAccessible(true);
      return (CommandMap) bukkitCommandMap.get(bukkitCommandMap);
    } catch (NoSuchFieldException | SecurityException | IllegalArgumentException
        | IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  private static Constructor<PluginCommand> pluginConstr()
      throws NoSuchMethodException, SecurityException {
    return PluginCommand.class.getConstructor(String.class, Plugin.class);
  }

  private static PluginCommand newCommand(String name, Plugin plugin) {
    try {
      return pluginConstr().newInstance(name, plugin);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
        | InvocationTargetException | NoSuchMethodException | SecurityException e) {
      throw new RuntimeException(e);
    }
  }

  public static void registerCommand(final SchematicBooksPlugin plugin) {
    final CommandMap map = getBukkitCommandMap();
    newCommand("save", plugin).register(map);
  }



}
