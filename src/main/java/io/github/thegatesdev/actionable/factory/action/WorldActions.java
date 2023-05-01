package io.github.thegatesdev.actionable.factory.action;

import io.github.thegatesdev.actionable.factory.ActionFactory;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.data.ReadableOptions;
import io.github.thegatesdev.mapletree.registry.FactoryRegistry;
import io.github.thegatesdev.mapletree.registry.Identifiable;
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
import static io.github.thegatesdev.actionable.Factories.*;

public final class WorldActions extends FactoryRegistry<Consumer<World>, ActionFactory<World>> {
    public WorldActions(String id) {
        super(id, Identifiable::id);
    }

    @Override
    public void registerStatic() {
        register(ActionFactory.multipleFactory(WORLD_ACTION));
        register(ActionFactory.ifElseFactory(WORLD_CONDITION, WORLD_ACTION));
        register(ActionFactory.loopFactory(WORLD_ACTION));
        register(ActionFactory.loopWhileFactory(WORLD_ACTION, WORLD_CONDITION));

        register(new ActionFactory<>("broadcast_message", (data, world) -> world.sendMessage(data.getUnsafe("message")), new ReadableOptions().add("message", COLORED_STRING)));

        register(new ActionFactory<>("each_player", (data, world) -> {
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
                .add("condition", ENTITY_CONDITION, null)
        ));
        register(new ActionFactory<>("each_entity", (data, world) -> {
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
                .add("condition", ENTITY_CONDITION, null)
                .add("living", Readable.bool(), false)
        ));

        register(new ActionFactory<>("location_action_at", (data, world) -> data.<Consumer<Location>>getUnsafe("action").accept(data.<Vector>getUnsafe("location").toLocation(world)), new ReadableOptions()
                .add("location", VECTOR)
                .add("action", LOCATION_ACTION)
        ));
    }
}
