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

package mrmathami.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public final class Range {
	private Range() {
	}

	@Retention(RetentionPolicy.SOURCE)
	public @interface Short {
		short from() default java.lang.Short.MIN_VALUE;

		short to() default java.lang.Short.MAX_VALUE;
	}

	@Retention(RetentionPolicy.SOURCE)
	public @interface Integer {
		int from() default java.lang.Integer.MIN_VALUE;

		int to() default java.lang.Integer.MAX_VALUE;
	}

	@Retention(RetentionPolicy.SOURCE)
	public @interface Long {
		long from() default java.lang.Long.MIN_VALUE;

		long to() default java.lang.Long.MAX_VALUE;
	}

	@Retention(RetentionPolicy.SOURCE)
	public @interface Float {
		float from() default java.lang.Float.MIN_VALUE;

		float to() default java.lang.Float.MAX_VALUE;
	}

	@Retention(RetentionPolicy.SOURCE)
	public @interface Double {
		double from() default java.lang.Double.MIN_VALUE;

		double to() default java.lang.Double.MAX_VALUE;
	}

}
