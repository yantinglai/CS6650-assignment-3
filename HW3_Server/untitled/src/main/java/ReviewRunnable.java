import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

public class ReviewRunnable implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewRunnable.class);
    private static final String QUEUE_NAME = "reviewQueue";
    private static final String CHARSET = StandardCharsets.UTF_8.name();
    private static final String UPDATE_SQL = "UPDATE albums SET %s = %s + 1 WHERE id = ?";
    private RMQChannelPool channelPool;

    public ReviewRunnable(RMQChannelPool pool) {
        this.channelPool = pool;
    }

    @Override
    public void run() {

        try (Channel channel = createChannel()) {
            consumeMessages(channel);
        } catch (IOException | TimeoutException e) {
            LOGGER.error("Error in run method: ", e);
        }
    }

    private Channel createChannel() throws IOException {
        Channel channel = channelPool.borrowObject();
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        channel.basicQos(50);
        return channel;
    }

    private void consumeMessages(Channel channel) throws IOException {
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), CHARSET);
            processMessage(message, delivery.getEnvelope().getDeliveryTag());
        };
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
    }

    private void processMessage(String message, long deliveryTag) {
        try {
            Channel channel = null;
            String[] messageParts = message.split(",");
            if (postReviewToDatabase(messageParts[0], messageParts[1])) {
                channel.basicAck(deliveryTag, false);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing message: ", e);
        }
    }

    public boolean postReviewToDatabase(String albumId, String likeAction) {
        String col = likeAction.equals("like") ? "`like`" : "dislike";
        String sql = String.format(UPDATE_SQL, col, col);
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement command = connection.prepareStatement(sql)) {
            command.setInt(1, Integer.parseInt(albumId));
            return command.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.error("SQL Error in postReviewToDatabase: ", e);
            return false;
        } catch (NumberFormatException e) {
            LOGGER.error("Number format error in postReviewToDatabase: ", e);
            return false;
        }
    }
}
