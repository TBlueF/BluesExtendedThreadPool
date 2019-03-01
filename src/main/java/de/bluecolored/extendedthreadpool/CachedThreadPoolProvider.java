/*
 * The MIT License (MIT)
 * 
 * Copyright (c) Blue <https://www.bluecolored.de>
 * Copyright (c) contributors
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package de.bluecolored.extendedthreadpool;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class CachedThreadPoolProvider<K, V> {

	private ThreadPool threadPool;
	private Map<K, Entry<V>> cache;
	private Function<K, V> provider;
	private BiConsumer<K, V> cacheRemovalListener;
	private int maxCacheEntries;
	
	/**
	 * Both, the provider and the cacheRemovalListener, are only called on the provided thread pool.
	 */
	public CachedThreadPoolProvider(ThreadPool threadPool, Function<K, V> provider, BiConsumer<K, V> cacheRemovalListener, int maxCacheEntries) {
		this.threadPool = threadPool;
		this.provider = provider;
		this.cacheRemovalListener = cacheRemovalListener;
		this.maxCacheEntries = maxCacheEntries;
		
		this.cache = new ConcurrentHashMap<>();
	}
	
	/**
	 * Does nothing if a value to this key already exists or is already scheduled to be generated.
	 * Otherwise, this method uses the provided ThreadPool to generate the value.
	 */
	public void prepare(K key) {
		getOrGenerate(key);
	}
	
	private synchronized Entry<V> getOrGenerate(K key) {
		Entry<V> entry = cache.get(key);
		if (entry != null) return entry;
		
		final Entry<V> newEntry = new Entry<>();
		threadPool.schedule(() -> newEntry.setValue(provider.apply(key)));
		cache.put(key, newEntry);
		clean();
		return newEntry;
	}
	
	private synchronized void clean() {
		while (cache.size() > maxCacheEntries) {
			boolean removed = removeOldestCacheEntry();
			
			if (!removed) break;
		}
	}
	
	private synchronized boolean removeOldestCacheEntry() {
		if (cache.isEmpty()) return false;
		
		long oldest = System.currentTimeMillis();
		K oldestKey = null;
		for (java.util.Map.Entry<K, CachedThreadPoolProvider<K, V>.Entry<V>> entry : cache.entrySet()) {
			Entry<V> e = entry.getValue();
			if (e.lastAccess < oldest && e.isPresent()) { //only remove generated/present entries
				oldest = e.lastAccess;
				oldestKey = entry.getKey();
			}
		}
		
		if (oldestKey == null) return false;
		
		Entry<V> entry = cache.remove(oldestKey);
		if (entry == null) return false;
		
		Optional<V> ov = entry.getValueIfPresent();
		if (ov.isPresent()) {
			final K key = oldestKey;
			final V value = ov.get();
			threadPool.schedule(() -> cacheRemovalListener.accept(key, value));
		}
		return true;
	}
	
	/**
	 * Returns the generated value to this key, blocks if necessary until the value is generated.
	 * This method uses the provided ThreadPool to generate the value.
	 * @throws InterruptedException if the thread gets interrupted while waiting
	 */
	public V get(K key) throws InterruptedException {
		Entry<V> entry = getOrGenerate(key);
		return entry.getValue();
	}
	
	/**
	 * Returns the value only if it is already present, does not generate it.
	 */
	public Optional<V> getIfPresent(K key) {
		Entry<V> entry = cache.get(key);
		if (entry == null) return Optional.empty();
		return entry.getValueIfPresent();
	}
	
	class Entry<T> {
		private T value;
		private long lastAccess;
		
		public Entry() {
			value = null;
			lastAccess = System.currentTimeMillis();
		}
		
		public Entry(T value) {
			this.value = value;
		}
		
		public synchronized void setValue(T value) {
			if (value == null) throw new NullPointerException("Value can't be set to null!");
			if (this.value != null) throw new IllegalStateException("Value is already set!");
			
			this.value = value;
			notifyAll();
		}
		
		/**
		 * Blocks until the value is present and then returns it
		 */
		public synchronized T getValue() throws InterruptedException {
			lastAccess = System.currentTimeMillis();
			
			while (value != null) {
				wait();
			}
			
			return value;
		}
		
		public Optional<T> getValueIfPresent() {
			return Optional.ofNullable(value);
		}
		
		public boolean isPresent() {
			return value != null;
		}
		
		public long getLastAccess() {
			return lastAccess;
		}
		
	}
	
}
