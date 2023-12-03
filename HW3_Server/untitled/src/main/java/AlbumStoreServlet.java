import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.List;
import java.sql.Statement;
import java.util.logging.Level;

import com.google.gson.Gson;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import static com.mysql.cj.conf.PropertyKey.logger;


@WebServlet(name = "org.example.AlbumStoreServlet",value = "/albums/*")
public class AlbumStoreServlet extends HttpServlet {
    private static final String ALBUM_DATA_FIELD = "albumData";
    private static final String IMAGE_FIELD = "image";
    private static final String INSERT_ALBUM_SQL = "INSERT INTO albums (artist, title, year, image) VALUES (?, ?, ?, ?)";
    private static final String SELECT_ALBUM_SQL = "SELECT artist, title, year FROM albums WHERE id = ?";
    private Gson gson = new Gson();

    private class ackMsg {
        private String confirmMessage;
        public ackMsg (String message) {
            confirmMessage = message;
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        res.setContentType("application/json");

        if (!ServletFileUpload.isMultipartContent(req)) {
            throw new ServletException("Content type is not multipart/form-data");
        }
        processMultipartRequest(req, res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            sendErrorResponse(response, "Missing albumId", HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String[] urlParts = pathInfo.split("/");
        if (!isGetUrlValid(urlParts)) {
            sendErrorResponse(response, "Invalid album ID", HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        String albumId = urlParts[1];
        try {
            albumInfo album = fetchAlbumFromDatabase(albumId);
            if (album != null) {
                sendResponse(response, gson.toJson(album));
            } else {
                sendErrorResponse(response, "Album not found", HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            sendErrorResponse(response, "Database error", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void sendResponse(HttpServletResponse response, String json) throws IOException {
        response.getWriter().write(json);
    }

    private void sendErrorResponse(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.getWriter().write(gson.toJson(new ackMsg(message)));
    }


    private void processMultipartRequest(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        try {
            MultipartFormData data = parseMultipartRequest(req);
            int generatedKey = saveAlbumData(data);
            sendResponseWithGeneratedKey(res, generatedKey, data.getImageBytes().length);
        } catch (FileUploadException | SQLException | ClassNotFoundException e) {
            throw new ServletException("Error processing multipart request", e);
        }
    }

    private MultipartFormData parseMultipartRequest(HttpServletRequest req)
            throws FileUploadException {
        // Parse the multi-form requests
        List<FileItem> items = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(req);
        albumInfo albumData = null;
        byte[] imageBytes = null;

        for (FileItem item : items) {
            if (item.isFormField() && ALBUM_DATA_FIELD.equals(item.getFieldName())) {
                albumData = gson.fromJson(item.getString(), albumInfo.class);
            } else if (!item.isFormField() && IMAGE_FIELD.equals(item.getFieldName())) {
                imageBytes = item.get();
            }
        }

        if (albumData == null || imageBytes == null) {
            throw new IllegalArgumentException("Missing album data or image");
        }

        return new MultipartFormData(albumData, imageBytes);
    }

    private int saveAlbumData(MultipartFormData data)
            throws SQLException, ClassNotFoundException {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement command = connection.prepareStatement(INSERT_ALBUM_SQL, Statement.RETURN_GENERATED_KEYS)) {
            command.setString(1, data.getAlbumData().getArtist());
            command.setString(2, data.getAlbumData().getTitle());
            command.setString(3, data.getAlbumData().getYear());
            command.setBytes(4, data.getImageBytes());

            command.executeUpdate();

            try (ResultSet rs = command.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to retrieve generated key");
                }
            }
        }
    }

    private albumInfo fetchAlbumFromDatabase(String albumId) throws SQLException {
        try (Connection connection = DatabaseUtil.getConnection();
             PreparedStatement command = connection.prepareStatement(SELECT_ALBUM_SQL)) {
            command.setInt(1, Integer.parseInt(albumId));
            try (ResultSet resultSet = command.executeQuery()) {
                if (resultSet.next()) {
                    return new albumInfo(
                            resultSet.getString("artist"),
                            resultSet.getString("title"),
                            resultSet.getString("year")
                    );
                }
            }
        }
        return null;
    }

    private void sendResponseWithGeneratedKey(HttpServletResponse res, int generatedKey, int imageSize)
            throws IOException {
        res.setStatus(HttpServletResponse.SC_OK);
        imageMetaData imageData = new imageMetaData(String.valueOf(generatedKey), String.valueOf(imageSize));
        try (PrintWriter out = res.getWriter()) {
            out.print(gson.toJson(imageData));
            out.flush();
        }
    }

    private boolean isGetUrlValid(String[] urlPath) {
        return urlPath.length == 2 && !urlPath[1].isEmpty();
    }
}

