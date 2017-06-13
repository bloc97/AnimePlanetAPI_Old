/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package animeplanetapi;

import java.io.IOException;
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
        List<AnimePreview> animePreviewList = new LinkedList();
        
        try {
            
            Document mainDoc = Jsoup.connect("http://www.anime-planet.com/anime/all?name=" + name).get();
            
            
            if (isDocumentFullAnimePage(mainDoc)) {
                animePreviewList.add(fetchFullAnimeFromPage(mainDoc));
            } else {
                animePreviewList = fetchAnimesFromListPage(mainDoc);
            }
            
        } catch (IOException ex) {
        }
        return animePreviewList;
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
        
        System.out.println(entryBar);
        
        String type = "", episodes = "N/A", minutesPerEpisode = "N/A",
               studio = "N/A", studioUrl = "N/A",
               beginYear = "", endYear = "",
               rating = "N/A";
                
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
        
        Element yearElement = entryBar.getElementsByClass("datePublished").first();
        String yearString = yearElement.text();
        Map<String, String> yearMap = Parsers.parseYear(yearString);
        beginYear = yearMap.get("beginYear");
        endYear = yearMap.get("endYear");
        
        List<String> yearUrls = yearElement.getElementsByAttributeValueStarting("href", "/anime/years/").eachAttr("href");
        yearUrls.replaceAll((String url) -> mainUrl + url);
        
        return null;
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

                String url = mainUrl + animeListElement.getElementsByTag("img").first().attributes().get("src");
                return new AnimePreview(title, altTitle, type, episodes, minutesPerEpisode, studio, beginYear, endYear, rating, desc, source, tags, url);
    }
    
    
}

