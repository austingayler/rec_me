package rec_me;

import de.umass.lastfm.Artist;
import java.util.Collection;

/**
 *
 * @author AJ Gayler
 */
public class lastfmUser {
    private final String username;
    
    public Collection<Artist> recArtists;

    public Collection<Artist> getRecArtists() {
        return recArtists;
    }

    public void setRecArtists(Collection<Artist> recArtists) {
        this.recArtists = recArtists;
    }

    public String getUsername() {
        return username;
    }
    
    public lastfmUser(String un) {
        username = un;
    }
}
