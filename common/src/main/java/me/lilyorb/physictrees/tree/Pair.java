package me.lilyorb.physictrees.tree;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Setter
public final class Pair<T, K> {

    private T first;
    private K second;

    public Pair(final T first, final K second) {
        this.first = first;
        this.second = second;
    }

}
