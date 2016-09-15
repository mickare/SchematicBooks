package de.mickare.schematicbooks.commands;

import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.books.GetCommand;
import de.mickare.schematicbooks.commands.books.ListCommand;
import de.mickare.schematicbooks.commands.books.ReloadCommand;
import de.mickare.schematicbooks.commands.books.SaveCommand;
import de.mickare.schematicbooks.commands.books.ShowCommand;

public class MainSchematicItemsCommand extends AbstractMainMenuCommand<SchematicBooksPlugin> {

  public static final String CMD = "sbook";

  public MainSchematicItemsCommand(SchematicBooksPlugin plugin) {
    super(plugin, plugin.getCommand(CMD));
  }

  @Override
  public MainSchematicItemsCommand register() {
    super.register();

    SchematicBooksPlugin plugin = this.getPlugin();

    this.setCommand(new SaveCommand(plugin));
    this.setCommand(new GetCommand(plugin));
    this.setCommand(new ShowCommand(plugin));
    this.setCommand(new ReloadCommand(plugin));
    this.setCommand(new ListCommand(plugin));

    return this;
  }

}
