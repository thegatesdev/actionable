package io.github.thegatesdev.actionable.builder.reactor;

import io.github.thegatesdev.actionable.builder.ReactorBuilder;
import io.github.thegatesdev.actionable.registry.BuilderRegistry;
import io.github.thegatesdev.maple.read.Options;
import io.github.thegatesdev.maple.read.Readable;
import io.github.thegatesdev.threshold.event.listening.ClassListener;
import org.bukkit.event.Event;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;

import static io.github.thegatesdev.actionable.registry.Registries.*;

public class Reactors extends BuilderRegistry.Static<ClassListener<? extends Event>, ReactorBuilder<?>> {
    private final Map<Class<?>, ReactorBuilder<?>> byEventClass = new HashMap<>();

    public Reactors(String key) {
        super(key);
    }

    @Override
    public void register(ReactorBuilder<?> builder) {
        byEventClass.computeIfAbsent(builder.eventClass(), aClass -> {
            super.register(builder);
            return builder;
        });
    }

    @SuppressWarnings("unchecked")
    public <E extends Event> ReactorBuilder<E> get(Class<E> eventClass) {
        return (ReactorBuilder<E>) byEventClass.get(eventClass);
    }


    @Override
    public void registerStatic() {
        register(simplePlayer("slot_change", PlayerItemHeldEvent.class));
        register(simplePlayer("item_break", PlayerItemBreakEvent.class));
        register(simplePlayer("item_consume", PlayerItemConsumeEvent.class));

        register(new ReactorBuilder<>("click", PlayerInteractEvent.class, new Options()
            .addVal("click_type", Readable.enumeration(ClickType.class), ClickType.ANY)
            .addVal("click_location", Readable.enumeration(ClickLocation.class), ClickLocation.ANY))
            .reactor("player", PlayerEvent::getPlayer, ENTITY_CONDITION, ENTITY_ACTION)
            .reactor("spot", PlayerInteractEvent::getInteractionPoint, LOCATION_CONDITION, LOCATION_ACTION)
            .condition((data, event) -> {
                if (!data.<ClickType>getUnsafe("click_type").compare(event.getAction())) return false;
                return data.<ClickLocation>getUnsafe("click_location").compare(event.getAction());
            })
        );
        register(new ReactorBuilder<>("entity_click", PlayerInteractEntityEvent.class)
            .reactor("player", PlayerEvent::getPlayer, ENTITY_CONDITION, ENTITY_ACTION)
            .reactor("clicked", PlayerInteractEntityEvent::getRightClicked, ENTITY_CONDITION, ENTITY_ACTION)
        );
        register(new ReactorBuilder<>("item_drop", PlayerDropItemEvent.class)
            .reactor("player", PlayerEvent::getPlayer, ENTITY_CONDITION, ENTITY_ACTION)
            .reactor("drop", PlayerDropItemEvent::getItemDrop, ENTITY_CONDITION, ENTITY_ACTION)
        );
        register(new ReactorBuilder<>("item_grab_attempt", PlayerAttemptPickupItemEvent.class, new Options()
            .addVal("fly_at_player", Readable.bool(), true))
            .action((data, event) -> event.setFlyAtPlayer(data.getBoolean("fly_at_player")))
            .reactor("player", PlayerEvent::getPlayer, ENTITY_CONDITION, ENTITY_ACTION)
            .reactor("item", PlayerAttemptPickupItemEvent::getItem, ENTITY_CONDITION, ENTITY_ACTION)
        );
    }

    private static <E extends PlayerEvent> ReactorBuilder<E> simplePlayer(String key, Class<E> eventClass) {
        return new ReactorBuilder<>(key, eventClass)
            .reactor("player", PlayerEvent::getPlayer, ENTITY_CONDITION, ENTITY_ACTION);
    }

    public enum ClickLocation {
        BLOCK, AIR, ANY, NONE;

        public boolean compare(Action action) {
            return switch (this) {
                case NONE -> false;
                case ANY -> action != Action.PHYSICAL;
                case BLOCK -> action == Action.LEFT_CLICK_BLOCK || action == Action.RIGHT_CLICK_BLOCK;
                case AIR -> action == Action.LEFT_CLICK_AIR || action == Action.RIGHT_CLICK_AIR;
            };
        }
    }

    public enum ClickType {
        RIGHT, LEFT, ANY, NONE;

        public boolean compare(Action action) {
            return switch (this) {
                case NONE -> false;
                case ANY -> action != Action.PHYSICAL;
                case RIGHT -> action.isRightClick();
                case LEFT -> action.isLeftClick();
            };
        }
    }
}
