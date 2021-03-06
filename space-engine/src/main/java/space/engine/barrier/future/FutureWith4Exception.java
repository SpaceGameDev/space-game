package space.engine.barrier.future;

import org.jetbrains.annotations.NotNull;
import space.engine.barrier.Barrier;
import space.engine.barrier.Delegate;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface FutureWith4Exception<R, EX1 extends Throwable, EX2 extends Throwable, EX3 extends Throwable, EX4 extends Throwable> extends GenericFuture<R>, Barrier {
	
	static <T, EX1 extends Throwable, EX2 extends Throwable, EX3 extends Throwable, EX4 extends Throwable> Delegate<FutureWith4Exception<T, EX1, EX2, EX3, EX4>, CompletableFutureWith4Exception<T, EX1, EX2, EX3, EX4>> delegate(Class<EX1> exceptionClass1, Class<EX2> exceptionClass2, Class<EX3> exceptionClass3, Class<EX4> exceptionClass4) {
		return new Delegate<>() {
			@Override
			public CompletableFutureWith4Exception<T, EX1, EX2, EX3, EX4> createCompletable() {
				return new CompletableFutureWith4Exception<>(exceptionClass1, exceptionClass2, exceptionClass3, exceptionClass4);
			}
			
			@Override
			public void complete(CompletableFutureWith4Exception<T, EX1, EX2, EX3, EX4> ret, FutureWith4Exception<T, EX1, EX2, EX3, EX4> delegate) {
				ret.completeCallableNoDelay(delegate::assertGetAnyException);
			}
		};
	}
	
	//abstract
	R awaitGet() throws InterruptedException, EX1, EX2, EX3, EX4;
	
	R awaitGet(long time, TimeUnit unit) throws InterruptedException, TimeoutException, EX1, EX2, EX3, EX4;
	
	R assertGet() throws FutureNotFinishedException, EX1, EX2, EX3, EX4;
	
	//awaitGetUninterrupted
	
	/**
	 * Waits until event is triggered and doesn't return when interrupted.
	 * The interrupt status of this {@link Thread} will be restored.
	 */
	default R awaitGetUninterrupted() throws EX1, EX2, EX3, EX4 {
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
	default R awaitGetUninterrupted(long time, TimeUnit unit) throws TimeoutException, EX1, EX2, EX3, EX4 {
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
	default R assertGetAnyException() throws Throwable {
		return assertGet();
	}
	
	//static
	static <R, EX1 extends Throwable, EX2 extends Throwable, EX3 extends Throwable, EX4 extends Throwable> FutureWith4Exception<R, EX1, EX2, EX3, EX4> finished(R get) {
		return new FutureWith4Exception<>() {
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
			
			@Override
			public boolean isDone() {
				return true;
			}
			
			@Override
			public void addHook(@NotNull Runnable run) {
				run.run();
			}
			
			@Override
			public void await() {
			
			}
			
			@Override
			public void await(long time, TimeUnit unit) {
			
			}
		};
	}
}
