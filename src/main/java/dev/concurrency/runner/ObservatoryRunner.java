package dev.concurrency.runner;

import dev.concurrency.exp1.RwLockFallacyBenchmark;
import dev.concurrency.exp2.LatencyFloorBenchmark;
import dev.concurrency.exp3.FalseSharingBenchmark;
import dev.concurrency.exp4.LeftRightBenchmark;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Small runner for the benchmark suite.
 *
 * No dashboard. No JSON handoff. Just run it and get a Markdown report.
 */
public class ObservatoryRunner {

    public static void main(String[] args) throws Exception {
        RunConfig config = parseArgs(args);

        System.out.printf("Running benchmarks (%s) -> %s%n", config.quick ? "quick" : "full", config.outputPath);

        StringBuilder md = new StringBuilder();
        md.append("# Lock Contention\n\n");
        md.append("- Java: ").append(System.getProperty("java.version")).append("\n");
        md.append("- CPUs: ").append(Runtime.getRuntime().availableProcessors()).append("\n");
        md.append("- Mode: ").append(config.quick ? "quick" : "full").append("\n");
        md.append("- Generated: ").append(Instant.now()).append("\n\n");

        if (config.runExp1) {
            try (var ignored = startSpinner("The Reader-Writer Lock Fallacy")) {
                md.append(runExperiment(
                        "exp1_rw_fallacy",
                        "The Reader-Writer Lock Fallacy",
                        RwLockFallacyBenchmark.class.getName(),
                        config
                ));
            }
        }

        try (var ignored = startSpinner("The Shared Counter Trap")) {
            if (config.runExp2) {
                md.append(runExperiment(
                        "exp2_latency_floor",
                        "The Latency Floor",
                        LatencyFloorBenchmark.class.getName(),
                        config
                ));
            }
        }

        try (var ignored = startSpinner("False Sharing")) {
            if (config.runExp3) {
                md.append(runExperiment(
                        "exp3_false_sharing",
                        "False Sharing",
                        FalseSharingBenchmark.class.getName(),
                        config
                ));
            }
        }

        try (var ignored = startSpinner("Left-Right vs  RWLock")) {
            if (config.runExp4) {
                md.append(runExperiment(
                        "exp4_left_right",
                        "Left-Right vs ReadWriteLock",
                        LeftRightBenchmark.class.getName(),
                        config
                ));
            }
        }

        Files.writeString(Path.of(config.outputPath), md.toString());

        System.out.printf("Done. Wrote %s%n", config.outputPath);
    }

    private static String runExperiment(
        String id,
        String title,
        String benchmarkClass,
        RunConfig config
    ) throws RunnerException {

        Options opts = new OptionsBuilder()
            .include(benchmarkClass)
            .warmupIterations(config.quick ? 2 : 3)
            .warmupTime(TimeValue.seconds(config.quick ? 1 : 2))
            .measurementIterations(config.quick ? 3 : 5)
            .measurementTime(TimeValue.seconds(config.quick ? 1 : 2))
            .forks(1)
            .verbosity(VerboseMode.SILENT)
            .jvmArgs("-XX:+UseG1GC")
            .build();

        Collection<RunResult> jmhResults = new Runner(opts).run();

        StringBuilder md = new StringBuilder();
        md.append("## ").append(title).append("\n\n");
        md.append("| Case | T | Params | Score |\n");
        md.append("|---|---:|---|---:|\n");

        for (RunResult result : jmhResults) {
            Map<String, String> params = new LinkedHashMap<>();
            for (String key : result.getParams().getParamsKeys()) {
                params.put(key, result.getParams().getParam(key));
            }

            String paramText = params.isEmpty() ? "-" : inlineParams(params);
            md.append("| ")
                .append(escapeCell(shortLabel(result.getPrimaryResult().getLabel()))).append(" | ")
                .append(result.getParams().getThreads()).append(" | ")
                .append(escapeCell(paramText)).append(" | ")
                .append(format(result.getPrimaryResult().getScore()))
                .append(" ± ")
                .append(format(result.getPrimaryResult().getScoreError()))
                .append(" ")
                .append(escapeCell(result.getPrimaryResult().getScoreUnit()))
                .append(" |\n");
        }

        md.append("\n");
        return md.toString();
    }

    private static String shortLabel(String label) {
        int idx = label.lastIndexOf('.');
        return idx >= 0 ? label.substring(idx + 1) : label;
    }

    private static String inlineParams(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!first) sb.append("; ");
            sb.append(e.getKey()).append('=').append(e.getValue());
            first = false;
        }
        return sb.toString();
    }

    private static String format(double v) {
        if (Double.isNaN(v)) return "NaN";
        if (Double.isInfinite(v)) return v > 0 ? "+Inf" : "-Inf";
        return String.format(Locale.US, "%.3f", v);
    }

    private static String escapeCell(String s) {
        return s.replace("|", "\\|");
    }

    private static final String[] SPINNER = {"|", "/", "-", "\\"};

    private static AutoCloseable startSpinner(String label) {
        var done = new java.util.concurrent.atomic.AtomicBoolean(false);

        Thread t = new Thread(() -> {
            int i = 0;
            while (!done.get()) {
                System.out.print("\r- " + label + " " + SPINNER[i++ % SPINNER.length]);
                System.out.flush();
                try {
                    Thread.sleep(120);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "observatory-spinner");

        t.setDaemon(true);
        t.start();

        return () -> {
            done.set(true);
            t.interrupt();
            try {
                t.join(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            System.out.print("\r- " + label + " [done]     \n");
            System.out.flush();
        };
    }

    static class RunConfig {
        boolean runExp1 = true;
        boolean runExp2 = true;
        boolean runExp3 = true;
        boolean runExp4 = true;
        boolean quick = false;
        String outputPath = "result.md";
    }

    private static RunConfig parseArgs(String[] args) {
        RunConfig config = new RunConfig();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--quick" -> config.quick = true;
                case "--output" -> {
                    if (i + 1 < args.length) config.outputPath = args[++i];
                }
                case "--exp" -> {
                    if (i + 1 < args.length) {
                        String exp = args[++i];
                        if (!exp.equals("all")) {
                            config.runExp1 = exp.contains("1");
                            config.runExp2 = exp.contains("2");
                            config.runExp3 = exp.contains("3");
                            config.runExp4 = exp.contains("4");
                        }
                    }
                }
            }
        }
        return config;
    }

}
