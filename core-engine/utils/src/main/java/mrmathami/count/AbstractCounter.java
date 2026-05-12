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

public abstract class AbstractCounter implements Countable {
	private int count;

	public AbstractCounter(int count) {
		this.count = count;
	}

	@Override
	public final int getCount() {
		return count;
	}

	@Override
	public final int setCount(int count) {
		final int oldCount = this.count;
		this.count = count;
		return oldCount;
	}

	@Override
	public final int increaseCount() {
		final int oldCount = this.count;
		this.count += 1;
		return oldCount;
	}

	@Override
	public final int decreaseCount() {
		final int oldCount = this.count;
		this.count -= 1;
		return oldCount;
	}

	@Override
	public final int addCount(int value) {
		final int oldCount = this.count;
		this.count += value;
		return oldCount;
	}

	@Override
	public final int subtractCount(int value) {
		final int oldCount = this.count;
		this.count -= value;
		return oldCount;
	}
}
