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

package mrmathami.collections.test;

import com.google.common.collect.testing.SetTestSuiteBuilder;
import com.google.common.collect.testing.TestCollidingSetGenerator;
import com.google.common.collect.testing.TestStringSetGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import junit.framework.Test;
import junit.framework.TestSuite;
import mrmathami.collections.ImmutableOrderedSet;

import java.util.Set;

public final class ImmutableOrderedSetTest {
	public static Test suite() {
		final TestSuite suite = new TestSuite("ImmutableOrderedSetTest");
		suite.addTest(SetTestSuiteBuilder.using(
				new TestStringSetGenerator() {
					@Override
					public Set<String> create(String[] elements) {
						return ImmutableOrderedSet.of(elements);
					}
				})
				.named("TestStringSetGenerator")
				.withFeatures(
						CollectionFeature.KNOWN_ORDER,
						CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
						CollectionFeature.SERIALIZABLE,
						CollectionSize.ANY)
				.createTestSuite());
		suite.addTest(SetTestSuiteBuilder.using(
				new TestCollidingSetGenerator() {
					@Override
					public Set<Object> create(Object... elements) {
						return ImmutableOrderedSet.of(elements);
					}
				})
				.named("TestCollidingSetGenerator")
				.withFeatures(
						CollectionFeature.KNOWN_ORDER,
						CollectionFeature.REJECTS_DUPLICATES_AT_CREATION,
						CollectionSize.ANY)
				.createTestSuite());
		return suite;
	}
}
