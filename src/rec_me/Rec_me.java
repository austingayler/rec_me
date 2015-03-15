package rec_me;

import com.google.common.primitives.Ints;
import com.google.gdata.client.youtube.YouTubeQuery;
import com.google.gdata.client.youtube.YouTubeService;
import com.google.gdata.data.youtube.VideoEntry;
import com.google.gdata.data.youtube.VideoFeed;
import com.google.gdata.data.youtube.YouTubeMediaGroup;
import com.google.gdata.util.ServiceException;

import de.umass.lastfm.Artist;
import de.umass.lastfm.Authenticator;
import de.umass.lastfm.Caller;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Session;
import de.umass.lastfm.Track;
import de.umass.lastfm.User;
//import de.umass.lastfm.Tag;
import static de.umass.util.StringUtilities.md5;
import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

/**
 * @author AJ Gayler
 */
public class Rec_me {

    private static String LFM_API_KEY;
    private static String LFM_SECRET;

    private static String YT_API_KEY;
    private static final String YT_CLIENT_KEY = "";

    private static Session session; //for last.fm
    public static ArrayList<Track> downloadQueue;

    private static lastfmUser user;
    private static String curChoice = null;
    private static boolean DEBUG_MODE;
    private static double SONG_TOLERANCE_1 = 0.4; //compares subsequent songs to the first song--listeners must be above this ratio
    private static double SONG_TOLERANCE_2 = 0.6; //compares songs to next top-listened songs by artist
    private static String api_sig;

    public static void main(String[] args) throws URISyntaxException, IOException, IllegalArgumentException, IllegalAccessException, ServiceException {
        printWelcome();
        authenticate();
        initSettings();
        printMenu();
        while (!curChoice.equalsIgnoreCase("q")) {
            switch (curChoice) {
                case "1": //user
                    addUserRecs();
                    break;
                case "2": //tag
                    addSongsByTag();
                    break;
                case "3": //similar
                    addSongsBySimilarArtist();
                    break;
                case "4": //download
                    download();
                    break;
                case "5":
                    changeSettings();
                    break;
                default:
                    System.out.println("idk how u got here lol");
                    break;
            }
            printMenu();
        }
    }

    public static void download() throws MalformedURLException, ServiceException, IOException {

        //goes through the download queue that has been built and downloads all the songs
        YouTubeService service = new YouTubeService(YT_CLIENT_KEY, YT_API_KEY);
        YouTubeQuery query = new YouTubeQuery(new URL("http://gdata.youtube.com/feeds/api/videos"));
        query.setOrderBy(YouTubeQuery.OrderBy.RELEVANCE);

        for (Track curTrack : downloadQueue) {
            int lfmTime = curTrack.getDuration();
            String searchString = curTrack.getArtist() + " " + curTrack.getName();

            query.setVideoQuery(searchString);
            VideoFeed videoFeed = service.query(query, VideoFeed.class);
            YouTubeMediaGroup mediaGroup = null;

            if (DEBUG_MODE) {
                System.out.println("Researching best video for  " + curTrack.getName() + " by " + curTrack.getArtist());
                System.out.println("Length of track: " + curTrack.getDuration() + " ms.");
            }

            //video feed = list of search results for a song
            for (VideoEntry videoEntry : videoFeed.getEntries()) {
                mediaGroup = videoEntry.getMediaGroup();
                int ytTime = Ints.checkedCast(mediaGroup.getDuration());

                if (DEBUG_MODE) {
                    System.out.println("Duration of current vid: " + ytTime);
                }

                double lowRange, highRange;
                lowRange = lfmTime - ((0.015) * (lfmTime));
                highRange = lfmTime + ((0.015) * (lfmTime));
                //the video in question must be very close to last.fm's reported time (which is probably the accurate one) to be downloaded
                if (DEBUG_MODE) {
                    System.out.println(lowRange + " < " + ytTime + " < " + highRange);
                }
                if (ytTime > lowRange && ytTime < highRange) {
                    //we have a winner!
                    break;
                }
            }

            String url = "www.youtube.com/watch?v=" + mediaGroup.getVideoId();
            String artist = curTrack.getArtist(); //folder names can't end in a period, kill it
            if (artist.length() > 0 && artist.charAt(artist.length() - 1) == '.') {
                artist = artist.substring(0, artist.length() - 1);
            }

            //manageDir(artist); //create the directory for the artist
            youtube_dl(mediaGroup.getVideoId(), artist);

            if (DEBUG_MODE) {
                System.out.println("Downloading from " + url);
                System.out.println("Tagging " + curTrack.getName());
            }
            tag(mediaGroup.getVideoId(), artist, curTrack.getName(), curTrack.getAlbum(), curTrack.getMbid());
        }
        System.out.println("Thanks for using rec_me! Enjoy the music!");
        curChoice = "q"; //die
    }

    public static void youtube_dl(String url, String artist) throws IOException {
        //youtube-dl.exe must be in the same directory
        Runtime rt = Runtime.getRuntime();
        //default file name is song_id.m4a
        String artistDir = "/music/" + "%(id)s.%(ext)s";
        String[] commands = {"youtube-dl", "--format", "bestaudio", "-o", artistDir, url};

        if (DEBUG_MODE) {
            System.out.println("Download command: " + Arrays.toString(commands));
        }

        Process proc = rt.exec(commands);

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        // read the output from the command
        //System.out.println("youtube-dl output:\n");
        String s = null;
        while ((s = stdInput.readLine()) != null) {
            System.out.println(s);
        }

        if (DEBUG_MODE) {
            // read any errors from the attempted command
            System.out.println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null) {
                System.out.println(s);
            }
        }

    }

//    private static void manageDir(String artist) {
//        //create directory
//        File f = new File("/music/" + artist);
//        if (!f.isDirectory()) {
//            boolean success = (new File(artist)).mkdirs();
//            if (!success) {
//                System.out.println("Directory creation failed. ");
//            }
//        }
//
//    }
    private static void tag(String videoId, String artist, String name, String album, String mbid) {

        String fp = "music/" + videoId + ".m4a";
        if (DEBUG_MODE) {
            System.out.println("Tagging " + fp);
        }
        File file = new File("music/" + videoId + ".m4a");
        try {
            //uses jaudiotagger to tag the files after downloading
            
            AudioFile f = AudioFileIO.read(file);
            Tag tag = f.getTag();
            tag.setField(FieldKey.ARTIST, artist);
            //tag.setField(FieldKey.ALBUM, album);
            tag.setField(FieldKey.MUSICBRAINZ_TRACK_ID, mbid);
            tag.setField(FieldKey.TITLE, name);

            f.commit();

        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException | InvalidAudioFrameException | CannotWriteException ex) {
            if(DEBUG_MODE) {
                Logger.getLogger(Rec_me.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        File newName = new File("/music/" + artist + " - " + name + ".m4a");
        boolean success = file.renameTo(newName); //rename to artist - track
        if (!success && true) {
            System.out.println("Failed to rename file.");
        }
    }

    private static void printWelcome() {
        System.out.println("Welcome to rec_me, the automated music downloader powered by last.fm.");
        System.out.println("To get started, you'll need to provide a few pieces of information.\n");

        Scanner in = new Scanner(System.in);
        boolean validInput = false;
        String str = null;

        System.out.println("Last.fm API key: ");
        do {
            str = in.nextLine();
            if (str.trim().length() != 32) {
                System.out.println("Invalid input. Please try again. Length must be 32 characters, alphanumeric.");
            } else {
                validInput = true;
                LFM_API_KEY = str;
            }
        } while (validInput == false);

        validInput = false;

        System.out.println("Last.fm secret key: ");
        do {
            str = in.nextLine();
            if (str.trim().length() != 32) {
                System.out.println("Invalid input. Please try again. Length must be 32 characters, alphanumeric.");
            } else {
                validInput = true;
                LFM_SECRET = str;
            }
        } while (validInput == false);

        validInput = false;

        System.out.println("Youtube API key: ");
        do {
            str = in.nextLine();
            if (str.trim().length() != 98) {
                System.out.println("Invalid input. Please try again. Length must be 98 characters.");
            } else {
                validInput = true;
                YT_API_KEY = str;
            }
        } while (validInput == false);

        validInput = false;

        System.out.println("Last.fm username: ");
        do {
            str = in.nextLine();
            String regex = "^[a-zA-Z0-9-_]+$";

            if (str.trim().length() >= 32 || !str.matches(regex)) {
                System.out.println("Invalid username. Please try again.");
            } else {
                validInput = true;
                user = new lastfmUser(str);
            }
        } while (validInput == false);
    }

    private static void printMenu() {
        Scanner in = new Scanner(System.in);
        boolean validInput = false;
        String str;

        System.out.println("\n1. Get music by user recommendations");
        System.out.println("2. Get music by tag");
        System.out.println("3. Get music by similar artists");
        System.out.println("4. Download the song queue!");
        System.out.println("5. Change settings");
        System.out.println("Q. Quit");
        do {
            str = in.nextLine();
            if (!str.equals("1") && !str.equals("2") && !str.equals("3")
                    && !str.equals("4") && !str.equals("5")
                    && !str.equalsIgnoreCase("q")) {
                System.out.println("Invalid input. Please try again.");
            } else {
                validInput = true;
                curChoice = str;
            }
        } while (validInput == false);
    }

    private static void authenticate() throws URISyntaxException {
        try {
            String token = Authenticator.getToken(LFM_API_KEY);
            String authURL = "http://www.last.fm/api/auth/?api_key=" + LFM_API_KEY + "&" + "token=" + token;

            Desktop d = Desktop.getDesktop();
            d.browse(new URI(authURL));

            System.out.println("Opening authentication page in browser...");
            System.out.println("If the page does not open, authenticate using the following URL:\n" + authURL + ".\n");
            System.out.println("Once you've authenticated with Last.fm, press enter to continue!");

            Scanner in = new Scanner(System.in);
            String str = in.nextLine();

            session = Authenticator.getSession(token, LFM_API_KEY, LFM_SECRET);

        } catch (IOException ex) {
            System.out.println("Something went wrong opening the page.");
        }

    }

    private static void changeSettings() {
        String str;
        Scanner in = new Scanner(System.in);
        boolean validInput = false;

        System.out.println("Turn debug mode on? (yes/no): ");
        do {
            str = in.nextLine();
            if (!str.equals("yes") && !str.equals("no")) {
                System.out.println("Invalid username. Please try again.");
            } else {
                validInput = true;
                switch (str) {
                    case "yes":
                        DEBUG_MODE = true;
                        break;
                    case "no":
                        DEBUG_MODE = false;
                        break;
                }
            }
        } while (validInput == false);

        double param;
        validInput = false;
        System.out.println("Enter song listen parameter 1 (warning, sensitive--0.4 is the default value): ");
        do {
            param = in.nextFloat();
            if (param > 1 || param < 0) {
                System.out.println("Invalid input. Please try again. Number must be between 0 and 1.");
            } else {
                validInput = true;
                SONG_TOLERANCE_1 = param;
            }
        } while (validInput == false);

        validInput = false;
        System.out.println("Enter song listen parameter 2 (warning, sensitive--0.6 is the default value): ");
        do {
            param = in.nextFloat();
            if (param > 1 || param < 0) {
                System.out.println("Invalid input. Please try again. Number must be between 0 and 1.");
            } else {
                validInput = true;
                SONG_TOLERANCE_1 = param;
            }
        } while (validInput == false);
    }

    private static void initSettings() {
        api_sig = md5(LFM_API_KEY);
        downloadQueue = new ArrayList<Track>();
        Caller.getInstance().setUserAgent("rec_me");
        Caller.getInstance().setDebugMode(false);
    }

    private static void addUserRecs() {
        //add songs to dl queue by those recommended by last.fm
        Scanner in = new Scanner(System.in);
        int numArtists;
        boolean validInput = false;
        System.out.println("How many user-recommended artists do you want to sample?");
        do {
            numArtists = in.nextInt();
            if (numArtists > 50 || numArtists < 1) {
                System.out.println("Invalid input. Please try a better number (1-50).");
            } else {
                validInput = true;
            }
        } while (validInput == false);

        PaginatedResult<Artist> recs = User.getRecommendedArtists(session);
        user.setRecArtists(recs.getPageResults());
        Iterator artistItr = user.getRecArtists().iterator();

        addResultsToDownloadQueue(artistItr, numArtists);
    }

    private static void addSongsByTag() {
        //add tracks to dl queue from last.fm's top tracks page for a tag
        // ie http://www.last.fm/tag/electronic/tracks
        Scanner in = new Scanner(System.in);

        int numTracks;
        boolean validInput = false;

        String tag;
        validInput = false;
        System.out.println("What tag do you want to get songs from?");
        do {
            String regex = "^[a-zA-Z0-9-_]+$";
            tag = in.nextLine();
            if (tag.trim().length() >= 32 || !tag.matches(regex)) {
                System.out.println("Invalid tag. Please try again.");
            } else {
                validInput = true;
            }
        } while (validInput == false);

        System.out.println("How many top tracks for that tag do you want?");
        do {
            numTracks = in.nextInt();
            if (numTracks > 50 || numTracks < 1) {
                System.out.println("Invalid input. Please try a better number (1-50).");
            } else {
                validInput = true;
            }
        } while (validInput == false);

        //tag is already being used by jaudiotagger class
        Collection<Track> topTracksForTag = de.umass.lastfm.Tag.getTopTracks(tag, api_sig);
        Iterator<Track> itr = topTracksForTag.iterator();
        while (itr.hasNext()) {
            Track track = itr.next();
            if(DEBUG_MODE) {
                System.out.println(track.getName() + " is a good track. Adding to download list.");
            }
            downloadQueue.add(itr.next());
        }
    }

    private static void addSongsBySimilarArtist() {
        //add songs to dl queue that are from artists similar to the specified one
        Scanner in = new Scanner(System.in);
        int numArtists;
        String artist;
        boolean validInput = false;
        System.out.println("By which artist do you want tracks similar to?");
        artist = in.nextLine();

        System.out.println("How many artists do you want to sample?");
        do {
            numArtists = in.nextInt();
            if (numArtists > 50 || numArtists < 1) {
                System.out.println("Invalid input. Please try a better number (1-50).");
            } else {
                validInput = true;
            }
        } while (validInput == false);

        Collection<Artist> recs = Artist.getSimilar(artist, api_sig);
        Iterator artistItr = recs.iterator();

        addResultsToDownloadQueue(artistItr, numArtists);
    }

    private static void addResultsToDownloadQueue(Iterator artistItr, int numArtists) {
        //goes through the iterator and adds the songs to the dl queue if they are good enough
        int i = 0;
        while (artistItr.hasNext()) {
            i++;
            Artist curArtist = (Artist) artistItr.next();
            System.out.println(curArtist.getName() + ": ");
            Collection<Track> topTracks = curArtist.getTopTracks(curArtist.getName(), LFM_API_KEY);

            Iterator trackItr = topTracks.iterator();

            Track trackOne = (Track) trackItr.next();

            Track curTrack = null;
            Track prevTrack = trackOne;

            downloadQueue.add(trackOne); //always download first track by an artist

            System.out.println(trackOne.getName() + " is a good track and will be downloaded.");
            while (trackItr.hasNext()) {
                curTrack = (Track) trackItr.next();
                double listenerRatioOne = (double) curTrack.getListeners() / trackOne.getListeners(); //comparison to first song
                double listenerRatioTwo = (double) curTrack.getListeners() / prevTrack.getListeners();//comparison to next song

                if (listenerRatioOne > SONG_TOLERANCE_1 && listenerRatioTwo > SONG_TOLERANCE_2) {
//                    if (DEBUG_MODE) {
                        System.out.println(curTrack.getName() + " is a good track and will be downloaded.");
//                    }
                    downloadQueue.add(curTrack);
                }

                prevTrack = curTrack;
            }
            System.out.println();

            if (i >= numArtists) {
                break;
            }
        }
    }
}
