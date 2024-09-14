package com.example.addon.util;

import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class NonNullList<E> extends AbstractList<E> {
    private final List<E> delegate;
    private final E defaultElement;

    public static <E> NonNullList<E> create() {
        return new NonNullList();
    }

    protected NonNullList() {
        this(new ArrayList(), (E) null);
    }

    protected NonNullList(List<E> p_i47327_1_, @Nullable E p_i47327_2_) {
        this.delegate = p_i47327_1_;
        this.defaultElement = p_i47327_2_;
    }

    @Nonnull
    public E get(int num) {
        return this.delegate.get(num);
    }

    public E set(int p_set_1_, E p_set_2_) {
        Validate.notNull(p_set_2_);
        return this.delegate.set(p_set_1_, p_set_2_);
    }

    public void add(int p_add_1_, E p_add_2_) {
        Validate.notNull(p_add_2_);
        this.delegate.add(p_add_1_, p_add_2_);
    }

    public E remove(int p_remove_1_) {
        return this.delegate.remove(p_remove_1_);
    }

    public int size() {
        return this.delegate.size();
    }

    public void clear() {
        if (this.defaultElement == null) {
            super.clear();
        } else {
            for (int i = 0; i < this.size(); ++i) {
                this.set(i, this.defaultElement);
            }
        }

    }
}
