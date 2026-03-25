package dev.concurrency.exp4;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * EXPERIMENT 4: Left-Right vs ReadWriteLock
 *
 *  We compare the Left-Right pattern
 * against a ReadWriteLock on a realistic read-heavy workload: a routing table
 * (HashMap lookup).
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 3)
@Fork(value = 1, jvmArgs = {
    "-XX:-RestrictContended",
    "-XX:+UseG1GC"
})
public class LeftRightBenchmark {
    private static final int TABLE_SIZE = 1000;
    private static final String[] KEYS;

    static {
        KEYS = new String[TABLE_SIZE];
        for (int i = 0; i < TABLE_SIZE; i++) {
            KEYS[i] = "route-" + i;
        }
    }

    private LeftRight<Map<String, Integer>> leftRightTable;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private Map<String, Integer> rwLockedTable;

    @State(Scope.Thread)
    public static class ThreadState {
        int keyIdx = 0;

        public String nextKey() {
            keyIdx = (keyIdx + 1) % TABLE_SIZE;
            return KEYS[keyIdx];
        }
    }

    @Setup
    public void setup() {
        // Initialize Left-Right with a populated routing table
        leftRightTable = new LeftRight<>(() -> {
            Map<String, Integer> m = new HashMap<>(TABLE_SIZE * 2);
            for (int i = 0; i < TABLE_SIZE; i++) {
                m.put(KEYS[i], i);
            }
            return m;
        });

        // Initialize RWLock table with the same data
        rwLockedTable = new HashMap<>(TABLE_SIZE * 2);
        for (int i = 0; i < TABLE_SIZE; i++) {
            rwLockedTable.put(KEYS[i], i);
        }
    }

    @Benchmark
    @Group("lr_read_t1")
    @GroupThreads(1)
    public Integer leftRight_read_1t(ThreadState ts, Blackhole bh) {
        return leftRightTable.read(table -> table.get(ts.nextKey()));
    }

    @Benchmark
    @Group("lr_read_t2")
    @GroupThreads(2)
    public Integer leftRight_read_2t(ThreadState ts, Blackhole bh) {
        return leftRightTable.read(table -> table.get(ts.nextKey()));
    }

    @Benchmark
    @Group("lr_read_t4")
    @GroupThreads(4)
    public Integer leftRight_read_4t(ThreadState ts, Blackhole bh) {
        return leftRightTable.read(table -> table.get(ts.nextKey()));
    }

    @Benchmark
    @Group("lr_read_t8")
    @GroupThreads(8)
    public Integer leftRight_read_8t(ThreadState ts, Blackhole bh) {
        return leftRightTable.read(table -> table.get(ts.nextKey()));
    }

    @Benchmark
    @Group("lr_read_t16")
    @GroupThreads(16)
    public Integer leftRight_read_16t(ThreadState ts, Blackhole bh) {
        return leftRightTable.read(table -> table.get(ts.nextKey()));
    }

    @Benchmark
    @Group("rw_read_t1")
    @GroupThreads(1)
    public Integer rwLock_read_1t(ThreadState ts, Blackhole bh) {
        rwLock.readLock().lock();
        try {
            return rwLockedTable.get(ts.nextKey());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Benchmark
    @Group("rw_read_t2")
    @GroupThreads(2)
    public Integer rwLock_read_2t(ThreadState ts, Blackhole bh) {
        rwLock.readLock().lock();
        try {
            return rwLockedTable.get(ts.nextKey());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Benchmark
    @Group("rw_read_t4")
    @GroupThreads(4)
    public Integer rwLock_read_4t(ThreadState ts, Blackhole bh) {
        rwLock.readLock().lock();
        try {
            return rwLockedTable.get(ts.nextKey());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Benchmark
    @Group("rw_read_t8")
    @GroupThreads(8)
    public Integer rwLock_read_8t(ThreadState ts, Blackhole bh) {
        rwLock.readLock().lock();
        try {
            return rwLockedTable.get(ts.nextKey());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Benchmark
    @Group("rw_read_t16")
    @GroupThreads(16)
    public Integer rwLock_read_16t(ThreadState ts, Blackhole bh) {
        rwLock.readLock().lock();
        try {
            return rwLockedTable.get(ts.nextKey());
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private int writeCounter = 0;

    @Benchmark
    @Threads(1)
    public void leftRight_write() {
        final int key = writeCounter++ % TABLE_SIZE;
        final int value = key * 2; // deterministic!
        leftRightTable.write(table -> table.put(KEYS[key], value));
    }

    @Benchmark
    @Threads(1)
    public void rwLock_write() {
        rwLock.writeLock().lock();
        try {
            int key = writeCounter++ % TABLE_SIZE;
            rwLockedTable.put(KEYS[key], key * 2);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
