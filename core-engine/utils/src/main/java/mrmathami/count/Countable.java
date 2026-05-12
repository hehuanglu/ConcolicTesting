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

public interface Countable {
	int getCount();

	int setCount(int count);

	default int increaseCount() {
		return setCount(getCount() + 1);
	}

	default int decreaseCount() {
		return setCount(getCount() - 1);
	}

	default int addCount(int value) {
		return setCount(getCount() + value);
	}

	default int subtractCount(int value) {
		return setCount(getCount() - value);
	}
}
