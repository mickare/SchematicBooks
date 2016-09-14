package de.mickare.schematicbooks;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

import org.bukkit.command.CommandSender;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

public enum Out {

  ERROR_CMD_ONLY_PLAYERS,
  ERROR_INTERNAL,

  PERMISSION_MISSING,
  PERMISSION_MISSING_EXTENSION,

  ARG_MISSING,
  ARG_INVALID,
  ARG_INVALID_INT_ONLY,
  ARG_INVALID_INT_MIN,
  ARG_INVALID_INT_MAX,
  ARG_INVALID_NAME_LENGTH,
  ARG_INVALID_NO_MATERIAL,

  CMD_MENU_HELP,
  CMD_MENU_HELP_LINE,
  CMD_MENU_HELP_NONE,
  CMD_MENU_UNKNOWN,
  
  SCHEMATIC_PICKED_UP,

  ;

  public String get(final Object... args) {
    return getMessage(this.name(), args);
  }

  public void send(final CommandSender receiver, final Object... args) {
    sendMessage(receiver, this.name(), args);
  }

  public String toString() {
    if (resource != null && resource.containsKey(this.name())) {
      return resource.getString(this.name());
    }
    return super.toString();
  }

  private static @Getter @Setter @NonNull Locale locale = Locale.getDefault();
  private static @Getter @NonNull ResourceBundle resource = null;
  private static final ThreadLocal<MessageFormat> format =
      ThreadLocal.withInitial(() -> new MessageFormat("", locale));

  public static void setResource(ResourceBundle resource) {
    for (Out o : Out.values()) {
      if (!resource.containsKey(o.name())) {
        System.err.println("bundle does not contain: " + o.name());
      }
    }
    Out.resource = resource;
  }

  public static String getMessage(final String key, final Object... args) {
    final String msg = resource.getString(key);
    if (args.length == 0) {
      return msg;
    }
    final MessageFormat f = format.get();
    f.applyPattern(msg);
    return f.format(args);
  }

  public static void sendMessage(final CommandSender receiver, final String key,
      final Object... args) {
    receiver.sendMessage(getMessage(key, args));
  }


}
