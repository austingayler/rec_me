# rec_me
Gets the best songs from recommended artists on last.fm and downloads them from Youtube after matching the songs using the duration of the song provided by Last.fm. If an artist has a lot of songs that are highly played, the downloader will grab more of them--if the artist has only one or two highly played songs, it will just get those. Downloaded songs are put in a /music/ folder.
Uses the Java Last.fm API bindings, Google Data API, jaudiotagger, and youtube-dl.

Dependencies: 
* lastfm-java-0.1.2.jar
*  gdata-client-1.0.jar
*  gdata-youtube-2.0.jar
*  gdata-core-1.0.jar
*  gdata-media-1.0.jar
*  guava-18.0.jar
*  jaudiotagger-2.2.3.jar
*  javax.mail.jar

Example output:
```
Welcome to rec_me, the automated music downloader powered by Last.fm.
To get started, you'll need to provide a few pieces of information.

Last.fm API key: 
...
Last.fm secret key: 
...
Youtube API key: 
...
Last.fm username: 
hello_world

Opening authentication page in browser...
If the page does not open, authenticate using the following URL:
http://www.last.fm/api/auth/?api_key=e245b01abfe1ecab4033890867167374&token=75b8c2c0974a6714cba01f7110d5eb33.

Once you've authenticated with Last.fm, press enter to continue!

1. Get music by user recommendations
2. Get music by tag
3. Get music by similar artists
4. Download the song queue!
5. Change settings
Q. Quit
1

How many user-recommended artists do you want to sample?
3

Discovery: 
Swing Tree is a good track and will be downloaded.

Ra Ra Riot: 
Dance With Me is a good track and will be downloaded.
Beta Love is a good track and will be downloaded.
Boy is a good track and will be downloaded.
Can You Tell is a good track and will be downloaded.

Dale Earnhardt Jr. Jr.: 
James Dean is a good track and will be downloaded.
Simple Girl is a good track and will be downloaded.


1. Get music by user recommendations
2. Get music by tag
3. Get music by similar artists
4. Download the song queue!
5. Change settings
Q. Quit
4

[youtube] wqOlC1iyTyA: Downloading webpage
[youtube] wqOlC1iyTyA: Extracting video information
[youtube] wqOlC1iyTyA: Downloading DASH manifest
[download] Destination: music\wqOlC1iyTyA.m4a

[download]   0.0% of 4.36MiB at  9.26KiB/s ETA 08:01
...
[download] 100.0% of 4.36MiB at 258.97KiB/s ETA 00:00
[download] 100% of 4.36MiB in 00:17                  

Thanks for using rec_me! Enjoy the music!
```
