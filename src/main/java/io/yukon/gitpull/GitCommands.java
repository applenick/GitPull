package io.yukon.gitpull;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

@CommandAlias("git")
@Description("Commands for managing a git repo.")
public class GitCommands extends BaseCommand {

  @Dependency private GitPull plugin;

  @Dependency private Repository repo;

  @Subcommand("reload")
  @Description("Reloads GitPull config")
  @CommandPermission("gitpull.reload")
  public void reload(CommandSender sender) throws CommandException {
    plugin.reloadConfig();
    sender.sendMessage(ChatColor.GREEN + "GitPull config reloaded!");
  }

  @CommandAlias("pull") // Command Alias so we can do /git pull or /pull
  @Subcommand("pull|update")
  @Description("Pulls the latest remote repo")
  @CommandPermission("gitpull.reload")
  public void pull(final CommandSender sender) throws CommandException {
    sender.sendMessage(
        ChatColor.GRAY
            + "Now pulling "
            + ChatColor.GREEN
            + plugin.repoName()
            + ChatColor.GRAY
            + "...");
    plugin
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            plugin,
            new Runnable() {
              @Override
              public void run() {
                pullRepo(sender);
              }
            });
  }

  @HelpCommand
  public void git(CommandSender sender, CommandHelp help) {
    help.showHelp();
  }

  private void pullRepo(CommandSender sender) {
    try {
      Git git = new Git(repo);
      git.pull().call();
      RevCommit commit = git.log().setMaxCount(1).call().iterator().next();
      sender.sendMessage(
          ChatColor.GREEN + plugin.repoName() + ChatColor.GRAY + " was successfully pulled!");
      sender.sendMessage(
          ChatColor.GRAY
              + "Commit by "
              + ChatColor.AQUA
              + commit.getAuthorIdent().getName()
              + ChatColor.GRAY
              + ": "
              + ChatColor.YELLOW
              + commit.getShortMessage());
      git.close();
    } catch (GitAPIException e) {
      e.printStackTrace();
      throw new CommandException(
          "An error has occurred, repo not updated.\n" + e.getCause().getMessage());
    }
  }
}
