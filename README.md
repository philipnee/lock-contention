# Lock Contention

You probably have heard:

- Mutex is slow.
- RW lock works well for read-heavy workloads.
- Don't we all love lock-free architecture.

This project is about the hardware physics of lock performance — an educational benchmark suite.
Based on Jon Gjengset's "Are Mutexes Slow?"

I highly encourage everyone to watch that video.

## What this is

This repo is here to bother those instincts.

The CPU does not care what your abstraction is called.
If your read path still pounds the same cache line, it will make you pay.
If your threads write to different variables that still sit on the same cache line, it will make you pay again.

This suite is small on purpose. Four experiments. Same theme.

1. **RW lock fallacy**  
   Read-heavy does not automatically mean RW lock wins.
2. **Latency floor**  
   Tiny critical sections hit the hardware floor fast.
3. **False sharing**  
   "My thread only touches its own counter" is not enough.
4. **Left-Right**  
   Reads can get very cheap if they stop fighting over shared metadata.

## Quick start

```bash
mvn clean package -q
java -jar target/observatory-benchmarks.jar --quick
```

That writes a Markdown report to `result.md` by default.

Want a different file?

```bash
java -jar target/observatory-benchmarks.jar --output my-run.md
```

Want one experiment only?

```bash
java -jar target/observatory-benchmarks.jar --exp 1
java -jar target/observatory-benchmarks.jar --exp 3 --quick
```

## What comes out

No dashboard.
No browser tab.
No dragging JSON into a toy UI.

You run the jar. It dumps a plain Markdown report.
That file has:

- machine info
- experiment sections
- benchmark tables
- params, thread counts, score, error bars, units

## The four experiments

### 1) RW lock fallacy

The usual story is:

> many readers, few writers, therefore RW lock should be great.

Sometimes yes.
Often not for tiny read sections.

Why?
Because readers still coordinate through shared lock state.
That means shared writes. Shared writes mean cache-line traffic.

### 2) Latency floor

If the protected work is tiny enough, the lock becomes the work.

This experiment burns a configurable amount of fake CPU work inside the critical section so you can watch the shape change.
At zero or near zero work, you are mostly measuring coordination overhead.

### 3) False sharing

This is the mean one.

Each thread updates "its own" counter.
Sounds independent.
Still collapses if those counters live next to each other in memory.

Then we pad the counters manually and the cliff disappears.
Same logic. Different layout. Very different result.

### 4) Left-Right

This is the fancy one.
Two copies. One active. One standby.
Readers touch the active copy. Writer updates the other side, flips, waits, then catches up the old side.

Reads get cheap because the read path avoids the shared lock bookkeeping that kills scaling.
Writes get more expensive. Nothing is free.

## What to look for

| Experiment | What usually shows up |
|---|---|
| 1 | RW lock starts looking worse than people expect as threads go up |
| 2 | empty or tiny sections make the coordination cost obvious |
| 3 | packed counters fall off a cliff, padded counters behave like adults |
| 4 | Left-Right reads scale much better, writes pay for it |

## Notes

- This is an educational repo, not a research paper.
- The exact numbers are machine-dependent.
- The shapes matter more than the absolute values.
- If your laptop is hot, on battery saver, or doing ten other things, the numbers will wander.

## Build requirements

- Java 21+
- Maven 3.8+

## Output philosophy

No one has time.
A Markdown file is enough.

## License

Educational use. Reference freely. If you talk about the inspiration, cite Jon Gjengset's work.
