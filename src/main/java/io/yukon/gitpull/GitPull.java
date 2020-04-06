package io.yukon.gitpull;

import co.aikar.commands.BukkitCommandManager;
import java.io.File;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitPull extends JavaPlugin {

  private BukkitCommandManager commands;
  private Repository repo;

  @Override
  public void onEnable() {
    this.getConfig().options().copyDefaults(true);
    this.saveConfig();
    this.reloadConfig();

    SshSessionFactory.setInstance(new SshSessionFactory(this));
    this.setupCommands();
  }

  private void setupCommands() {
    this.commands = new BukkitCommandManager(this);
    commands.enableUnstableAPI("help");
    commands.registerDependency(Repository.class, repo);
    commands.registerCommand(new GitCommands());
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();
    try {
      this.repo =
          new FileRepositoryBuilder()
              .setGitDir(new File(pullPath()))
              .readEnvironment()
              .findGitDir()
              .build();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String pullPath() {
    String path = "";
    path += this.getConfig().getString("repo-path", "/");
    if (!path.endsWith("/")) path += "/";
    path += ".git";
    return path;
  }

  public String sshPassphrase() {
    return getConfig().getString("ssh-passphrase", "password");
  }

  public String repoName() {
    return getConfig().getString("repo-name", "repo");
  }
}
