package io.yukon.gitpull;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.CommandHelp;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Dependency;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.HelpCommand;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import com.google.common.collect.Lists;
import io.yukon.gitpull.GitPull.CachedRepo;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Optional;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandException;
import org.bukkit.command.CommandSender;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

@CommandAlias("git|gitpull")
@Description("Commands for managing a git repo.")
public class GitCommands extends BaseCommand {

  public static final String DIV = ChatColor.GOLD + "\u00BB ";

  @Dependency private GitPull plugin;

  @Subcommand("reload|reloadconfig")
  @Description("Reloads GitPull config")
  @CommandPermission("gitpull.reload")
  public void reload(final CommandSender sender) throws CommandException {
    plugin.reloadConfig();
    sender.sendMessage(ChatColor.GREEN + "GitPull config reloaded!");
  }

  @Subcommand("status")
  @Description("View repo status of an individual repo")
  @CommandPermission("gitpull.status")
  @CommandCompletion("@repos")
  @Syntax("[repo] - Name of the target repo")
  public void status(final CommandSender sender, String repoName) {
    Optional<CachedRepo> repo = plugin.getRepo(repoName);

    if (repo.isPresent()) {
      plugin
          .getServer()
          .getScheduler()
          .runTaskAsynchronously(
              plugin,
              new Runnable() {
                @Override
                public void run() {
                  sendRepoStatus(sender, repo.get(), false);
                }
              });
    } else {
      sender.sendMessage(format("&cNo repo with name &e%s&c was found!", repo));
    }
  }

  @Subcommand("list|ls")
  @Description("View a list of loaded repos")
  @CommandPermission("gitpull.list")
  public void list(final CommandSender sender) {
    if (plugin.getRepos().isEmpty()) {
      sender.sendMessage(ChatColor.RED + "No git repos found!");
      return;
    }
    sender.sendMessage(
        format("&7&m-------&r &aGit Repos &7(&b%d&7) &7&m-------&r", plugin.getRepos().size()));

    plugin
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            plugin,
            new Runnable() {
              @Override
              public void run() {
                plugin.getRepos().forEach(repo -> sendRepoStatus(sender, repo, true));
              }
            });
  }

  private void sendRepoStatus(CommandSender sender, CachedRepo repo, boolean hover) {
    List<BaseComponent> extras = Lists.newArrayList();
    BaseComponent text = new TextComponent(format("&a%s&7 - ", repo.getName()));
    BaseComponent secondLine = null;

    // Pull Button
    TextComponent button =
        new TextComponent(ChatColor.GRAY + "[" + ChatColor.YELLOW + "Pull" + ChatColor.GRAY + "]");
    button.setHoverEvent(
        new HoverEvent(
            HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder(ChatColor.GRAY + "Click to pull the latest remote changes!")
                .create()));
    button.setClickEvent(
        new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/git pull " + repo.getName()));

    try {
      Git git = new Git(repo.getRepo());
      RevCommit commit = git.log().setMaxCount(1).call().iterator().next();

      TextComponent commitID = new TextComponent(commit.abbreviate(7).name());
      commitID.setColor(ChatColor.GREEN);
      commitID.addExtra(" ");

      DateFormat date = SimpleDateFormat.getInstance();
      String msgAuthor =
          format(
              "&7Commit by &b%s &7on &3%s &7:&e %s",
              commit.getAuthorIdent().getName(),
              date.format(commit.getAuthorIdent().getWhen()),
              commit.getShortMessage());

      if (hover) {
        commitID.setHoverEvent(
            new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(msgAuthor).create()));
      } else {
        secondLine = new TextComponent(DIV + msgAuthor);
      }

      extras.add(commitID);
    } catch (GitAPIException e) {
      // Just omit if fails...
    }

    if (hover) {
      extras.add(button);
    }
    text.setExtra(extras);

    sender.sendMessage(text);
    if (secondLine != null) {
      sender.sendMessage(secondLine);
    }
  }

  @Subcommand("pull|update")
  @Description("Pulls the latest remote repo")
  @Syntax("[repo] - Name of the target repo, * for all")
  @CommandPermission("gitpull.reload")
  @CommandCompletion("@repos")
  public void pull(final CommandSender sender, @Default("*") String repoName)
      throws CommandException {
    boolean all = (repoName.equalsIgnoreCase("*") || repoName.equalsIgnoreCase("all"));
    if (all) {
      plugin.getRepos().forEach(r -> pullRepo(sender, r));
    } else {
      Optional<CachedRepo> repo = plugin.getRepo(repoName);
      if (repo.isPresent()) {
        pullRepo(sender, repo.get());
      } else {
        sender.sendMessage(format("&cNo repo with name &e%s&c was found!", repoName));
      }
    }
  }

  @HelpCommand
  @Description("Display the help page")
  public void git(final CommandSender sender, CommandHelp help) {
    help.showHelp();
  }

  private void pullRepo(final CommandSender sender, CachedRepo repo) {
    sender.sendMessage(format("&7Now pulling &a%s&7...", repo.getName()));

    plugin
        .getServer()
        .getScheduler()
        .runTaskAsynchronously(
            plugin,
            new Runnable() {
              @Override
              public void run() {
                try {
                  Git git = new Git(repo.getRepo());
                  git.pull().call();
                  RevCommit commit = git.log().setMaxCount(1).call().iterator().next();
                  sender.sendMessage(format("&a%s &7was successfully pulled!", repo.getName()));
                  sender.sendMessage(
                      format(
                          "%s&7Commit &a%s&7 by &b%s&7: %s",
                          DIV,
                          commit.abbreviate(7).name(),
                          commit.getAuthorIdent().getName(),
                          commit.getShortMessage()));
                  git.close();
                } catch (GitAPIException e) {
                  e.printStackTrace();
                  throw new CommandException(
                      "An error has occurred, repo not updated.\n" + e.getCause().getMessage());
                }
              }
            });
  }

  private String format(String format, Object... args) {
    return ChatColor.translateAlternateColorCodes('&', String.format(format, args));
  }
}
