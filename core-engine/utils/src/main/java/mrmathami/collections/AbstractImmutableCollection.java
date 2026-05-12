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

package mrmathami.collections;

import mrmathami.annotations.Nonnull;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

import static mrmathami.collections.ImmutableCollections.up;

abstract class AbstractImmutableCollection<E> implements Collection<E> {

	@Override
	public final boolean add(@Nonnull E element) {
		throw up();
	}

	@Override
	public final boolean remove(@Nonnull Object object) {
		throw up();
	}

	@Override
	public final boolean addAll(@Nonnull Collection<? extends E> collection) {
		throw up();
	}

	@Override
	public final boolean retainAll(@Nonnull Collection<?> collection) {
		throw up();
	}

	@Override
	public final boolean removeAll(@Nonnull Collection<?> collection) {
		throw up();
	}

	@Override
	public final void clear() {
		throw up();
	}

	@Override
	public final boolean removeIf(@Nonnull Predicate<? super E> filter) {
		throw up();
	}

}