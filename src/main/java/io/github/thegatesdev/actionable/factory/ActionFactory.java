package io.github.thegatesdev.actionable.factory;

import io.github.thegatesdev.actionable.util.twin.Twin;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.mapletree.data.Readable;
import io.github.thegatesdev.mapletree.data.*;
import io.github.thegatesdev.mapletree.registry.Identifiable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ActionFactory<T> implements Identifiable, Factory<Consumer<T>>, ReadableOptionsHolder {
    private final String id;
    private final BiConsumer<DataMap, T> effect;
    private final ReadableOptions readableOptions;

    public ActionFactory(String id, BiConsumer<DataMap, T> effect, ReadableOptions readableOptions) {
        this.id = id;
        this.effect = effect;
        this.readableOptions = readableOptions;
    }

    public ActionFactory(String id, BiConsumer<DataMap, T> effect) {
        this(id, effect, new ReadableOptions());
    }

    public static <T> ActionFactory<T> multipleFactory(DataTypeHolder<? extends Consumer<T>> dataType) {
        return new ActionFactory<>("multiple", (data, t) -> data.<Collection<Consumer<T>>>getUnsafe("actions").forEach(action -> action.accept(t)), new ReadableOptions().add("actions", dataType.list()));
    }

    public static <T> ActionFactory<T> loopFactory(DataTypeHolder<? extends Consumer<T>> loopedActionType) {
        return new ActionFactory<>("repeat", (data, t) -> {
            final int times = data.getInt("times");
            final Consumer<T> action = data.getUnsafe("action");
            for (int i = 0; i < times; i++) action.accept(t);
        }, new ReadableOptions().add("times", Readable.primitive(Number.class)).add("action", loopedActionType));
    }

    public static <T> ActionFactory<T> loopWhileFactory(DataTypeHolder<? extends Consumer<T>> loopedActionType, DataTypeHolder<? extends Predicate<T>> loopConditionType) {
        return new ActionFactory<>("repeat_while", (data, t) -> {
            final Consumer<T> action = data.getUnsafe("action");
            final Predicate<T> condition = data.getUnsafe("condition");
            while (condition.test(t)) action.accept(t);
        }, new ReadableOptions().add("action", loopedActionType).add("condition", loopConditionType));
    }

    public static <A, T> ActionFactory<Twin<A, T>> flippedFactory(DataTypeHolder<? extends Consumer<Twin<T, A>>> dataType) {
        return new ActionFactory<>("flip_actor", (data, o) -> data.<Consumer<Twin<T, A>>>getUnsafe("action").accept(o.flipped()), new ReadableOptions().add("action", dataType));
    }

    public static <A, T> ActionFactory<Twin<A, T>> splitFactory(DataTypeHolder<? extends Consumer<A>> actorAction, DataTypeHolder<? extends Consumer<T>> targetAction) {
        return new ActionFactory<>("split", (data, twin) -> {
            final Consumer<A> aA = data.getUnsafe("actor_action", null);
            if (aA != null) aA.accept(twin.actor());
            final Consumer<T> tA = data.getUnsafe("target_action", null);
            if (tA != null) tA.accept(twin.target());
        }, new ReadableOptions()
                .add("actor_action", actorAction, null)
                .add("target_action", targetAction, null)
        );
    }


    public static <T> ActionFactory<T> ifElseFactory(DataTypeHolder<? extends Predicate<T>> conditionDataType, DataTypeHolder<? extends Consumer<T>> actionDataType) {
        return new ActionFactory<>("if_else", (data, t) -> {
            if (data.<Predicate<T>>getUnsafe("condition").test(t))
                data.<Consumer<T>>getUnsafe("if_action").accept(t);
            else {
                final Consumer<T> elseAction = data.getUnsafe("else_action", null);
                if (elseAction != null) elseAction.accept(t);
            }
        }, new ReadableOptions()
                .add("condition", conditionDataType)
                .add("if_action", actionDataType)
                .add("else_action", actionDataType, null)
        );
    }

    @Override
    public Consumer<T> build(DataMap data) {
        return new Action(readableOptions.read(data));
    }

    @Nonnull
    @Override
    public ReadableOptions readableOptions() {
        return readableOptions;
    }

    @Override
    public String id() {
        return id;
    }

    private class Action implements Consumer<T> {
        private final DataMap data;

        public Action(DataMap data) {
            this.data = data;
        }

        public void accept(T t) {
            ActionFactory.this.effect.accept(data, t);
        }
    }
}
