package moe.hertz.vanilla_laser;

import lombok.extern.slf4j.Slf4j;
import me.lortseam.completeconfig.gui.ConfigScreenBuilder;
import me.lortseam.completeconfig.gui.cloth.ClothConfigScreenBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.command.argument.Vec3ArgumentType;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;

import static net.minecraft.server.command.CommandManager.*;

@Slf4j(topic = "vanilla-laser")
public class VanillaLaser implements ModInitializer {
  public static final EntityType<LaserShooterEntity> SHOOTER = Registry.register(
      Registry.ENTITY_TYPE,
      new Identifier("laser", "shooter"),
      FabricEntityTypeBuilder
          .create(SpawnGroup.CREATURE, LaserShooterEntity::new)
          .dimensions(EntityDimensions.fixed(0f, 0f))
          .trackRangeBlocks(0)
          .build());

  @Override
  public void onInitialize() {
    new ModConfig().load();

    if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
      ConfigScreenBuilder.setMain("vanilla-laser", new ClothConfigScreenBuilder());
    }

    if (ModConfig.isCommandEnabled())
      CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
        dispatcher.register(literal(ModConfig.getCommandName())
            .then(
                literal("create")
                    .then(argument("target", Vec3ArgumentType.vec3())
                        .executes(context -> {
                          try {
                            var src = context.getSource();
                            var source = src.getPosition();
                            var target = Vec3ArgumentType.getVec3(context, "target");
                            createLaser(src.getWorld(), source, target);
                          } catch (Throwable e) {
                            log.error("command error", e);
                          }
                          return 1;
                        }).then(argument("source", Vec3ArgumentType.vec3())
                            .then(argument("target", Vec3ArgumentType.vec3())
                                .executes(context -> {
                                  try {
                                    var src = context.getSource();
                                    var source = Vec3ArgumentType.getVec3(context, "source");
                                    var target = Vec3ArgumentType.getVec3(context, "target");
                                    createLaser(src.getWorld(), source, target);
                                  } catch (Throwable e) {
                                    log.error("command error", e);
                                  }
                                  return 1;
                                }))))));
      });
  }

  public static LaserInstance createLaser(ServerWorld world, Vec3d source, Vec3d target) {
    var ins = new LaserInstance(source, target);
    ins.spawn(world);
    return ins;
  }
}
