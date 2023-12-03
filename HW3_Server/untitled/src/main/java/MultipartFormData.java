public class MultipartFormData {
    private albumInfo albumData;
    private byte[] imageBytes;

    public MultipartFormData(albumInfo albumData, byte[] imageBytes) {
        this.albumData = albumData;
        this.imageBytes = imageBytes;
    }

    public albumInfo getAlbumData() {
        return albumData;
    }

    public void setAlbumData(albumInfo albumData) {
        this.albumData = albumData;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }
}
