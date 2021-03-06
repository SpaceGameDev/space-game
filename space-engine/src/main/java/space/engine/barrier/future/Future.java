package space.engine.barrier.future;

import space.engine.barrier.Barrier;
import space.engine.barrier.Delegate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface Future<R> extends GenericFuture<R>, Barrier {
	
	class DelegateFuture<T> implements Delegate<Future<T>, CompletableFuture<T>> {
		
		@Override
		public CompletableFuture<T> createCompletable() {
			return new CompletableFuture<>();
		}
		
		@Override
		public void complete(CompletableFuture<T> ret, Future<T> delegate) {
			ret.complete(delegate.assertGet());
		}
	}
	
	DelegateFuture<Object> DELEGATE = new DelegateFuture<>();
	
	@SuppressWarnings("unchecked")
	static <T> Delegate<Future<T>, CompletableFuture<T>> delegate() {
		return (Delegate<Future<T>, CompletableFuture<T>>) (Object) DELEGATE;
	}
	
	//abstract get
	R awaitGet() throws InterruptedException;
	
	R awaitGet(long time, TimeUnit unit) throws InterruptedException, TimeoutException;
	
	R assertGet() throws FutureNotFinishedException;
	
	//awaitGetUninterrupted
	
	/**
	 * Waits until event is triggered and doesn't return when interrupted.
	 * The interrupt status of this {@link Thread} will be restored.
	 */
	default R awaitGetUninterrupted() {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					return awaitGet();
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}
	
	/**
	 * Waits until event is triggered with a timeout and doesn't return when interrupted.
	 * The interrupt status of this {@link Thread} will be restored.
	 *
	 * @throws TimeoutException thrown if waiting takes longer than the specified timeout
	 */
	default R awaitGetUninterrupted(long time, TimeUnit unit) throws TimeoutException {
		boolean interrupted = false;
		try {
			while (true) {
				try {
					return awaitGet(time, unit);
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted)
				Thread.currentThread().interrupt();
		}
	}
	
	//anyException
	@Override
	default R awaitGetAnyException() throws Throwable {
		return awaitGet();
	}
	
	@Override
	default R awaitGetAnyException(long time, TimeUnit unit) throws Throwable {
		return awaitGet(time, unit);
	}
	
	@Override
	default R assertGetAnyException() {
		return assertGet();
	}
	
	//default
	@Override
	default Future<R> dereference() {
		if (isDone())
			return finished(assertGet());
		
		CompletableFuture<R> future = new CompletableFuture<>();
		this.addHook(() -> future.complete(this.assertGet()));
		return future;
	}
	
	//static
	static <R> Future<R> finished(R get) {
		class Finished extends Barrier.DoneBarrier implements Future<R> {
			
			@Override
			public R awaitGet() {
				return get;
			}
			
			@Override
			public R awaitGet(long time, TimeUnit unit) {
				return get;
			}
			
			@Override
			public R assertGet() throws FutureNotFinishedException {
				return get;
			}
		}
		return new Finished();
	}
	
	/**
	 * Returns a new {@link Future} which triggers when the 'inner' {@link Future} of the supplied {@link Future} is triggered.
	 * The value of the returned {@link Future} is the same as the 'inner' {@link Future}.
	 *
	 * @param future the Future containing the Barrier to await for
	 * @return see description
	 */
	static <T> Future<T> innerFuture(Future<? extends Future<T>> future) {
		return Barrier.inner(future).toFuture(future.assertGet());
	}
}
