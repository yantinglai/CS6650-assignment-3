import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import com.google.gson.Gson;
import com.rabbitmq.client.*;

@WebServlet(name = "org.example.ReviewServlet", value = "/review/*")
public class ReviewServlet extends HttpServlet {
    private Gson gson = new Gson();
    private RMQChannelPool channelPool;
    private ExecutorService consumerService;
    private static final int MAX_SIZE = 300;
    private static final int NUM_OF_CONSUMERS = 250;
    private String IP_ADDRESS = "35.165.44.35";

    public class ackMsg {
        private String confirmMessage;
        public ackMsg (String message) {
            confirmMessage = message;
        }
    }

    @Override
    public void init() throws ServletException {
        initializeRabbitMQ();
        startConsumers();
    }

    private void initializeRabbitMQ() throws ServletException {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost(IP_ADDRESS);
            com.rabbitmq.client.Connection connection = factory.newConnection();
            RMQChannelFactory channelFactory = new RMQChannelFactory(connection);
            channelPool = new RMQChannelPool(MAX_SIZE, channelFactory);
        } catch (Exception e) {
            throw new ServletException("Failed to connect to RabbitMQ", e);
        }
    }

    private void startConsumers() {
        consumerService = Executors.newFixedThreadPool(NUM_OF_CONSUMERS);
        for (int i = 0; i < NUM_OF_CONSUMERS; i++) {
            consumerService.submit(new ReviewRunnable(channelPool));
        }
        consumerService.shutdown();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");

        String urlPath = req.getPathInfo();
        if (urlPath == null || urlPath.isEmpty() || !isPostUrlValid(urlPath.split("/"))) {
            sendErrorResponse(res, "Your Post Message has wrong format", HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        processReviewUpdateRequest(req, res, urlPath.split("/"));
    }

    private boolean isPostUrlValid(String[] urlPath) {
        return urlPath.length == 3 && ("like".equals(urlPath[1]) || "dislike".equals(urlPath[1]));
    }
    
    private void processReviewUpdateRequest(HttpServletRequest req, HttpServletResponse res, String[] urlParts)
            throws IOException {
        String albumId = urlParts[2];
        String likeOrDislike = urlParts[1];
        Channel rabbitMQChannel = null;

        try {
            String message = albumId + "," + likeOrDislike;
            rabbitMQChannel = channelPool.borrowObject();
            rabbitMQChannel.basicPublish("", "reviewQueue", null, message.getBytes("UTF-8"));
            sendSuccessResponse(res, "Your review request has been successfully submitted! ");
        } catch (Exception e) {
            sendErrorResponse(res, "Error occurs when submitting request", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            safelyReturnChannelToPool(rabbitMQChannel);
        }
    }

    private void sendSuccessResponse(HttpServletResponse res, String message) throws IOException {
        res.getWriter().write(gson.toJson(new ackMsg(message)));
    }

    private void sendErrorResponse(HttpServletResponse res, String message, int statusCode) throws IOException {
        res.setStatus(statusCode);
        res.getWriter().write(gson.toJson(new ackMsg(message)));
    }

    private void safelyReturnChannelToPool(Channel rabbitMQChannel) {
        if (rabbitMQChannel != null) {
            try {
                channelPool.returnObject(rabbitMQChannel);
            } catch (Exception e) {
                System.out.println("error" + e);
            }
        }
    }


}
