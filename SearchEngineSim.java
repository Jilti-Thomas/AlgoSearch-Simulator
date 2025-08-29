import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

class Document {
    String title;
    Document(String title) { this.title = title; }
}

enum CPUType {
    BASIC(1.0), MID(0.5), PRO(0.2);
    double factor;
    CPUType(double factor) { this.factor = factor; }
}

public class SearchEngineSim {
    static Scanner sc = new Scanner(System.in);
    static List<Document> documents = new ArrayList<>();

    // Generate random documents with optional keyword insertion
    static void generateDocuments(int n, String keyword) {
        documents.clear();
        Random rand = new Random();
        for (int i = 0; i < n; i++) {
            String title = randomTitle(i);
            if (rand.nextInt(20) == 0) // 5% docs contain keyword
                title += " " + keyword;
            documents.add(new Document(title));
        }
    }

    static String randomTitle(int id) {
        String[] words = {"cloud","alpha","node","data","search","engine","query","system"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            sb.append(words[ThreadLocalRandom.current().nextInt(words.length)]).append(" ");
        }
        return sb.toString().trim() + " (doc" + id + ")";
    }

    // --- SEARCH ALGORITHMS ---
    static int linearSearch(String keyword) {
        int count = 0;
        for (Document doc : documents) {
            if (doc.title.contains(keyword)) count++;
        }
        return count;
    }

    static boolean binarySearch(List<Document> docs, String target) {
        int low = 0, high = docs.size() - 1;
        while (low <= high) {
            int mid = (low + high) / 2;
            int cmp = docs.get(mid).title.compareTo(target);
            if (cmp == 0) return true;
            if (cmp < 0) low = mid + 1; else high = mid - 1;
        }
        return false;
    }

    // --- SORTING ALGORITHMS ---
    static void bubbleSort(List<Document> docs) {
        int n = docs.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                if (docs.get(j).title.compareTo(docs.get(j+1).title) > 0) {
                    Collections.swap(docs, j, j+1);
                }
            }
        }
    }

    static void quickSort(List<Document> docs, int low, int high) {
        if (low < high) {
            int pi = partition(docs, low, high);
            quickSort(docs, low, pi-1);
            quickSort(docs, pi+1, high);
        }
    }

    static int partition(List<Document> docs, int low, int high) {
        String pivot = docs.get(high).title;
        int i = (low-1);
        for (int j=low; j<high; j++) {
            if (docs.get(j).title.compareTo(pivot) <= 0) {
                i++;
                Collections.swap(docs, i, j);
            }
        }
        Collections.swap(docs, i+1, high);
        return i+1;
    }

    static void mergeSort(List<Document> docs, int l, int r) {
        if (l < r) {
            int m = (l+r)/2;
            mergeSort(docs, l, m);
            mergeSort(docs, m+1, r);
            merge(docs, l, m, r);
        }
    }

    static void merge(List<Document> docs, int l, int m, int r) {
        int n1 = m-l+1, n2 = r-m;
        List<Document> L = new ArrayList<>(n1);
        List<Document> R = new ArrayList<>(n2);
        for (int i=0; i<n1; i++) L.add(docs.get(l+i));
        for (int j=0; j<n2; j++) R.add(docs.get(m+1+j));
        int i=0, j=0, k=l;
        while (i<n1 && j<n2) {
            if (L.get(i).title.compareTo(R.get(j).title) <= 0) docs.set(k++, L.get(i++));
            else docs.set(k++, R.get(j++));
        }
        while (i<n1) docs.set(k++, L.get(i++));
        while (j<n2) docs.set(k++, R.get(j++));
    }

    // --- Benchmark ---
    static double[] benchmark(Runnable search, int trials, CPUType cpu) {
        double[] times = new double[trials];
        for (int i = 0; i < trials; i++) {
            List<Document> copy = new ArrayList<>(documents);
            long start = System.nanoTime();
            search.run();
            long end = System.nanoTime();
            times[i] = (end - start) * cpu.factor / 1e9;
        }
        return times;
    }

    static double average(double[] arr) {
        return Arrays.stream(arr).average().orElse(0);
    }

    static void showStats(String algo, double[] times) {
        double mean = average(times);
        double best = Arrays.stream(times).min().orElse(0);
        double worst = Arrays.stream(times).max().orElse(0);
        System.out.printf("%-15s Mean=%.6f Best=%.6f Worst=%.6f%n", algo, mean, best, worst);
    }

    static void showBarChart(Map<String, Double> results) {
        System.out.println("\n--- Performance Comparison (Bar Chart) ---");
        double max = Collections.max(results.values());
        for (Map.Entry<String, Double> e : results.entrySet()) {
            int barLength = (int) ((e.getValue() / max) * 50);
            System.out.printf("%-15s | %s (%.6f s)%n", e.getKey(),
                    "#".repeat(Math.max(0, barLength)), e.getValue());
        }
    }

    // --- MAIN ---
    public static void main(String[] args) {
        System.out.println("=== Search & Sorting Benchmark Tool ===");
        System.out.print("Enter number of documents: ");
        int n = sc.nextInt();
        sc.nextLine();
        System.out.print("Enter keyword to search: ");
        String keyword = sc.nextLine();

        generateDocuments(n, keyword);

        System.out.println("Choose CPU type: 1.Basic 2.Mid 3.Pro");
        int cpuChoice = sc.nextInt();
        CPUType cpu = (cpuChoice == 2 ? CPUType.MID : cpuChoice == 3 ? CPUType.PRO : CPUType.BASIC);

        int trials = 5;
        Map<String, Double> results = new LinkedHashMap<>();

        // Linear Search
        double[] t1 = benchmark(() -> linearSearch(keyword), trials, cpu);
        results.put("LinearSearch", average(t1));
        showStats("LinearSearch", t1);

        // Binary Search (after sorting)
        List<Document> sortedDocs = new ArrayList<>(documents);
        sortedDocs.sort(Comparator.comparing(d -> d.title));
        String target = sortedDocs.get(sortedDocs.size()/2).title;
        double[] t2 = benchmark(() -> binarySearch(sortedDocs, target), trials, cpu);
        results.put("BinarySearch", average(t2));
        showStats("BinarySearch", t2);

        // Bubble Sort
        double[] t3 = benchmark(() -> bubbleSort(new ArrayList<>(documents)), trials, cpu);
        results.put("BubbleSort", average(t3));
        showStats("BubbleSort", t3);

        // Quick Sort
        double[] t4 = benchmark(() -> quickSort(new ArrayList<>(documents), 0, documents.size()-1), trials, cpu);
        results.put("QuickSort", average(t4));
        showStats("QuickSort", t4);

        // Merge Sort
        double[] t5 = benchmark(() -> mergeSort(new ArrayList<>(documents), 0, documents.size()-1), trials, cpu);
        results.put("MergeSort", average(t5));
        showStats("MergeSort", t5);

        // Bar Chart
        showBarChart(results);
    }
}
