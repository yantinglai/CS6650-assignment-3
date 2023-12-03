package cs6650;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

class WorkerThread implements Runnable {
    private final String ipAddr;
    private final int apiCalls;
    private final APIClient client;

    public WorkerThread(String ipAddr1, String ipAddr2, int apiCalls, Queue<String> que, AtomicLong success, AtomicLong fail) throws IOException {
        this.ipAddr = ipAddr1;
        this.apiCalls = apiCalls;
        this.client = new APIClient(ipAddr1,ipAddr2,que,success,fail);
    }

    @Override
    public void run() {
        for (int i = 0; i < apiCalls; i++) {
            try {
                client.albumPost();
            } catch (IOException e) {
                System.out.println("Failed to post:" + e);
                throw new RuntimeException(e);
            }
            client.reviewPost(LoadTestClient.LIKE);
            client.reviewPost(LoadTestClient.LIKE);
            client.reviewPost(LoadTestClient.DISLIKE);
        }
    }
}
