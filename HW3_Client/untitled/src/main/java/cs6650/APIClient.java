package cs6650;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

class APIClient {
    private final String albumIpAddr;
    private final String reviewIpAddr;
    private final HttpClient httpClient;
    private byte[] imageBytes;
    private Queue<String> records;

    private final AtomicLong numOfSuccessfulRequests;
    private final AtomicLong numOfFailedRequests;
    private static final String IMAGE_PATH = "/Users/sundri/Desktop/HW3_Client/untitled/burrito_cat.jpeg";
    public APIClient(String ipAddr1,String ipAddr2,Queue<String> queue,AtomicLong numOfSuccessfulRequests, AtomicLong numOfFailedRequests) throws IOException {
        this.albumIpAddr = ipAddr1;
        this.reviewIpAddr = ipAddr2;
        this.httpClient = HttpClient.newHttpClient();
        this.records = queue;
        this.numOfSuccessfulRequests = numOfSuccessfulRequests;
        this.numOfFailedRequests = numOfFailedRequests;
    }

    public void albumPost() throws IOException {
        String boundary = UUID.randomUUID().toString();
        String albumDataPart = buildAlbumDataPart(boundary);
        byte[] imageBytes = loadImageBytes(IMAGE_PATH);
        String imageHeader = buildImageHeader(boundary);
        String endBoundary = "\r\n--" + boundary + "--\r\n";

        List<byte[]> byteArrays = Arrays.asList(
                albumDataPart.getBytes(),
                imageHeader.getBytes(),
                imageBytes,
                endBoundary.getBytes()
        );

        HttpRequest request = buildHttpRequest(byteArrays, boundary);
        executeHttpRequest(request, 1);
    }

    private String buildAlbumDataPart(String boundary) {
        String jsonString = "{\n" +
                "    \"artist\": \"Sundri Bun\",\n" +
                "    \"title\": \"Please work this time!\",\n" +
                "    \"year\": \"2023\"\n" +
                "}";
        return "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"albumData\"\r\n\r\n" +
                jsonString + "\r\n";
    }

    private byte[] loadImageBytes(String imagePath) throws IOException {
        try {
            return Files.readAllBytes(Path.of(imagePath));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read image file", e);
        }
    }

    private String buildImageHeader(String boundary) {
        return "--" + boundary + "\r\n" +
                "Content-Disposition: form-data; name=\"image\"; filename=\"filename.jpg\"\r\n" +
                "Content-Type: image/jpeg\r\n\r\n";
    }

    private HttpRequest buildHttpRequest(List<byte[]> byteArrays, String boundary) {
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofByteArrays(byteArrays);
        return HttpRequest.newBuilder()
                .uri(URI.create(albumIpAddr))
                .POST(publisher)
                .header("Content-Type", "multipart/form-data;boundary=" + boundary)
                .build();
    }



    public void reviewPost(String likeOrDislike) {
        String concatIpAddr = reviewIpAddr+"/"+likeOrDislike+"/1";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(concatIpAddr))
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        executeHttpRequest(request,1);
    }


    private void executeHttpRequest(HttpRequest request,int method) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                long end = System.currentTimeMillis();
                long latency = end - start;
                addRecord(start,method,latency,statusCode);
                if (statusCode >= 200 && statusCode < 400) {
                    numOfSuccessfulRequests.incrementAndGet();
                    break;
                }
                if (statusCode >= 400 && statusCode <= 500) {
                    numOfFailedRequests.incrementAndGet();
                    System.err.println("Client error: " + response);
                    break;
                }
            } catch (Exception e) {
                System.err.println("Unexpected Error: " + e.getMessage());
            }
        }
    }

    private void addRecord(long start, int method, long latency, int statusCode) {
        String record = String.format("%s,%d,%d,%d\n", start, method, latency, statusCode);
        this.records.add(record);
    }
}