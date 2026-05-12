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

import java.lang.reflect.Array;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public final class ArrayUtils {

	private ArrayUtils() {
	}


	public static void boundCheck(int index, int length) {
		if (index < 0 || index >= length) throw new IndexOutOfBoundsException();
	}


	private static void swap0(@Nonnull boolean[] array, int indexA, int indexB) {
		assert indexA >= 0 && indexA < array.length;
		assert indexB >= 0 && indexB < array.length;
		final boolean temp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = temp;
	}

	private static void swap0(@Nonnull byte[] array, int indexA, int indexB) {
		assert indexA >= 0 && indexA < array.length;
		assert indexB >= 0 && indexB < array.length;
		final byte temp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = temp;
	}

	private static void swap0(@Nonnull char[] array, int indexA, int indexB) {
		assert indexA >= 0 && indexA < array.length;
		assert indexB >= 0 && indexB < array.length;
		final char temp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = temp;
	}

	private static void swap0(@Nonnull double[] array, int indexA, int indexB) {
		assert indexA >= 0 && indexA < array.length;
		assert indexB >= 0 && indexB < array.length;
		final double temp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = temp;
	}

	private static void swap0(@Nonnull float[] array, int indexA, int indexB) {
		assert indexA >= 0 && indexA < array.length;
		assert indexB >= 0 && indexB < array.length;
		final float temp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = temp;
	}

	private static void swap0(@Nonnull int[] array, int indexA, int indexB) {
		assert indexA >= 0 && indexA < array.length;
		assert indexB >= 0 && indexB < array.length;
		final int temp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = temp;
	}

	private static void swap0(@Nonnull long[] array, int indexA, int indexB) {
		assert indexA >= 0 && indexA < array.length;
		assert indexB >= 0 && indexB < array.length;
		final long temp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = temp;
	}

	private static void swap0(@Nonnull short[] array, int indexA, int indexB) {
		assert indexA >= 0 && indexA < array.length;
		assert indexB >= 0 && indexB < array.length;
		final short temp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = temp;
	}

	private static <E> void swap0(@Nonnull E[] array, int indexA, int indexB) {
		assert indexA >= 0 && indexA < array.length;
		assert indexB >= 0 && indexB < array.length;
		final E temp = array[indexA];
		array[indexA] = array[indexB];
		array[indexB] = temp;
	}


	public static void swap(@Nonnull boolean[] array, int indexA, int indexB) {
		boundCheck(indexA, array.length);
		boundCheck(indexB, array.length);
		swap0(array, indexA, indexB);
	}

	public static void swap(@Nonnull byte[] array, int indexA, int indexB) {
		boundCheck(indexA, array.length);
		boundCheck(indexB, array.length);
		swap0(array, indexA, indexB);
	}

	public static void swap(@Nonnull char[] array, int indexA, int indexB) {
		boundCheck(indexA, array.length);
		boundCheck(indexB, array.length);
		swap0(array, indexA, indexB);
	}

	public static void swap(@Nonnull double[] array, int indexA, int indexB) {
		boundCheck(indexA, array.length);
		boundCheck(indexB, array.length);
		swap0(array, indexA, indexB);
	}

	public static void swap(@Nonnull float[] array, int indexA, int indexB) {
		boundCheck(indexA, array.length);
		boundCheck(indexB, array.length);
		swap0(array, indexA, indexB);
	}

	public static void swap(@Nonnull int[] array, int indexA, int indexB) {
		boundCheck(indexA, array.length);
		boundCheck(indexB, array.length);
		swap0(array, indexA, indexB);
	}

	public static void swap(@Nonnull long[] array, int indexA, int indexB) {
		boundCheck(indexA, array.length);
		boundCheck(indexB, array.length);
		swap0(array, indexA, indexB);
	}

	public static void swap(@Nonnull short[] array, int indexA, int indexB) {
		boundCheck(indexA, array.length);
		boundCheck(indexB, array.length);
		swap0(array, indexA, indexB);
	}

	public static <E> void swap(@Nonnull E[] array, int indexA, int indexB) {
		boundCheck(indexA, array.length);
		boundCheck(indexB, array.length);
		swap0(array, indexA, indexB);
	}


	public static void shuffle(@Nonnull boolean[] array) {
		shuffle(array, ThreadLocalRandom.current());
	}

	public static void shuffle(@Nonnull byte[] array) {
		shuffle(array, ThreadLocalRandom.current());
	}

	public static void shuffle(@Nonnull char[] array) {
		shuffle(array, ThreadLocalRandom.current());
	}

	public static void shuffle(@Nonnull double[] array) {
		shuffle(array, ThreadLocalRandom.current());
	}

	public static void shuffle(@Nonnull float[] array) {
		shuffle(array, ThreadLocalRandom.current());
	}

	public static void shuffle(@Nonnull int[] array) {
		shuffle(array, ThreadLocalRandom.current());
	}

	public static void shuffle(@Nonnull long[] array) {
		shuffle(array, ThreadLocalRandom.current());
	}

	public static void shuffle(@Nonnull short[] array) {
		shuffle(array, ThreadLocalRandom.current());
	}

	public static <E> void shuffle(@Nonnull E[] array) {
		shuffle(array, ThreadLocalRandom.current());
	}


	public static void shuffle(@Nonnull boolean[] array, @Nonnull Random random) {
		for (int i = array.length - 1; i > 0; i--) {
			swap0(array, random.nextInt(i + 1), i);
		}
	}

	public static void shuffle(@Nonnull byte[] array, @Nonnull Random random) {
		for (int i = array.length - 1; i > 0; i--) {
			swap0(array, random.nextInt(i + 1), i);
		}
	}

	public static void shuffle(@Nonnull char[] array, @Nonnull Random random) {
		for (int i = array.length - 1; i > 0; i--) {
			swap0(array, random.nextInt(i + 1), i);
		}
	}

	public static void shuffle(@Nonnull double[] array, @Nonnull Random random) {
		for (int i = array.length - 1; i > 0; i--) {
			swap0(array, random.nextInt(i + 1), i);
		}
	}

	public static void shuffle(@Nonnull float[] array, @Nonnull Random random) {
		for (int i = array.length - 1; i > 0; i--) {
			swap0(array, random.nextInt(i + 1), i);
		}
	}

	public static void shuffle(@Nonnull int[] array, @Nonnull Random random) {
		for (int i = array.length - 1; i > 0; i--) {
			swap0(array, random.nextInt(i + 1), i);
		}
	}

	public static void shuffle(@Nonnull long[] array, @Nonnull Random random) {
		for (int i = array.length - 1; i > 0; i--) {
			swap0(array, random.nextInt(i + 1), i);
		}
	}

	public static void shuffle(@Nonnull short[] array, @Nonnull Random random) {
		for (int i = array.length - 1; i > 0; i--) {
			swap0(array, random.nextInt(i + 1), i);
		}
	}

	public static <E> void shuffle(@Nonnull E[] array, @Nonnull Random random) {
		for (int i = array.length - 1; i > 0; i--) {
			swap0(array, random.nextInt(i + 1), i);
		}
	}


	@Nonnull
	public static boolean[] concat(@Nonnull boolean[] arrayA, @Nonnull boolean[] arrayB) {
		final int lengthA = arrayA.length, lengthB = arrayB.length;
		final boolean[] array = new boolean[lengthA + lengthB];
		System.arraycopy(arrayA, 0, array, 0, lengthA);
		System.arraycopy(arrayB, 0, array, lengthA, lengthB);
		return array;
	}

	@Nonnull
	public static byte[] concat(@Nonnull byte[] arrayA, @Nonnull byte[] arrayB) {
		final int lengthA = arrayA.length, lengthB = arrayB.length;
		final byte[] array = new byte[lengthA + lengthB];
		System.arraycopy(arrayA, 0, array, 0, lengthA);
		System.arraycopy(arrayB, 0, array, lengthA, lengthB);
		return array;
	}

	@Nonnull
	public static char[] concat(@Nonnull char[] arrayA, @Nonnull char[] arrayB) {
		final int lengthA = arrayA.length, lengthB = arrayB.length;
		final char[] array = new char[lengthA + lengthB];
		System.arraycopy(arrayA, 0, array, 0, lengthA);
		System.arraycopy(arrayB, 0, array, lengthA, lengthB);
		return array;
	}

	@Nonnull
	public static double[] concat(@Nonnull double[] arrayA, @Nonnull double[] arrayB) {
		final int lengthA = arrayA.length, lengthB = arrayB.length;
		final double[] array = new double[lengthA + lengthB];
		System.arraycopy(arrayA, 0, array, 0, lengthA);
		System.arraycopy(arrayB, 0, array, lengthA, lengthB);
		return array;
	}

	@Nonnull
	public static float[] concat(@Nonnull float[] arrayA, @Nonnull float[] arrayB) {
		final int lengthA = arrayA.length, lengthB = arrayB.length;
		final float[] array = new float[lengthA + lengthB];
		System.arraycopy(arrayA, 0, array, 0, lengthA);
		System.arraycopy(arrayB, 0, array, lengthA, lengthB);
		return array;
	}

	@Nonnull
	public static int[] concat(@Nonnull int[] arrayA, @Nonnull int[] arrayB) {
		final int lengthA = arrayA.length, lengthB = arrayB.length;
		final int[] array = new int[lengthA + lengthB];
		System.arraycopy(arrayA, 0, array, 0, lengthA);
		System.arraycopy(arrayB, 0, array, lengthA, lengthB);
		return array;
	}

	@Nonnull
	public static long[] concat(@Nonnull long[] arrayA, @Nonnull long[] arrayB) {
		final int lengthA = arrayA.length, lengthB = arrayB.length;
		final long[] array = new long[lengthA + lengthB];
		System.arraycopy(arrayA, 0, array, 0, lengthA);
		System.arraycopy(arrayB, 0, array, lengthA, lengthB);
		return array;
	}

	@Nonnull
	public static short[] concat(@Nonnull short[] arrayA, @Nonnull short[] arrayB) {
		final int lengthA = arrayA.length, lengthB = arrayB.length;
		final short[] array = new short[lengthA + lengthB];
		System.arraycopy(arrayA, 0, array, 0, lengthA);
		System.arraycopy(arrayB, 0, array, lengthA, lengthB);
		return array;
	}

	@Nonnull
	public static <E> E[] concat(@Nonnull E[] arrayA, @Nonnull E[] arrayB) {
		final int lengthA = arrayA.length, lengthB = arrayB.length;
		@SuppressWarnings("unchecked") final E[] array
				= (E[]) Array.newInstance(arrayA.getClass().getComponentType(), lengthA + lengthB);
		System.arraycopy(arrayA, 0, array, 0, lengthA);
		System.arraycopy(arrayB, 0, array, lengthA, lengthB);
		return array;
	}

}
