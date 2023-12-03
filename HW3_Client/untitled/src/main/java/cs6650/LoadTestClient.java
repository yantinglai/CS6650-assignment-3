package cs6650;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.concurrent.EventCountCircuitBreaker;


public class LoadTestClient {
    public static EventCountCircuitBreaker breaker = new EventCountCircuitBreaker(5, 2, TimeUnit.MINUTES, 5, 10, TimeUnit.MINUTES);
    public static final String LIKE = "like";
    public static final String DISLIKE = "dislike";
    private static final int INITIAL_THREADS = 10;
    private static final int API_CALLS_PER_THREAD = 100;
    private static final AtomicLong numOfSuccessfulRequests = new AtomicLong(0);
    private static final AtomicLong numOfFailedRequests = new AtomicLong(0);
    private static BufferedWriter writer;
    private static final String OUTPUT_FILE = "output.csv";
    private static Queue<String> globalQueue = new ConcurrentLinkedQueue<>();

    private static void writeToFile() throws IOException {
        writer = new BufferedWriter(new FileWriter(OUTPUT_FILE, true));
        for(String record:globalQueue) {
            try {
                writer.write(record);
                writer.flush();
            } catch (IOException e) {
                System.err.println("Failed to log request: " + e.getMessage());
            }
        }
    }

    private static boolean isArgumentsValid(String[] args) {
        if (args.length != 5) {
            System.out.println("Usage: Client.LoadTestClient <threadGroupSize> <numThreadGroups> <delay> <AlbumIPAddr> <ReviewIPAddr>");
            return false;
        }
        return true;
    }


    public static void main(String[] args) throws Exception {
        if (!isArgumentsValid(args)) {
            return;
        }
        int threadGroupSize = Integer.parseInt(args[0]);
        int numThreadGroups = Integer.parseInt(args[1]);
        int delay = Integer.parseInt(args[2]);
        String ipAddr1 = args[3];
        String ipAddr2 = args[4];
        executeWarmupPhase( ipAddr1,  ipAddr2);
        executeMainTest(threadGroupSize, numThreadGroups,  delay,  ipAddr1,  ipAddr2);
    }


    private static void executeWarmupPhase(String ipAddr1, String ipAddr2) throws InterruptedException, IOException {
        ExecutorService initService = Executors.newFixedThreadPool(INITIAL_THREADS);
        for (int i = 0; i < INITIAL_THREADS; i++) {
            initService.submit(new WorkerThread(ipAddr1, ipAddr2, 100, globalQueue, numOfSuccessfulRequests, numOfFailedRequests));
        }
        initService.shutdown();
        initService.awaitTermination(1, TimeUnit.HOURS);
        System.out.println("Warmup Phase finished, The Url is " + ipAddr1);
    }


    private static void executeMainTest(int threadGroupSize, int numThreadGroups, int delay, String ipAddr1, String ipAddr2) throws Exception {
        long startTime = System.currentTimeMillis();
        int totalThreads =  threadGroupSize * numThreadGroups;
        ExecutorService mainService = Executors.newFixedThreadPool(totalThreads);
        for (int i = 0; i < numThreadGroups; i++) {
            for (int j = 0; j < threadGroupSize; j++) {
                if (breaker.checkState()) {
                    try {
                        mainService.submit(new WorkerThread(ipAddr1, ipAddr2, API_CALLS_PER_THREAD, globalQueue, numOfSuccessfulRequests, numOfFailedRequests));
                    } catch (Exception ex) {
                        breaker.incrementAndCheckState();
                    }
                } else {
                    System.out.println("Error, Circuit breaker open");
                }
            }
            if (i < numThreadGroups - 1) {
                Thread.sleep(delay * 1000L);
            }
        }
        mainService.shutdown();
        mainService.awaitTermination(1, TimeUnit.HOURS);
        long endTime = System.currentTimeMillis();
        calculateAndPrintStats(startTime, endTime, threadGroupSize, numThreadGroups);
    }

    private static void calculateAndPrintStats(long startTime, long endTime, int threadGroupSize, int numThreadGroups) throws Exception {
        double wallTime = (endTime - startTime) / 1000.0;
        long totalRequests =  (long) threadGroupSize * numThreadGroups * API_CALLS_PER_THREAD;
        double throughput = 4 *  totalRequests / wallTime;

        System.out.println("Wall Time: " + wallTime + " seconds");
        System.out.println("Throughput: " + throughput + " requests per second");
        System.out.println("Number of Successful Requests: " + numOfSuccessfulRequests);
        System.out.println("Number of Failed Requests: " + numOfFailedRequests);

        writeToFile();
        RecordStats.recordStatistics(OUTPUT_FILE);
    }

}





