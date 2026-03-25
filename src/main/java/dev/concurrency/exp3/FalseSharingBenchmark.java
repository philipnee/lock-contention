package dev.concurrency.exp3;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * EXPERIMENT 3: False Sharing — The Invisible Performance Cliff
 *
 * This experiment demonstrates one of the most insidious performance bugs
 * in concurrent Java: False Sharing.
 *
 * We have N threads. Each thread writes exclusively to its own counter slot.
 * By definition, there is NO logical sharing — thread 0 owns slot 0,
 * thread 1 owns slot 1, etc. No two threads touch the same data.
 *
 * And yet, without careful memory layout, performance collapses at ~4 threads.
 *
 * WHY
 * ------------------------------------
 * CPUs don't fetch individual bytes — they fetch 64-byte cache lines.
 * A long[] array in Java is laid out contiguously in memory:
 *
 * FIX
 * ----------------------------
 * Force each counter to occupy its own dedicated 64-byte cache line.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(value = 1, jvmArgs = {
    "-XX:-RestrictContended",
    "-XX:+UseG1GC"
})
public class FalseSharingBenchmark {

    // Maximum threads we'll test
    private static final int MAX_THREADS = 16;

    // -----------------------------------------------------------------------
    // PACKED COUNTERS: Dense array — multiple counters per cache line
    // -----------------------------------------------------------------------

    /**
     * long array. Each is 8 bytes. cache line is 64 bytes.
     * Therefore, 8 counters share a single cache line.
     * When any thread writes to "its" slot, it invalidates the line for all others.
     */
    private final long[] packedCounters = new long[MAX_THREADS];

    // -----------------------------------------------------------------------
    // PADDED COUNTERS: Each counter in its own padded struct
    // -----------------------------------------------------------------------

    /**
     *  JVM inserts padding before and after the field to ensure it
     * occupies its own 64-byte cache line.
     */
    static final class PaddedCounter {
        // Manual padding so each logical counter lives on its own cache line.
        // This avoids relying on jdk.internal.vm.annotation.Contended.
        @SuppressWarnings("unused")
        long p1, p2, p3, p4, p5, p6, p7;
        volatile long value = 0;
        @SuppressWarnings("unused")
        long q1, q2, q3, q4, q5, q6, q7;
    }

    private final PaddedCounter[] paddedCounters = new PaddedCounter[MAX_THREADS];

    @Setup
    public void setup() {
        for (int i = 0; i < MAX_THREADS; i++) {
            paddedCounters[i] = new PaddedCounter();
        }
    }

    // -----------------------------------------------------------------------
    // Thread index tracking — each thread gets a stable slot assignment
    // -----------------------------------------------------------------------

    @State(Scope.Thread)
    public static class ThreadState {
        int slot = -1;

        @Setup(Level.Trial)
        public void assignSlot() {
            this.slot = (int)(Thread.currentThread().getId() % MAX_THREADS);
        }
    }

    @Benchmark
    @Group("packed_t1")
    @GroupThreads(1)
    public void packed_1thread(ThreadState ts) {
        packedCounters[ts.slot]++;
    }

    @Benchmark
    @Group("packed_t2")
    @GroupThreads(2)
    public void packed_2threads(ThreadState ts) {
        packedCounters[ts.slot]++;
    }

    @Benchmark
    @Group("packed_t4")
    @GroupThreads(4)
    public void packed_4threads(ThreadState ts) {
        packedCounters[ts.slot]++;
    }

    @Benchmark
    @Group("packed_t8")
    @GroupThreads(8)
    public void packed_8threads(ThreadState ts) {
        packedCounters[ts.slot]++;
    }

    @Benchmark
    @Group("packed_t16")
    @GroupThreads(16)
    public void packed_16threads(ThreadState ts) {
        packedCounters[ts.slot]++;
    }

    @Benchmark
    @Group("padded_t1")
    @GroupThreads(1)
    public void padded_1thread(ThreadState ts) {
        paddedCounters[ts.slot].value++;
    }

    @Benchmark
    @Group("padded_t2")
    @GroupThreads(2)
    public void padded_2threads(ThreadState ts) {
        paddedCounters[ts.slot].value++;
    }

    @Benchmark
    @Group("padded_t4")
    @GroupThreads(4)
    public void padded_4threads(ThreadState ts) {
        paddedCounters[ts.slot].value++;
    }

    @Benchmark
    @Group("padded_t8")
    @GroupThreads(8)
    public void padded_8threads(ThreadState ts) {
        paddedCounters[ts.slot].value++;
    }

    @Benchmark
    @Group("padded_t16")
    @GroupThreads(16)
    public void padded_16threads(ThreadState ts) {
        paddedCounters[ts.slot].value++;
    }
}
