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

import com.google.common.collect.testing.MapTestSuiteBuilder;
import com.google.common.collect.testing.TestStringMapGenerator;
import com.google.common.collect.testing.features.CollectionFeature;
import com.google.common.collect.testing.features.CollectionSize;
import com.google.common.collect.testing.features.MapFeature;
import junit.framework.Test;
import mrmathami.collections.ImmutableOrderedMap;

import java.util.Map;

public final class ImmutableOrderedMapTest {
	public static Test suite() {
		return MapTestSuiteBuilder.using(
				new TestStringMapGenerator() {
					@Override
					protected Map<String, String> create(Map.Entry<String, String>[] entries) {
						return ImmutableOrderedMap.ofEntries(entries);
					}
				})
				.named("ImmutableOrderedMap")
				.withFeatures(
						MapFeature.REJECTS_DUPLICATES_AT_CREATION,
						CollectionFeature.SERIALIZABLE,
						CollectionSize.ANY)
				.createTestSuite();
	}
}
