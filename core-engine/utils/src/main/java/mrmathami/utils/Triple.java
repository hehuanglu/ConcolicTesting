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

public interface Triple<A, B, C> extends Serializable, Cloneable {

	@Nonnull
	static <A, B, C> Triple<A, B, C> mutableOf(A a, B b, C c) {
		return new MutableTriple<>(a, b, c);
	}

	@Nonnull
	static <A, B, C> Triple<A, B, C> mutableOf(@Nonnull Triple<A, B, C> triple) {
		return new MutableTriple<>(triple.getA(), triple.getB(), triple.getC());
	}

	@Nonnull
	static <A, B, C> Triple<A, B, C> immutableOf(A a, B b, C c) {
		return new ImmutableTriple<>(a, b, c);
	}

	@Nonnull
	static <A, B, C> Triple<A, B, C> immutableOf(@Nonnull Triple<A, B, C> triple) {
		return new ImmutableTriple<>(triple.getA(), triple.getB(), triple.getC());
	}

	A getA();

	A setA(A a) throws UnsupportedOperationException;

	B getB();

	B setB(B b) throws UnsupportedOperationException;

	C getC();

	C setC(C c) throws UnsupportedOperationException;

	@Nonnull
	Triple<A, B, C> clone();

}

final class MutableTriple<A, B, C> implements Triple<A, B, C> {
	private static final long serialVersionUID = -1L;
	private A a;
	private B b;
	private C c;

	MutableTriple(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
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

	@Override
	public final C getC() {
		return c;
	}

	@Override
	public final C setC(C c) {
		final C oldC = this.c;
		this.c = c;
		return oldC;
	}

	@SuppressWarnings("unchecked")
	@Nonnull
	@Override
	public MutableTriple<A, B, C> clone() {
		try {
			return (MutableTriple<A, B, C>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof Triple)) return false;
		final Triple<?, ?, ?> triple = (Triple<?, ?, ?>) object;
		return Objects.equals(a, triple.getA()) && Objects.equals(b, triple.getB()) && Objects.equals(c, triple.getC());
	}

	@Override
	public final int hashCode() {
		return Objects.hash(a, b, c);
	}

	@Nonnull
	@Override
	public final String toString() {
		return "{ " + a + ", " + b + ", " + c + " }";
	}
}

final class ImmutableTriple<A, B, C> implements Triple<A, B, C> {
	private static final long serialVersionUID = -1L;
	private final A a;
	private final B b;
	private final C c;

	ImmutableTriple(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}

	@Override
	public final A getA() {
		return a;
	}

	@Override
	public final A setA(A a) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Immutable triple can't be modified.");
	}

	@Override
	public final B getB() {
		return b;
	}

	@Override
	public final B setB(B b) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Immutable triple can't be modified.");
	}

	@Override
	public final C getC() {
		return c;
	}

	@Override
	public final C setC(C c) throws UnsupportedOperationException {
		throw new UnsupportedOperationException("Immutable triple can't be modified.");
	}

	@SuppressWarnings("unchecked")
	@Nonnull
	@Override
	public ImmutableTriple<A, B, C> clone() {
		try {
			return (ImmutableTriple<A, B, C>) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final boolean equals(@Nullable Object object) {
		if (this == object) return true;
		if (!(object instanceof Triple)) return false;
		final Triple<?, ?, ?> triple = (Triple<?, ?, ?>) object;
		return Objects.equals(a, triple.getA()) && Objects.equals(b, triple.getB()) && Objects.equals(c, triple.getC());
	}

	@Override
	public final int hashCode() {
		return Objects.hash(a, b, c);
	}

	@Nonnull
	@Override
	public final String toString() {
		return "{ " + a + ", " + b + ", " + c + " }";
	}
}