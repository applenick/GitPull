package io.yukon.gitpull;

import co.aikar.commands.BukkitCommandManager;
import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.craftbukkit.libs.jline.internal.Preconditions;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

public class GitPull extends JavaPlugin {

  private BukkitCommandManager commands;
  private List<CachedRepo> repos;
  private Logger logger;

  private static final String REPOS_CONFIG_PATH = "repositories.";

  @Override
  public void onEnable() {
    this.logger = this.getLogger();

    this.getConfig().options().copyDefaults(false);
    this.saveConfig();
    this.reloadConfig();

    SshSessionFactory.setInstance(new SshSessionFactory(this));
    this.setupCommands();
  }

  private void setupCommands() {
    this.commands = new BukkitCommandManager(this);
    commands.enableUnstableAPI("help");
    commands
        .getCommandCompletions()
        .registerStaticCompletion(
            "repos", repos.stream().map(CachedRepo::getName).collect(Collectors.toList()));
    commands.registerCommand(new GitCommands());
  }

  public List<CachedRepo> getRepos() {
    return repos;
  }

  public Optional<CachedRepo> getRepo(String name) {
    return repos.stream().filter(r -> r.getName().equalsIgnoreCase(name)).findAny();
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();
    List<CachedRepo> repos = Lists.newArrayList();
    Set<String> keys = getConfig().getConfigurationSection("repositories").getKeys(false);
    for (String key : keys) {
      try {
        repos.add(new CachedRepo(pullPath(key), repoName(key)));
      } catch (IOException e) {
        logger.log(Level.WARNING, "There was an error creating repo " + key, e);
      }
    }
    this.repos = repos;
  }

  public String sshPassphrase() {
    return this.getConfig().getString("ssh-passphrase", "password");
  }

  private String pullPath(String key) {
    String path = "";
    path += this.getConfig().getString(REPOS_CONFIG_PATH + key + ".repo-path", "/");
    if (!path.endsWith("/")) path += "/";
    path += ".git";
    return path;
  }

  private String repoName(String key) {
    return this.getConfig().getString(REPOS_CONFIG_PATH + key + ".repo-name", "repo");
  }

  public class CachedRepo {
    private final String pathway;
    private final String name;
    private final Repository repo;

    public CachedRepo(String pathway, String name) throws IOException {
      this.pathway = Preconditions.checkNotNull(pathway);
      this.name = Preconditions.checkNotNull(name);

      this.repo =
          new FileRepositoryBuilder()
              .setGitDir(new File(getPathway()))
              .readEnvironment()
              .findGitDir()
              .build();
    }

    public String getPathway() {
      return pathway;
    }

    public String getName() {
      return name;
    }

    public Repository getRepo() {
      return repo;
    }
  }
}
