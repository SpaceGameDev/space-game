package space.engine;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import space.engine.barrier.Barrier;
import space.engine.barrier.functions.Starter;
import space.engine.event.Event;
import space.engine.event.EventEntry;
import space.engine.event.SequentialEventBuilder;
import space.engine.simpleQueue.pool.Executor;
import space.engine.simpleQueue.pool.SimpleThreadPool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class Side {
	
	//pool
	private static final SimpleThreadPool POOL = new SimpleThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
		private final AtomicInteger COUNT = new AtomicInteger();
		
		@Override
		public Thread newThread(@NotNull Runnable r) {
			return new Thread(r, "space-pool-" + COUNT.incrementAndGet());
		}
	});
	private static final ThreadLocal<Executor> THLOCAL_POOL_OVERRIDE = new ThreadLocal<>();
	
	public static void overrideThreadlocalPool(@Nullable Executor executor) {
		THLOCAL_POOL_OVERRIDE.set(executor);
	}
	
	public static Executor pool() {
		Executor executor = THLOCAL_POOL_OVERRIDE.get();
		return executor != null ? executor : POOL;
	}
	
	//event exit
	public static final Event<Starter<Barrier>> EVENT_EXIT = new SequentialEventBuilder<>();
	public static final EventEntry<Starter<Barrier>> EXIT_EVENT_ENTRY_BEFORE_APPLICATION_SHUTDOWN;
	public static final EventEntry<Starter<Barrier>> EXIT_EVENT_ENTRY_POOL_EXIT;
	
	static {
		EVENT_EXIT.addHook(EXIT_EVENT_ENTRY_BEFORE_APPLICATION_SHUTDOWN = new EventEntry<>(Starter.noop()));
		//don't wait on pool exit -> will cause deadlock
		EVENT_EXIT.addHook(EXIT_EVENT_ENTRY_POOL_EXIT = new EventEntry<>(POOL::stop, EXIT_EVENT_ENTRY_BEFORE_APPLICATION_SHUTDOWN));
	}
	
	public static Barrier exit() {
		return EVENT_EXIT.submit(Starter::start);
	}
}
