package io.github.thegatesdev.actionable.util.twin;

public final class ImmutableTwin<A, T> implements Twin<A, T> {
    private final A actor;
    private final T target;
    private ImmutableTwin<T, A> flipped;

    public ImmutableTwin(A actor, T target) {
        this.actor = actor;
        this.target = target;
    }

    @Override
    public Twin<T, A> flipped() {
        if (flipped == null) flipped = new ImmutableTwin<>(target, actor);
        return flipped;
    }

    @Override
    public A actor() {
        return actor;
    }

    @Override
    public T target() {
        return target;
    }
}
