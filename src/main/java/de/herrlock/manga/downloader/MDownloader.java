package de.herrlock.manga.downloader;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.imageio.ImageIO;

import de.herrlock.log.Logger;
import de.herrlock.manga.host.ChapterList;
import de.herrlock.manga.host.ChapterList.Chapter;
import de.herrlock.manga.util.Constants;
import de.herrlock.manga.util.Utils;

public class MDownloader {

    /**
     * a Scanner to {@link System.in}
     */
    private static Scanner sc;

    private static Logger L;

    public static void execute(InputStream in) {
        L = Utils.getLogger();

        try {
            L.trace();
            try (Scanner _sc = new Scanner(in, "UTF-8")) {
                sc = _sc;
                new MDownloader().run();
            }
        }
        catch (RuntimeException ex) {
            L.error(ex);
            throw ex;
        }
        catch (Exception ex) {
            L.error(ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * the parent-folder to write the pages into
     */
    private File path;
    /**
     * a {@link ChapterList}-Instance containing the {@link URL}s of the {@link Chapter}s
     */
    private ChapterList chapterlist;
    /**
     * a {@link Map} containing the {@link URL}s of all the pages
     */
    private Map<String, Map<Integer, URL>> picturemap;
    /**
     * the chapters that failed the download
     */
    private List<DoLaterChapter> doAfterwards = new ArrayList<>(0);

    MDownloader() {
        // nothing to init
    }

    private void run() {
        L.trace();
        try {
            createChapterList();
            if (goon1()) {
                createPictureLinks();
                if (goon2()) {
                    downloadAll();
                }
                else {
                    L.none("bye");
                }
            }
            else {
                L.none("bye");
            }
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private boolean goon1() {
        int noOfChapters = this.chapterlist.size();
        if (noOfChapters > 0) {
            L.none(noOfChapters + " chapter" + (noOfChapters > 1 ? "s" : "") + " availabile.");
            return goon();
        }
        L.warn("no chapters availabile, exiting");
        return false;
    }

    private boolean goon2() {
        int noOfPictures = 0;
        for (Map<Integer, URL> m : this.picturemap.values()) {
            noOfPictures += m.size();
        }
        if (noOfPictures > 0) {
            L.none(noOfPictures + " page" + (noOfPictures > 1 ? "s" : "") + " availabile.");
            return goon();
        }
        L.warn("no pictures availabile, exiting");
        return false;
    }

    private static boolean goon() {
        L.none("go on? y|n");
        try {
            char c = sc.next(".+").charAt(0);
            return c == 'y' || c == 'Y';
        }
        catch (NoSuchElementException ex) {
            return false;
        }
    }

    private void createChapterList() throws IOException {
        L.trace();
        this.chapterlist = ChapterList.getInstance();
        String mangaName = this.chapterlist.getMangaName().toLowerCase(Locale.ENGLISH).replace(' ', '_');
        this.path = new File(Constants.TARGET_FOLDER, mangaName);
        L.none("Save to: " + this.path.getAbsolutePath());
    }

    private void createPictureLinks() throws IOException {
        L.trace();
        if (this.chapterlist != null) {
            this.picturemap = new HashMap<>(this.chapterlist.size());
            for (Chapter chapter : this.chapterlist) {
                Map<Integer, URL> pictureMap = this.chapterlist.getAllPageURLs(chapter);
                this.picturemap.put(chapter.getNumber(), pictureMap);
            }
        }
        else {
            String message = "ChapterList not initialized";
            L.error(message);
            throw new RuntimeException(message);
        }
    }

    private void downloadAll() throws IOException {
        L.trace();
        if (this.picturemap != null) {
            List<String> keys = new ArrayList<>(this.picturemap.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                downloadChapter(key);
            }
        }
        else {
            String message = "PageMap not initialized";
            L.error(message);
            throw new RuntimeException(message);
        }
    }

    private void downloadChapter(String key) throws IOException {
        Map<Integer, URL> urlMap = this.picturemap.get(key);
        L.none("Download chapter " + key + " - " + urlMap.size() + " pages");
        File chapterFolder = new File(this.path, key);
        if (chapterFolder.exists() || chapterFolder.mkdirs()) {
            for (Map.Entry<Integer, URL> e : urlMap.entrySet()) {
                dlPic(e.getValue(), chapterFolder, e.getKey());
            }
            downloadFailedPages();
        }
        L.none("finished chapter " + key);
    }

    private void downloadFailedPages() throws IOException {
        List<DoLaterChapter> list = new ArrayList<>(this.doAfterwards);
        this.doAfterwards.clear();
        for (DoLaterChapter c : list) {
            dlPic(c.pageUrl, c.chapterFolder, c.pageNumber);
        }
        if (!this.doAfterwards.isEmpty()) {
            downloadFailedPages();
        }
    }

    private void dlPic(URL pageUrl, File chapterFolder, int pageNumber) throws IOException {
        URL imageUrl = this.chapterlist.imgLink(pageUrl);
        URLConnection con = Utils.getConnection(imageUrl);
        try (InputStream in = con.getInputStream()) {
            L.debug("read image " + imageUrl);
            BufferedImage image = ImageIO.read(in);
            File output = new File(chapterFolder, pageNumber + ".jpg");
            L.debug("write to " + output);
            ImageIO.write(image, "jpg", output);
            L.info("Chapter " + chapterFolder.getName() + ", Page " + pageNumber + " - finished");
        }
        catch (SocketException | SocketTimeoutException ex) {
            L.warn("Chapter " + chapterFolder.getName() + ", Page " + pageNumber + " - " + ex.getMessage());
            this.doAfterwards.add(new DoLaterChapter(pageUrl, chapterFolder, pageNumber));
        }
    }
}

class DoLaterChapter {
    final URL pageUrl;
    final File chapterFolder;
    final int pageNumber;

    public DoLaterChapter(URL pageUrl, File chapterFolder, int pageNumber) {
        this.pageUrl = pageUrl;
        this.chapterFolder = chapterFolder;
        this.pageNumber = pageNumber;
    }
}
