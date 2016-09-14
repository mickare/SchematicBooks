package de.mickare.schematicbooks.commands;

import de.mickare.schematicbooks.SchematicBooksPlugin;
import de.mickare.schematicbooks.commands.items.GetCommand;
import de.mickare.schematicbooks.commands.items.SaveCommand;
import de.mickare.schematicbooks.commands.items.ShowCommand;

public class MainSchematicItemsCommand extends AbstractMainMenuCommand<SchematicBooksPlugin> {

  public MainSchematicItemsCommand(SchematicBooksPlugin plugin) {
    super(plugin, plugin.getCommand("sitem"));
  }

  @Override
  public MainSchematicItemsCommand register() {
    super.register();

    SchematicBooksPlugin plugin = this.getPlugin();

    this.setCommand(new SaveCommand(plugin));
    this.setCommand(new GetCommand(plugin));
    this.setCommand(new ShowCommand(plugin));

    return this;
  }

}
