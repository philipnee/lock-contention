package dev.concurrency.exp1;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * EXPERIMENT 1: The Reader-Writer Lock Fallacy
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgs = {
    "-XX:-RestrictContended",
    "-XX:+UseG1GC"
})
public class RwLockFallacyBenchmark {

    private volatile long sharedValue = 42L;

    private final ReentrantLock mutex = new ReentrantLock();
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock rwReadLock = rwLock.readLock();

    @Benchmark
    @Group("baseline")
    @GroupThreads(1)
    public long noLock_t1(Blackhole bh) {
        return sharedValue;
    }

    @Benchmark
    @Group("mutex_t1")
    @GroupThreads(1)
    public long mutex_1thread(Blackhole bh) throws InterruptedException {
        mutex.lock();
        try {
            return sharedValue;
        } finally {
            mutex.unlock();
        }
    }

    @Benchmark
    @Group("mutex_t2")
    @GroupThreads(2)
    public long mutex_2threads(Blackhole bh) throws InterruptedException {
        mutex.lock();
        try {
            return sharedValue;
        } finally {
            mutex.unlock();
        }
    }

    @Benchmark
    @Group("mutex_t4")
    @GroupThreads(4)
    public long mutex_4threads(Blackhole bh) throws InterruptedException {
        mutex.lock();
        try {
            return sharedValue;
        } finally {
            mutex.unlock();
        }
    }

    @Benchmark
    @Group("mutex_t8")
    @GroupThreads(8)
    public long mutex_8threads(Blackhole bh) throws InterruptedException {
        mutex.lock();
        try {
            return sharedValue;
        } finally {
            mutex.unlock();
        }
    }

    @Benchmark
    @Group("mutex_t16")
    @GroupThreads(16)
    public long mutex_16threads(Blackhole bh) throws InterruptedException {
        mutex.lock();
        try {
            return sharedValue;
        } finally {
            mutex.unlock();
        }
    }

    @Benchmark
    @Group("rwlock_t1")
    @GroupThreads(1)
    public long rwlock_1thread(Blackhole bh) throws InterruptedException {
        rwReadLock.lock();
        try {
            return sharedValue;
        } finally {
            rwReadLock.unlock();
        }
    }

    @Benchmark
    @Group("rwlock_t2")
    @GroupThreads(2)
    public long rwlock_2threads(Blackhole bh) throws InterruptedException {
        rwReadLock.lock();
        try {
            return sharedValue;
        } finally {
            rwReadLock.unlock();
        }
    }

    @Benchmark
    @Group("rwlock_t4")
    @GroupThreads(4)
    public long rwlock_4threads(Blackhole bh) throws InterruptedException {
        rwReadLock.lock();
        try {
            return sharedValue;
        } finally {
            rwReadLock.unlock();
        }
    }

    @Benchmark
    @Group("rwlock_t8")
    @GroupThreads(8)
    public long rwlock_8threads(Blackhole bh) throws InterruptedException {
        rwReadLock.lock();
        try {
            return sharedValue;
        } finally {
            rwReadLock.unlock();
        }
    }

    @Benchmark
    @Group("rwlock_t16")
    @GroupThreads(16)
    public long rwlock_16threads(Blackhole bh) throws InterruptedException {
        rwReadLock.lock();
        try {
            return sharedValue;
        } finally {
            rwReadLock.unlock();
        }
    }
}
