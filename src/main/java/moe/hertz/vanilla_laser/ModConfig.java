package moe.hertz.vanilla_laser;

import lombok.Getter;
import me.lortseam.completeconfig.api.ConfigContainer;
import me.lortseam.completeconfig.api.ConfigEntries;
import me.lortseam.completeconfig.data.Config;

@ConfigEntries
public class ModConfig extends Config implements ConfigContainer {
  @Getter
  private static boolean commandEnabled = false;

  @Getter
  private static String commandName = "laser";

  ModConfig() {
    super("vanilla-laser");
  }
}
