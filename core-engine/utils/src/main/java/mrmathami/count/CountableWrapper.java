/*
 * Copyright (C) 2020-2021 Mai Thanh Minh (a.k.a. thanhminhmr or mrmathami)
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package mrmathami.count;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public interface CountableWrapper<E> extends Countable, Serializable, Cloneable {
	@Nonnull
	static <E> CountableWrapper<E> mutableOf(E value) {
		return new MutableCountableWrapper<>(value, 0);
	}

	@Nonnull
	static <E> CountableWrapper<E> mutableOf(E value, int count) {
		return new MutableCountableWrapper<>(value, count);
	}

	@Nonnull
	static <E> CountableWrapper<E> mutableOf(@Nonnull CountableWrapper<E> wrapper) {
		return new MutableCountableWrapper<>(wrapper.getValue(), wrapper.getCount());
	}

	@Nonnull
	static <E> CountableWrapper<E> immutableOf(E value) {
		return new ImmutableCountableWrapper<>(value, 0);
	}

	@Nonnull
	static <E> CountableWrapper<E> immutableOf(E value, int count) {
		return new ImmutableCountableWrapper<>(value, count);
	}

	@Nonnull
	static <E> CountableWrapper<E> immutableOf(@Nonnull CountableWrapper<E> wrapper) {
		return new ImmutableCountableWrapper<>(wrapper.getValue(), wrapper.getCount());
	}

	E getValue();

	E setValue(E value) throws UnsupportedOperationException;

	@Nonnull
	CountableWrapper<E> clone();
}

final class MutableCountableWrapper<E> extends AbstractCounter implements CountableWrapper<E> {
	private static final long serialVersionUID = -1L;
	private E value;

	MutableCountableWrapper(E value, int count) {
		super(count);
		this.value = value;
	}

	@Override
	public E getValue() {
		return value;
	}

	@Override
	public E setValue(E value) {
		final E oldValue = this.value;
		this.value = value;
		return oldValue;
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public MutableCountableWrapper<E> clone() {
		try {
			return (MutableCountableWrapper<E>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof CountableWrapper)) return false;
		final CountableWrapper<?> wrapper = (CountableWrapper<?>) object;
		return Objects.equals(value, wrapper.getValue()) && Objects.equals(getCount(), wrapper.getCount());
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, getCount());
	}

	@Nonnull
	@Override
	public String toString() {
		return "{ " + value + ", " + getCount() + " }";
	}
}

final class ImmutableCountableWrapper<E> extends AbstractCounter implements CountableWrapper<E> {
	private static final long serialVersionUID = -1L;
	private final E value;

	ImmutableCountableWrapper(E value, int count) {
		super(count);
		this.value = value;
	}

	@Override
	public E getValue() {
		return value;
	}

	@Override
	public E setValue(E value) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Immutable countable wrapper can't be modified.");
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public ImmutableCountableWrapper<E> clone() {
		try {
			return (ImmutableCountableWrapper<E>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof CountableWrapper)) return false;
		final CountableWrapper<?> wrapper = (CountableWrapper<?>) object;
		return Objects.equals(value, wrapper.getValue()) && Objects.equals(getCount(), wrapper.getCount());
	}

	@Override
	public int hashCode() {
		return Objects.hash(value, getCount());
	}

	@Nonnull
	@Override
	public String toString() {
		return "{ " + value + ", " + getCount() + " }";
	}
}
