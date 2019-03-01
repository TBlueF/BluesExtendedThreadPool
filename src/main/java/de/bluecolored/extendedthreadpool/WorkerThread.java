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

public class WorkerThread extends Thread {

	private ThreadPool pool;
	private boolean started;
	private boolean interrupted;
	
	public WorkerThread(ThreadPool pool) {
		this.pool = pool;
		
		this.started = false;
		this.interrupted = false;
	}
	
	@Override
	public synchronized void run() {
		started = true;
		try {
			while (!interrupted) {
				Runnable task = pool.taskQueue.take();
				task.run();
				
				if (interrupted()) interrupted = true;
			}
		} catch (InterruptedException ex) {
			interrupted = true;
		}
	}
	
	public ThreadPool getThreadPool() {
		return pool;
	}
	
	public boolean isRunning() {
		return started && !interrupted;
	}
	
	public boolean isTerminated() {
		return interrupted;
	}
	
	public boolean isStarted() {
		return started;
	}
	
}
