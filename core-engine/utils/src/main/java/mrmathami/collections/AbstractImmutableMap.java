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
import mrmathami.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static mrmathami.collections.ImmutableCollections.up;

abstract class AbstractImmutableMap<K, V> implements Map<K, V> {

	@Nullable
	@Override
	public final V put(@Nonnull K key, @Nonnull V value) {
		throw up();
	}

	@Nullable
	@Override
	public final V remove(@Nonnull Object key) {
		throw up();
	}

	@Override
	public final void putAll(@Nonnull Map<? extends K, ? extends V> map) {
		throw up();
	}

	@Override
	public final void clear() {
		throw up();
	}

	@Override
	public final void replaceAll(@Nonnull BiFunction<? super K, ? super V, ? extends V> remapper) {
		throw up();
	}

	@Nullable
	@Override
	public final V putIfAbsent(@Nonnull K key, @Nonnull V value) {
		throw up();
	}

	@Override
	public final boolean remove(@Nonnull Object key, @Nonnull Object value) {
		throw up();
	}

	@Override
	public final boolean replace(@Nonnull K key, @Nullable V oldValue, @Nullable V newValue) {
		throw up();
	}

	@Nullable
	@Override
	public final V replace(@Nonnull K key, @Nullable V value) {
		throw up();
	}

	@Nullable
	@Override
	public final V computeIfAbsent(@Nonnull K key, @Nonnull Function<? super K, ? extends V> remapper) {
		throw up();
	}

	@Nullable
	@Override
	public final V computeIfPresent(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remapper) {
		throw up();
	}

	@Nullable
	@Override
	public final V compute(@Nonnull K key, @Nonnull BiFunction<? super K, ? super V, ? extends V> remapper) {
		throw up();
	}

	@Nullable
	@Override
	public final V merge(@Nonnull K key, @Nonnull V value, @Nonnull BiFunction<? super V, ? super V, ? extends V> remapper) {
		throw up();
	}

}
