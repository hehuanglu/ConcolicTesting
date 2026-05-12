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

package mrmathami.utils;

import mrmathami.annotations.Nonnull;
import mrmathami.annotations.Nullable;

import java.io.Serializable;
import java.util.Objects;

public interface Pair<A, B> extends Serializable, Cloneable {
	@Nonnull
	static <A, B> Pair<A, B> mutableOf(A a, B b) {
		return new MutablePair<>(a, b);
	}

	@Nonnull
	static <A, B> Pair<A, B> mutableOf(@Nonnull Pair<A, B> pair) {
		return new MutablePair<>(pair.getA(), pair.getB());
	}

	@Nonnull
	static <A, B> Pair<A, B> immutableOf(A a, B b) {
		return new ImmutablePair<>(a, b);
	}

	@Nonnull
	static <A, B> Pair<A, B> immutableOf(@Nonnull Pair<A, B> pair) {
		return new ImmutablePair<>(pair.getA(), pair.getB());
	}

	A getA();

	A setA(A a) throws UnsupportedOperationException;

	B getB();

	B setB(B b) throws UnsupportedOperationException;

	@Nonnull
	Pair<A, B> clone();
}

final class MutablePair<A, B> implements Pair<A, B> {
	private static final long serialVersionUID = -1L;
	private A a;
	private B b;

	MutablePair(A a, B b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public final A getA() {
		return a;
	}

	@Override
	public final A setA(A a) {
		final A oldA = this.a;
		this.a = a;
		return oldA;
	}

	@Override
	public final B getB() {
		return b;
	}

	@Override
	public final B setB(B b) {
		final B oldB = this.b;
		this.b = b;
		return oldB;
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public final MutablePair<A, B> clone() {
		try {
			return (MutablePair<A, B>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof Pair)) return false;
		final Pair<?, ?> pair = (Pair<?, ?>) object;
		return Objects.equals(a, pair.getA()) && Objects.equals(b, pair.getB());
	}

	@Override
	public final int hashCode() {
		return Objects.hash(a, b);
	}

	@Nonnull
	@Override
	public final String toString() {
		return "{ " + a + ", " + b + " }";
	}
}

final class ImmutablePair<A, B> implements Pair<A, B> {
	private static final long serialVersionUID = -1L;
	private final A a;
	private final B b;

	ImmutablePair(A a, B b) {
		this.a = a;
		this.b = b;
	}

	@Override
	public final A getA() {
		return a;
	}

	@Override
	public final A setA(A a) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Immutable pair can't be modified.");
	}

	@Override
	public final B getB() {
		return b;
	}

	@Override
	public final B setB(B b) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Immutable pair can't be modified.");
	}

	@Nonnull
	@Override
	@SuppressWarnings("unchecked")
	public ImmutablePair<A, B> clone() {
		try {
			return (ImmutablePair<A, B>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof Pair)) return false;
		final Pair<?, ?> pair = (Pair<?, ?>) object;
		return Objects.equals(a, pair.getA()) && Objects.equals(b, pair.getB());
	}

	@Override
	public final int hashCode() {
		return Objects.hash(a, b);
	}

	@Nonnull
	@Override
	public final String toString() {
		return "{ " + a + ", " + b + " }";
	}
}