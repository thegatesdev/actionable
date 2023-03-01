package io.github.thegatesdev.actionable.util.twin;

public class MutableTwin<A, T> implements Twin<A, T> {

    private A actor;
    private T target;

    public MutableTwin(A actor, T target) {
        this.actor = actor;
        this.target = target;
    }

    public MutableTwin<A, T> setActor(A actor) {
        this.actor = actor;
        return this;
    }

    public MutableTwin<A, T> setTarget(T target) {
        this.target = target;
        return this;
    }

    @Override
    public A actor() {
        return actor;
    }

    @Override
    public T target() {
        return target;
    }

    @Override
    public MutableTwin<T, A> flipped() {
        return new MutableTwin<>(target, actor);
    }
}
