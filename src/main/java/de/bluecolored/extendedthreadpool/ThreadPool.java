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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Supplier;

public class ThreadPool {

	private boolean started;
	private boolean terminated;
	private WorkerThread[] workers;
	
	BlockingQueue<Runnable> taskQueue;
	
	public ThreadPool(int threadCount) {
		workers = new WorkerThread[threadCount];
		
		started = false;
		terminated = false;
		
		taskQueue = new LinkedBlockingQueue<>();
		
		for (int i = 0; i < workers.length; i++) {
			workers[i] = new WorkerThread(this);
		}
	}
	
	public synchronized void start() {
		if (started) return;
		
		for (Thread worker : workers) {
			worker.start();
		}
		
		started = true;
	}
	
	public synchronized void terminate() {
		if (terminated) return;
		
		for (Thread worker : workers) {
			worker.interrupt();
		}
		
		terminated = true;
	}
	
	public void schedule(Runnable runnable) {
		taskQueue.add(runnable);
	}
	
	public <T> Future<T> schedule(Supplier<T> supplier){
		CompletableFuture<T> future = new CompletableFuture<>();
		taskQueue.add(() -> future.complete(supplier.get()));
		return future;
	}
	
	public boolean isTerminated() {
		return terminated;
	}
	
	public boolean isStarted() {
		return started;
	}
	
}
