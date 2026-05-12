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

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class ThreadFactoryBuilder {

	@Nonnull private static final AtomicLong COUNT = new AtomicLong(0);

	@Nullable private ThreadFactory backingThreadFactory = null;
	@Nullable private String namePrefix = null;
	@Nullable private Boolean daemon = null;
	@Nullable private Integer priority = null;
	@Nullable private UncaughtExceptionHandler uncaughtExceptionHandler = null;
	@Nullable private ClassLoader contextClassLoader = null;

	@Nonnull
	private static ThreadFactory build(@Nonnull ThreadFactoryBuilder builder) {
		final ThreadFactory backingThreadFactory =
				builder.backingThreadFactory != null
						? builder.backingThreadFactory
						: Executors.defaultThreadFactory();
		final String namePrefix =
				builder.namePrefix != null && !builder.namePrefix.isEmpty()
						? builder.namePrefix
						: "ThreadFactoryBuilder-" + COUNT.getAndIncrement();
		final AtomicLong count = new AtomicLong(0);
		final Boolean daemon = builder.daemon;
		final Integer priority = builder.priority;
		final UncaughtExceptionHandler uncaughtExceptionHandler = builder.uncaughtExceptionHandler;
		final ClassLoader contextClassLoader = builder.contextClassLoader;

		return runnable -> {
			Thread thread = backingThreadFactory.newThread(runnable);
			thread.setName(namePrefix + "-" + count.getAndIncrement());
			if (daemon != null) thread.setDaemon(daemon);
			if (priority != null) thread.setPriority(priority);
			if (uncaughtExceptionHandler != null) thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
			if (contextClassLoader != null) thread.setContextClassLoader(contextClassLoader);
			return thread;
		};
	}

	@Nonnull
	public ThreadFactoryBuilder setBackingThreadFactory(@Nullable ThreadFactory backingThreadFactory) {
		this.backingThreadFactory = backingThreadFactory;
		return this;
	}

	@Nonnull
	public ThreadFactoryBuilder setNamePrefix(@Nonnull String namePrefix) {
		this.namePrefix = namePrefix;
		return this;
	}

	@Nonnull
	public ThreadFactoryBuilder setDaemon(@Nullable Boolean daemon) {
		this.daemon = daemon;
		return this;
	}

	@Nonnull
	public ThreadFactoryBuilder setPriority(@Nullable Integer priority) {
		this.priority = priority == null || (priority >= Thread.MIN_PRIORITY && priority <= Thread.MAX_PRIORITY)
				? priority : null;
		return this;
	}

	@Nonnull
	public ThreadFactoryBuilder setUncaughtExceptionHandler(@Nullable UncaughtExceptionHandler uncaughtExceptionHandler) {
		this.uncaughtExceptionHandler = uncaughtExceptionHandler;
		return this;
	}

	@Nonnull
	public ThreadFactoryBuilder setContextClassLoader(@Nullable ClassLoader contextClassLoader) {
		this.contextClassLoader = contextClassLoader;
		return this;
	}

	@Nonnull
	public ThreadFactory build() {
		return build(this);
	}

}