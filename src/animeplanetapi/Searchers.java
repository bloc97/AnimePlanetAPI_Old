/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package animeplanetapi;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author bowen
 */
public abstract class Searchers {
    
    public final static String mainUrl = "http://www.anime-planet.com";
    
    
    public static List<AnimePreview> searchAnimeByName(String name) {
        name = Parsers.formatIllegalSpace(name);
        List<AnimePreview> animePreviewList = new LinkedList();
        
        try {
            
            Document mainDoc = Jsoup.connect("http://www.anime-planet.com/anime/all?name=" + name + "&sort=status_1&order=desc").get();
            
            
            if (isDocumentFullAnimePage(mainDoc)) {
                animePreviewList.add(fetchFullAnimeFromPage(mainDoc));
            } else {
                animePreviewList = fetchAnimesFromListPage(mainDoc);
            }
            
        } catch (Exception ex) {
        }
        return animePreviewList;
    }
    
    public static AnimePage getAnimeByName(String name) {
        name = Parsers.formatIllegalSpace(name);
        
        try {
            
            Document mainDoc = Jsoup.connect("http://www.anime-planet.com/anime/all?name=" + name + "&sort=status_1&order=desc").get();
            
            
            if (isDocumentFullAnimePage(mainDoc)) {
                return fetchFullAnimeFromPage(mainDoc);
            } else {
                try {
                    AnimePreview ap = fetchAnimesFromListPage(mainDoc).get(0);
                    Document fullDoc = Jsoup.connect(ap.getUrl()).get();
                    return fetchFullAnimeFromPage(fullDoc);
                } catch (Exception ex) {
                }
            }
            
        } catch (Exception ex) {
        }
        return new AnimePage(-1, "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", "", new AnimeUserStats(0, 0, 0, 0, 0, 0), "", "", Arrays.asList(new String[] {"None"}), "");
    }
    
    public static boolean isDocumentFullAnimePage(Document mainDoc) {
        Elements elements = mainDoc.body().getElementsByAttributeValue("itemprop", "name");
        for (Element element : elements) {
            if (element.tagName().equalsIgnoreCase("h1")) {
                return true;
            }
        }
        return false;
    }
    
    
    public static AnimePage fetchFullAnimeFromPage(Document mainDoc) {
        
        Element entryBar = mainDoc.body().getElementById("siteContainer").getElementsByClass("pure-g entryBar").first();
        
        //System.out.println(entryBar);
        
        String type = "", episodes = "N/A", minutesPerEpisode = "N/A",
               studio = "N/A", studioUrl = "N/A",
               beginYear = "", endYear = "", beginYearUrl = "N/A", endYearUrl = "N/A", season = "N/A", seasonUrl = "N/A",
               rating = "N/A", ratingCount = "N/A", rank = "N/A";
                
        String typeString = entryBar.getElementsByClass("type").first().text();
        Map<String, String> typeMap = Parsers.parseType(typeString);
        type = typeMap.get("type");
        episodes = typeMap.get("episodes");
        minutesPerEpisode = typeMap.get("minutesPerEpisode");
        
        Elements studioElement = entryBar.getElementsByAttributeValueStarting("href", "/anime/studios/");
        if (!studioElement.isEmpty()) {
            studio = studioElement.first().text();
            studioUrl = mainUrl + studioElement.attr("href");
        }
        
        
        String yearString = "TBA";
        try {
            Element yearElement = entryBar.getElementsByClass("iconYear").first();
            yearString = yearElement.text();
            
            Elements yearElements = yearElement.getElementsByAttributeValueStarting("href", "/anime/years/");

            for (Element eachYearElement : yearElements) {
                if (eachYearElement.text().equalsIgnoreCase(beginYear)) {
                    beginYearUrl = mainUrl + eachYearElement.attr("href");
                } else if (eachYearElement.text().equalsIgnoreCase(endYear)) {
                    endYearUrl = mainUrl + eachYearElement.attr("href");
                }
            }

            Elements seasonElements = yearElement.parent().getElementsByAttributeValueStarting("href", "/anime/seasons/");

            if (!seasonElements.isEmpty()) {
                season = seasonElements.first().text();
                seasonUrl = mainUrl + seasonElements.first().attr("href");
            }
        } catch (Exception ex) {
        }
        Map<String, String> yearMap = Parsers.parseYear(yearString);
        beginYear = yearMap.get("beginYear");
        endYear = yearMap.get("endYear");
        
        
        try {
            rating = ((Double)(Double.parseDouble(entryBar.getElementsByAttributeValue("itemprop", "aggregateRating").first().getElementsByAttributeValue("itemprop", "ratingValue").attr("content"))/2D)).toString();
            ratingCount = entryBar.getElementsByAttributeValue("itemprop", "aggregateRating").first().getElementsByAttributeValue("itemprop", "ratingCount").attr("content");
            rank = entryBar.getElementsByAttributeValue("itemprop", "aggregateRating").first().parent().nextElementSibling().text();
            rank = rank.substring(rank.indexOf('#') + 1);
        } catch (Exception ex) {
        }
        
        String title = mainDoc.body().getElementsByAttributeValue("itemprop", "name").first().text();
        String altTitle = "";
        try {
            altTitle = mainDoc.body().getElementsByClass("aka").first().text();
            altTitle = altTitle.substring(altTitle.indexOf(':') + 1).trim();
        } catch (Exception ex) {
        }
        
        Element descriptionElement = mainDoc.body().getElementsByAttributeValue("itemprop", "description").first();
        
        
        String idString = descriptionElement.parent().parent().parent().getElementsByAttributeValue("data-mode", "anime").attr("data-id");
        
        int id;
        try {
            id = Integer.parseInt(idString);
        } catch (Exception ex) {
            id = -1;
        }
        
        String url = mainDoc.head().getElementsByAttributeValue("rel", "canonical").attr("href");
        
        String desc = descriptionElement.text();
        String source = "N/A";
        try {
            source = descriptionElement.parent().getElementsByClass("notes").first().text().substring(8);
        } catch (Exception ex) {
        }
        LinkedList<String> tags = new LinkedList();
        try {
            for (Element e : descriptionElement.parent().getElementsByClass("categories").first().getElementsByAttributeValue("itemprop", "genre")) {
                tags.add(e.text());
            }
        } catch (Exception ex) {
            tags.add("None");
        }
        
        String thumbUrl = mainUrl + mainDoc.body().getElementsByAttributeValue("itemprop", "image").first().attr("src");
        
        int[] intUserStats = new int[6];
        
        
        try {
            Document statsDoc = Jsoup.connect("http://www.anime-planet.com/ajaxDelegator.php?mode=stats&type=anime&id=" + id + "&url=null").get();
            Elements userStatsList = statsDoc.body().getElementsByClass("slCount");
            int i = 0;
            for (Element e : userStatsList) {
                intUserStats[i] = Integer.parseInt(e.text().replaceAll(",", ""));
                i++;
            }
            
        } catch (Exception ex) {
        }
        
        AnimeUserStats userStats = new AnimeUserStats(intUserStats[0], intUserStats[1], intUserStats[2], intUserStats[3], intUserStats[4], intUserStats[5]);
        
        
        
        return new AnimePage(id, url, title, altTitle, type, episodes, minutesPerEpisode, studio, studioUrl, beginYear, endYear, beginYearUrl, endYearUrl, season, seasonUrl, rating, ratingCount, rank, userStats, desc, source, tags, thumbUrl);
    }
    
    protected static List<AnimePreview> fetchAnimesFromListPage(Document mainDoc) {
        LinkedList<AnimePreview> animePreviewList = new LinkedList();

        //Elements elements = doc.body().getElementsByAttributeValue("data-type", "anime");
        Elements foundList = mainDoc.body().getElementsByAttributeValue("data-type", "anime").first().getElementsByTag("li");

        for (Element animeListElement : foundList) {
            animePreviewList.add(getAnimePreviewFromListElement(animeListElement));
        }
        return animePreviewList;
    }
    
    protected static AnimePreview getAnimePreviewFromListElement(Element animeListElement) {
        
        String idString = animeListElement.attr("data-id");
        
        int id;
        try {
            id = Integer.parseInt(idString);
        } catch (Exception ex) {
            id = -1;
        }
        
        String url = mainUrl + animeListElement.getElementsByAttributeValueStarting("href", "/anime/").attr("href");
        
        Document animePreviewDoc = Jsoup.parse(animeListElement.getElementsByTag("a").first().attributes().get("title"));
        //System.out.println(animePreviewDoc);

        String title = animePreviewDoc.body().getElementsByTag("h5").first().text();
        String altTitle = "";
        try {
            altTitle = animePreviewDoc.body().getElementsByClass("aka").first().text().substring(11);
        } catch (Exception ex) {
        }

        String type = "", episodes = "N/A", minutesPerEpisode = "N/A",
               studio = "N/A",
               beginYear = "", endYear = "",
               rating = "N/A";

        boolean foundStudio = false;

        Elements entryBarElements = animePreviewDoc.body().getElementsByClass("entryBar").first().getElementsByTag("li");

        for (Element headerElement : entryBarElements) {
            if (headerElement.hasClass("type")) {

                String typeString = headerElement.text();
                Map<String, String> typeMap = Parsers.parseType(typeString);

                type = typeMap.get("type");
                episodes = typeMap.get("episodes");
                minutesPerEpisode = typeMap.get("minutesPerEpisode");

            } else if (headerElement.hasClass("iconYear")) {
                String yearString = headerElement.text();

                Map<String, String> yearMap = Parsers.parseYear(yearString);
                beginYear = yearMap.get("beginYear");
                endYear = yearMap.get("endYear");

            } else {
                if (headerElement.getElementsByClass("ttRating").isEmpty()) {
                    studio = headerElement.text();
                } else {
                    rating = headerElement.getElementsByClass("ttRating").first().text();
                }
            }

        }

        String desc = animePreviewDoc.body().getElementsByTag("p").first().text();
        String source = "N/A";
        try {
            source = animePreviewDoc.body().getElementsByClass("notes").first().getAllElements().first().text().substring(8);
        } catch (Exception ex) {
        }

        LinkedList<String> tags = new LinkedList();
        try {
            for (Element e : animePreviewDoc.body().getElementsByClass("categories").first().getElementsByTag("ul").first().getElementsByTag("li")) {
                tags.add(e.text());
            }
        } catch (Exception ex) {
            tags.add("None");
        }
        
        String thumbUrl = mainUrl + animeListElement.getElementsByTag("img").first().attributes().get("src");
        return new AnimePreview(id, url, title, altTitle, type, episodes, minutesPerEpisode, studio, beginYear, endYear, rating, desc, source, tags, thumbUrl);
    }
    
    
}

