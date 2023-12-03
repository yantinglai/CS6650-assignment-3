public class albumInfo {
    private String artist;
    private String title;
    private String year;

    albumInfo(String artist, String title, String year) {
        this.artist = artist;
        this.title = title;
        this.year = year;
    }

    public String getArtist() {
        return artist;
    }
    public void setArtist(String artist) {
        this.artist = artist;
    }
    public String getTitle() {
        return title;
    }

    public String getYear() {
        return year;
    }


}