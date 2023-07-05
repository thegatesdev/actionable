package io.github.thegatesdev.actionable.registry;

import io.github.thegatesdev.maple.data.DataMap;

public interface Builder<Data> {

    Data build(DataMap data);
}
