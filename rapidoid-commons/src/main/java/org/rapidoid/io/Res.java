package org.rapidoid.io;

import org.rapidoid.annotation.Authors;
import org.rapidoid.annotation.Since;
import org.rapidoid.log.Log;
import org.rapidoid.u.U;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/*
 * #%L
 * rapidoid-commons
 * %%
 * Copyright (C) 2014 - 2015 Nikolche Mihajlovski and contributors
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

@Authors("Nikolche Mihajlovski")
@Since("4.1.0")
public class Res {

	private static final ConcurrentMap<ResKey, Res> FILES = U.concurrentMap();

	public static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(1);

	public static final String[] DEFAULT_LOCATIONS = {""};

	private final String name;

	private final String[] possibleLocations;

	private volatile byte[] bytes;

	private volatile long lastUpdatedOn;

	private volatile long lastModified;

	private volatile String content;

	private volatile boolean trackingChanges;

	private volatile String cachedFileName;

	private volatile Object attachment;

	private final Map<String, Runnable> changeListeners = U.synchronizedMap();

	private Res(String name, String... possibleLocations) {
		this.name = name;
		this.possibleLocations = possibleLocations;
	}

	public static Res from(File file) {
		return file.isAbsolute() ? absolute(file) : from(file.getName(), "");
	}

	public static Res absolute(File file) {
		U.must(file.isAbsolute(), "Expected from filename!");

		return create(file.getAbsolutePath());
	}

	public static Res from(String filename, String... possibleLocations) {
		if (U.isEmpty(possibleLocations)) {
			possibleLocations = DEFAULT_LOCATIONS;
		}

		U.must(!U.isEmpty(filename), "Resource filename must be specified!");
		U.must(!new File(filename).isAbsolute(), "Expected relative filename!");

		return create(filename, possibleLocations);
	}

	private static Res create(String filename, String... possibleLocations) {
		ResKey key = new ResKey(filename, possibleLocations);

		Res cachedFile = FILES.get(key);

		if (cachedFile == null) {
			cachedFile = new Res(filename, possibleLocations);

			if (FILES.size() < 1000) {
				// FIXME use real cache with proper expiration
				FILES.putIfAbsent(key, cachedFile);
			}
		}

		return cachedFile;
	}

	public synchronized byte[] getBytes() {
		loadResource();

		mustExist();
		return bytes;
	}

	public byte[] getBytesOrNull() {
		loadResource();
		return bytes;
	}

	protected void loadResource() {
		// micro-caching the file content, expires after 500ms
		if (U.time() - lastUpdatedOn >= 500) {
			boolean hasChanged = false;

			synchronized (this) {

				byte[] old = bytes;
				byte[] foundRes = null;

				if (possibleLocations.length == 0) {
					Log.trace("Trying to load the from resource", "name", name);

					byte[] res = load(name);
					if (res != null) {
						Log.trace("Loaded the from resource", "name", name);
						foundRes = res;
						this.cachedFileName = name;
					}

				} else {
					for (String location : possibleLocations) {
						String filename = U.path(location, name);

						Log.trace("Trying to load the resource", "name", name, "location", location, "filename", filename);

						byte[] res = load(filename);
						if (res != null) {
							Log.trace("Loaded the resource", "name", name, "file", filename);
							foundRes = res;
							this.cachedFileName = filename;
							break;
						}
					}
				}

				if (foundRes == null) {
					this.cachedFileName = null;
				}

				this.bytes = foundRes;
				hasChanged = !U.eq(old, bytes) && (old == null || bytes == null || !Arrays.equals(old, bytes));
				lastUpdatedOn = U.time();

				if (hasChanged) {
					content = null;
					attachment = null;
				}
			}

			if (hasChanged) {
				notifyChangeListeners();
			}
		}
	}

	protected byte[] load(String filename) {
		File file = IO.file(filename);

		if (file.exists()) {

			// a normal file on the file system
			Log.trace("Resource file exists", "name", name, "file", file);

			if (file.lastModified() > this.lastModified || !filename.equals(cachedFileName)) {
				Log.info("Loading resource file", "name", name, "file", file);
				this.lastModified = file.lastModified();
				return IO.loadBytes(filename);
			} else {
				Log.trace("Resource file not modified", "name", name, "file", file);
				return bytes;
			}
		} else {
			// it might not exist or it might be on the classpath or compressed in a JAR
			Log.trace("Trying to load classpath resource", "name", name, "file", file);
			return IO.loadBytes(filename);
		}
	}

	public synchronized String getContent() {
		mustExist();

		if (content == null) {
			byte[] b = getBytes();
			content = b != null ? new String(b) : null;
		}

		return content;
	}

	public boolean exists() {
		loadResource();
		return bytes != null;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return "Res(" + name + ")";
	}

	public synchronized Reader getReader() {
		mustExist();
		return new StringReader(getContent());
	}

	public void mustExist() {
		U.must(exists(), "The file '%s' doesn't exist! Path: %s", name, possibleLocations);
	}

	public Res onChange(Runnable listener) {
		return onChange("", listener);
	}

	public Res onChange(String name, Runnable listener) {
		changeListeners.put(name, listener);
		return this;
	}

	private void notifyChangeListeners() {
		if (!changeListeners.isEmpty()) {
			Log.info("Resource has changed, reloading...", "name", name);
		}

		for (Runnable listener : changeListeners.values()) {
			try {
				listener.run();
			} catch (Throwable e) {
				Log.error("Error while processing resource changes!", e);
			}
		}
	}

	public synchronized Res trackChanges() {
		if (!trackingChanges) {
			this.trackingChanges = true;
			EXECUTOR.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					// loading the resource causes the resource to check for changes
					loadResource();
				}
			}, 0, 300, TimeUnit.MILLISECONDS);
		}

		return this;
	}

	@SuppressWarnings("unchecked")
	public <T> T attachment() {
		return exists() ? (T) attachment : null;
	}

	public void attach(Object attachment) {
		this.attachment = attachment;
	}

}
