package dev.concurrency.exp2;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * EXPERIMENT 2: The 60ns Latency Floor
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 4, time = 1)
@Measurement(iterations = 6, time = 2)
@Fork(value = 1, jvmArgs = {
    "-XX:-RestrictContended",
    "-XX:+UseG1GC"
})
public class LatencyFloorBenchmark {

    private final ReentrantLock mutex = new ReentrantLock();
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final AtomicLong spinLock = new AtomicLong(0);

    @Param({"0", "10", "30", "60", "100", "300"})
    public int workTokens;

    @Benchmark
    public void baseline_noLock(Blackhole bh) {
        Blackhole.consumeCPU(workTokens);
    }

    @Benchmark
    @Threads(1)
    public void mutex_uncontended(Blackhole bh) {
        mutex.lock();
        try {
            // burn through some CPU cycles
            Blackhole.consumeCPU(workTokens);
        } finally {
            mutex.unlock();
        }
    }

    @Benchmark
    @Threads(4)
    public void mutex_contended_4t(Blackhole bh) {
        mutex.lock();
        try {
            Blackhole.consumeCPU(workTokens);
        } finally {
            mutex.unlock();
        }
    }

    @Benchmark
    @Threads(8)
    public void mutex_contended_8t(Blackhole bh) {
        mutex.lock();
        try {
            Blackhole.consumeCPU(workTokens);
        } finally {
            mutex.unlock();
        }
    }

    @Benchmark
    @Threads(1)
    public void rwlock_read_uncontended(Blackhole bh) {
        rwLock.readLock().lock();
        try {
            Blackhole.consumeCPU(workTokens);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Benchmark
    @Threads(4)
    public void rwlock_read_4readers(Blackhole bh) {
        rwLock.readLock().lock();
        try {
            Blackhole.consumeCPU(workTokens);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Benchmark
    @Threads(8)
    public void rwlock_read_8readers(Blackhole bh) {
        rwLock.readLock().lock();
        try {
            Blackhole.consumeCPU(workTokens);
        } finally {
            rwLock.readLock().unlock();
        }
    }
    @Benchmark
    @Threads(1)
    public void atomic_cas_uncontended(Blackhole bh) {
        // Simulate a lock with CAS: try to set 0 → 1 (acquire), then 1 → 0 (release)
        while (!spinLock.compareAndSet(0, 1)) {
            Thread.onSpinWait();
        }
        try {
            Blackhole.consumeCPU(workTokens);
        } finally {
            spinLock.set(0);
        }
    }

    @Benchmark
    @Threads(4)
    public void atomic_cas_contended_4t(Blackhole bh) {
        while (!spinLock.compareAndSet(0, 1)) {
            Thread.onSpinWait();
        }
        try {
            Blackhole.consumeCPU(workTokens);
        } finally {
            spinLock.set(0);
        }
    }
}
