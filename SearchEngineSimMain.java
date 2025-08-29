
// SearchEngineSim.java
import java.util.*;
import java.util.stream.Collectors;

abstract class CPU {
    final String name;
    final double speedFactor; // higher -> faster (simulated)

    CPU(String name, double speedFactor) {
        this.name = name;
        this.speedFactor = speedFactor;
    }

    public double adjustTimeSeconds(double measuredSeconds) {
        return measuredSeconds / speedFactor;
    }
}

class BasicCPU extends CPU {
    BasicCPU() {
        super("Basic", 1.0);
    }
}

class MidCPU extends CPU {
    MidCPU() {
        super("Mid", 2.0);
    }
}

class ProCPU extends CPU {
    ProCPU() {
        super("Pro", 3.5);
    }
}

class SearchEngineSim {
    private final List<String> documents; // original unsorted docs
    private final List<String> sortedTitles; // sorted titles for binary search
    private final Random rng;

    public SearchEngineSim(int docCount, long seed) {
        this.rng = new Random(seed);
        this.documents = generateDocs(docCount);
        this.sortedTitles = documents.stream().sorted().collect(Collectors.toList());
    }

    // Create synthetic document titles; sprinkle some with the keyword "KEYWORD42"
    private List<String> generateDocs(int n) {
        List<String> docs = new ArrayList<>(n);
        String[] sampleWords = { "alpha", "beta", "gamma", "delta", "search", "query", "page", "index", "node", "cloud",
                "data", "info", "doc" };
        for (int i = 0; i < n; i++) {
            // create a pseudo-random title
            int words = 3 + rng.nextInt(3); // 3..5 words
            StringBuilder sb = new StringBuilder();
            for (int w = 0; w < words; w++) {
                sb.append(sampleWords[rng.nextInt(sampleWords.length)]);
                if (w < words - 1)
                    sb.append(' ');
            }
            // occasionally insert a special keyword so linear search finds it
            if (rng.nextDouble() < 0.08) { // ~8% of docs contain keyword
                sb.append(" KEYWORD42");
            }
            // keep uniqueness by adding id
            sb.append(" (doc").append(String.format("%04d", i)).append(')');
            docs.add(sb.toString());
        }
        return docs;
    }

    // Linear search: find docs whose title contains the keyword (substring,
    // case-sensitive)
    public int linearSearchCount(String keyword) {
        int count = 0;
        for (String title : documents) {
            if (title.contains(keyword))
                count++;
        }
        return count;
    }

    // Binary search: exact-match on sorted list of titles
    // Returns index if found, -1 otherwise
    public int binarySearchExact(String target) {
        int lo = 0, hi = sortedTitles.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            int cmp = sortedTitles.get(mid).compareTo(target);
            if (cmp == 0)
                return mid;
            if (cmp < 0)
                lo = mid + 1;
            else
                hi = mid - 1;
        }
        return -1;
    }

    // Run many trials and measure time for linear search
    public double[] measureLinear(String keyword, int trials) {
        double[] times = new double[trials];
        for (int t = 0; t < trials; t++) {
            long s = System.nanoTime();
            int found = linearSearchCount(keyword); // perform the search
            long e = System.nanoTime();
            times[t] = (e - s) / 1e9; // seconds
            // optional tiny shuffle to vary memory layout (not required)
            if (t < trials - 1)
                Collections.shuffle(documents, new Random(rng.nextLong()));
            // keep debug info suppressed
        }
        return times;
    }

    // Run many trials and measure time for binary search (searching an exact title)
    public double[] measureBinary(String targetTitle, int trials) {
        double[] times = new double[trials];
        for (int t = 0; t < trials; t++) {
            long s = System.nanoTime();
            int idx = binarySearchExact(targetTitle);
            long e = System.nanoTime();
            times[t] = (e - s) / 1e9;
            // we expect idx >= 0 because targetTitle is chosen from generated docs
        }
        return times;
    }

    // Helpers: mean and stddev
    public static double mean(double[] arr) {
        double s = 0;
        for (double v : arr)
            s += v;
        return s / arr.length;
    }

    public static double stddev(double[] arr) {
        double m = mean(arr);
        double s = 0;
        for (double v : arr)
            s += (v - m) * (v - m);
        return Math.sqrt(s / arr.length);
    }

    // Accessors for docs so caller can pick a target title for binary search
    public List<String> getDocuments() {
        return documents;
    }

    public List<String> getSortedTitles() {
        return sortedTitles;
    }
}

// -------------- Main Program --------------
public class SearchEngineSimMain {
    public static void main(String[] args) {
        final int DOC_COUNT = 1000;
        final int TRIALS = 10;
        final long SEED = 1234L;

        SearchEngineSim sim = new SearchEngineSim(DOC_COUNT, SEED);

        // Choose a linear-search keyword that we expect to appear in some docs (we used
        // "KEYWORD42")
        String linearKeyword = "KEYWORD42";
        // For binary search, pick a title that definitely exists (choose one randomly)
        List<String> docs = sim.getDocuments();
        String binaryTarget = docs.get((int) (Math.abs(SEED) % docs.size())); // deterministic pick

        // Create CPU models
        CPU[] cpus = { new BasicCPU(), new MidCPU(), new ProCPU() };

        System.out.println("Search Engine Simulation over " + DOC_COUNT + " documents");
        System.out.println("Linear Search: substring match (\"" + linearKeyword + "\")");
        System.out.println("Binary Search: exact match (title chosen from documents)");
        System.out.println("Trials per CPU/algorithm: " + TRIALS);
        System.out.println();

        // Measure and print results table
        System.out.printf("%-15s %-8s %-8s %-12s %-12s\n", "Algorithm", "CPU", "DocCount", "Mean(s)", "StdDev(s)");
        System.out.println("------------------------------------------------------------------");

        // Measure Linear Search
        for (CPU cpu : cpus) {
            double[] rawTimes = sim.measureLinear(linearKeyword, TRIALS);
            // simulate faster CPU by dividing time by speedFactor
            double[] adj = Arrays.stream(rawTimes).map(t -> cpu.adjustTimeSeconds(t)).toArray();
            double mean = SearchEngineSim.mean(adj);
            double std = SearchEngineSim.stddev(adj);
            System.out.printf("%-15s %-8s %-8d %-12.6f %-12.6f\n", "Linear Search", cpu.name, DOC_COUNT, mean, std);
        }

        // Measure Binary Search
        for (CPU cpu : cpus) {
            double[] rawTimes = sim.measureBinary(binaryTarget, TRIALS);
            double[] adj = Arrays.stream(rawTimes).map(t -> cpu.adjustTimeSeconds(t)).toArray();
            double mean = SearchEngineSim.mean(adj);
            double std = SearchEngineSim.stddev(adj);
            System.out.printf("%-15s %-8s %-8d %-12.6f %-12.6f\n", "Binary Search", cpu.name, DOC_COUNT, mean, std);
        }

        // Print a short sample: how many documents matched keyword
        int matchCount = sim.linearSearchCount(linearKeyword);
        System.out.println();
        System.out.println("Sample checks:");
        System.out.println(" - Documents containing \"" + linearKeyword + "\": " + matchCount);
        System.out.println(" - Binary search target title (exists): " + binaryTarget);
    }
}
