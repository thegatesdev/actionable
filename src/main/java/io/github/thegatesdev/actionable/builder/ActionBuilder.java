package io.github.thegatesdev.actionable.builder;

import io.github.thegatesdev.actionable.registry.DataBuilder;
import io.github.thegatesdev.maple.data.DataMap;
import io.github.thegatesdev.maple.data.DataValue;
import io.github.thegatesdev.maple.data.Keyed;
import io.github.thegatesdev.maple.read.ReadableOptions;
import io.github.thegatesdev.maple.read.struct.DataTypeHolder;
import io.github.thegatesdev.threshold.util.twin.Twin;

import javax.annotation.Nonnull;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static io.github.thegatesdev.maple.read.Readable.integer;

public class ActionBuilder<T> implements DataBuilder<Consumer<T>>, Keyed {
    private final String key;
    protected final BiConsumer<DataMap, T> effect;
    protected final ReadableOptions readableOptions;

    public ActionBuilder(String key, BiConsumer<DataMap, T> effect, ReadableOptions readableOptions) {
        this.key = key;
        this.effect = effect;
        this.readableOptions = readableOptions;
    }

    public ActionBuilder(String key, BiConsumer<DataMap, T> effect) {
        this(key, effect, new ReadableOptions());
    }

    public static <T> ActionBuilder<T> moreFactory(DataTypeHolder<DataValue<Consumer<T>>> dataType) {
        return new ActionBuilder<>("more", (data, t) ->
            data.getList("actions").each(element -> element.asValue().<Consumer<T>>valueUnsafe().accept(t)),
            new ReadableOptions().add("actions", dataType.dataType().list()));
    }

    public static <T> ActionBuilder<T> loopFactory(DataTypeHolder<DataValue<Consumer<T>>> loopedActionType) {
        return new ActionBuilder<>("repeat", (data, t) -> {
            int times = data.getInt("times");
            Consumer<T> action = data.getUnsafe("action");
            for (int i = 0; i < times; i++) action.accept(t);
        }, new ReadableOptions().add("times", integer()).add("action", loopedActionType));
    }

    public static <T> ActionBuilder<T> loopWhileFactory(DataTypeHolder<DataValue<Consumer<T>>> loopedActionType, DataTypeHolder<DataValue<Predicate<T>>> loopConditionType) {
        return new ActionBuilder<>("repeat_while", (data, t) -> {
            Consumer<T> action = data.getUnsafe("action");
            Predicate<T> condition = data.getUnsafe("condition");
            for (int i = 0; i < 1000; i++) { // 1000 arbitrary limit on iteration count for now
                if (!condition.test(t)) break;
                action.accept(t);
            }
        }, new ReadableOptions().add("action", loopedActionType).add("condition", loopConditionType));
    }

    public static <A, T> ActionBuilder<Twin<A, T>> flippedFactory(DataTypeHolder<DataValue<Consumer<Twin<T, A>>>> dataType) {
        return new ActionBuilder<>("flip_actor", (data, o) -> data.<Consumer<Twin<T, A>>>getUnsafe("action").accept(o.flipped()), new ReadableOptions().add("action", dataType));
    }

    public static <A, T> ActionBuilder<Twin<A, T>> splitFactory(DataTypeHolder<DataValue<Consumer<A>>> actorAction, DataTypeHolder<DataValue<Consumer<T>>> targetAction) {
        return new ActionBuilder<>("split", (data, twin) -> {
            Consumer<A> actorA = data.getUnsafe("actor_action", null);
            Consumer<T> targetA = data.getUnsafe("target_action", null);
            if (actorA != null) actorA.accept(twin.actor());
            if (targetA != null) targetA.accept(twin.target());
        }, new ReadableOptions()
            .addOptional("actor_action", actorAction)
            .addOptional("target_action", targetAction)
        );
    }


    public static <T> ActionBuilder<T> ifElseFactory(DataTypeHolder<DataValue<Predicate<T>>> conditionDataType, DataTypeHolder<DataValue<Consumer<T>>> actionDataType) {
        return new ActionBuilder<>("if_else", (data, t) -> {
            Predicate<T> condition = data.getUnsafe("condition");
            Consumer<T> ifAction = data.getUnsafe("if_action");
            if (condition.test(t)) ifAction.accept(t);
            else data.ifValue("else_action", value -> value.<Consumer<T>>valueUnsafe().accept(t));
        }, new ReadableOptions()
            .add("condition", conditionDataType)
            .add("if_action", actionDataType)
            .addOptional("else_action", actionDataType)
        );
    }

    public Consumer<T> build(DataMap data) {
        return new Action(readableOptions.read(data));
    }

    @Nonnull
    public ReadableOptions readableOptions() {
        return readableOptions;
    }

    @Override
    public String key() {
        return key;
    }

    private class Action implements Consumer<T> {
        private final DataMap data;

        public Action(DataMap data) {
            this.data = data;
        }

        public void accept(T t) {
            ActionBuilder.this.effect.accept(data, t);
        }
    }
}
