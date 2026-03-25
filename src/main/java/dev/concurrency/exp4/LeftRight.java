package dev.concurrency.exp4;


import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * LEFT-RIGHT DATA STRUCTURE
 *
 * A wait-free read, lock-free write concurrent data structure pattern
 * that achieves near-baseline read throughput by completely avoiding
 * cache line invalidation on the read path.
 *
 * TRADE-OFF
 * -------------------------
 * - Reads:  zero lock overhead, wait-free, scales to any thread count
 * - Writes: expensive — must wait for all readers, apply mutation twice,
 *           and requires deterministic/replayable operations
 * - Memory: 2x the memory of a single copy
 *
 */
public class LeftRight<T> {
    static final class EpochCounter {
        // VarHandle for acquire/release semantics without volatile overhead
        private static final VarHandle VALUE;
        static {
            try {
                VALUE = MethodHandles.lookup()
                    .findVarHandle(EpochCounter.class, "value", long.class);
            } catch (ReflectiveOperationException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @SuppressWarnings("unused")
        private long p1, p2, p3, p4, p5, p6, p7;

        private long value = 0;

        @SuppressWarnings("unused")
        private long q1, q2, q3, q4, q5, q6, q7;

        /**
         * Increment with release semantics
         */
        void incrementRelease() {
            VALUE.setRelease(this, (long) VALUE.get(this) + 1);
        }

        /**
         * Read with acquire semantics
         */
        long getAcquire() {
            return (long) VALUE.getAcquire(this);
        }

        boolean isOdd() {
            return (getAcquire() & 1L) != 0;
        }
    }

    @SuppressWarnings("unchecked")
    private final T[] copies;

    /**
     * VarHandle gives us fine-grained memory ordering (acquire/release)
     * without the overhead of full volatile on every access.
     */
    private volatile int activeIdx = 0;
    private static final VarHandle ACTIVE_IDX;

    static {
        try {
            ACTIVE_IDX = MethodHandles.lookup()
                .findVarHandle(LeftRight.class, "activeIdx", int.class);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * each reader thread gets its own padded counter that lives in its own cache line.
     */
    private final ThreadLocal<EpochCounter> epochCounters = ThreadLocal.withInitial(EpochCounter::new);

    /**
     * all epoch counters ever created
     */
    private final java.util.concurrent.CopyOnWriteArrayList<EpochCounter> allCounters =
        new java.util.concurrent.CopyOnWriteArrayList<>();

    private final java.util.concurrent.locks.ReentrantLock writerLock =
        new java.util.concurrent.locks.ReentrantLock();

    @SuppressWarnings("unchecked")
    public LeftRight(Supplier<T> factory) {
        copies = (T[]) new Object[2];
        copies[0] = factory.get();
        copies[1] = factory.get();
    }

    public <R> R read(Function<T, R> fn) {
        EpochCounter epoch = getOrRegisterEpoch();
        epoch.incrementRelease();

        try {
            int idx = (int) ACTIVE_IDX.getAcquire(this);
            return fn.apply(copies[idx]);
        } finally {
            // Signal exit: odd → even (we are done reading)
            epoch.incrementRelease();
        }
    }

    public void readVoid(Consumer<T> fn) {
        read(t -> { fn.accept(t); return null; });
    }

    public void write(Consumer<T> mutation) {
        writerLock.lock();
        try {
            int currentActive = (int) ACTIVE_IDX.getAcquire(this);
            int inactive = 1 - currentActive;

            mutation.accept(copies[inactive]);
            ACTIVE_IDX.setRelease(this, inactive);
            drainReaders(currentActive);
            mutation.accept(copies[currentActive]);

        } finally {
            writerLock.unlock();
        }
    }

    private void drainReaders(int oldActive) {
        // Snapshot the counter list — new readers that register after
        // the swap will read the new active idx anyway, so they're safe.
        for (EpochCounter counter : allCounters) {
            // Spin until this thread's epoch is even
            while (counter.isOdd()) {
                Thread.onSpinWait(); // hint to the CPU to use spin-wait power mode
            }
        }
    }

    /**
     * Helpers
     */

    private EpochCounter getOrRegisterEpoch() {
        EpochCounter counter = epochCounters.get();
        // The first time a thread reads, register its counter in the global list
        // so writers can drain it. CopyOnWriteArrayList handles concurrent registration.
        if (!allCounters.contains(counter)) {
            allCounters.add(counter);
        }
        return counter;
    }
}
