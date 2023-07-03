package io.github.thegatesdev.actionable.builder.action;

import io.github.thegatesdev.actionable.builder.ActionBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.ReadableOptions;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.github.thegatesdev.actionable.Actionable.COLORED_STRING;
import static io.github.thegatesdev.actionable.Actionable.VECTOR;
import static io.github.thegatesdev.actionable.registry.Registries.*;
import static io.github.thegatesdev.maple.read.Readable.bool;

public final class WorldActions extends BuilderRegistry.Static<Consumer<World>, ActionBuilder<World>> {
    public WorldActions(String id) {
        super(id);
        info().description("An action executed on a world.");
    }

    @Override
    public void registerStatic() {
        register(ActionBuilder.moreFactory(WORLD_ACTION));
        register(ActionBuilder.ifElseFactory(WORLD_CONDITION, WORLD_ACTION));
        register(ActionBuilder.loopFactory(WORLD_ACTION));
        register(ActionBuilder.loopWhileFactory(WORLD_ACTION, WORLD_CONDITION));

        register(new ActionBuilder<>("broadcast_message", (data, world) -> world.sendMessage(data.getUnsafe("message")), new ReadableOptions().add("message", COLORED_STRING)));

        register(new ActionBuilder<>("each_player", (data, world) -> {
            final Consumer<Entity> action = data.getUnsafe("action");
            final Predicate<Entity> condition = data.getUnsafe("condition", null);
            if (condition == null)
                for (Player player : world.getPlayers())
                    action.accept(player);
            else
                for (Player player : world.getPlayers()) {
                    if (condition.test(player)) action.accept(player);
                }
        }, new ReadableOptions()
                .add("action", ENTITY_ACTION)
                .addOptional("condition", ENTITY_CONDITION)
        ));
        register(new ActionBuilder<>("each_entity", (data, world) -> {
            final Consumer<Entity> action = data.getUnsafe("action");
            final Predicate<Entity> condition = data.getUnsafe("condition", null);
            final List<? extends Entity> toLoop = data.getBoolean("living") ? world.getLivingEntities() : world.getEntities();
            if (condition == null)
                for (Entity entity : toLoop)
                    action.accept(entity);
            else
                for (Entity entity : toLoop) {
                    if (condition.test(entity)) action.accept(entity);
                }
        }, new ReadableOptions()
                .add("action", ENTITY_ACTION)
                .addOptional("condition", ENTITY_CONDITION)
                .add("living", bool(), false)
        ));

        register(new ActionBuilder<>("run_at", (data, world) -> data.<Consumer<Location>>getUnsafe("action").accept(data.<Vector>getUnsafe("location").toLocation(world)), new ReadableOptions()
                .add("location", VECTOR)
                .add("action", LOCATION_ACTION)
        ));
    }
}
