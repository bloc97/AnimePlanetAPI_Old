/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package animeplanetapi;

import java.util.List;

/**
 *
 * @author bowen
 */
class AnimePage extends AnimePreview {
    
    public AnimePage(String title, String altTitle, String type, String episodes, String minutesPerEpisode, String studio, String beginYear, String endYear, String rating, String description, String source, List<String> tags, String thumbnailUrl) {
        super(title, altTitle, type, episodes, minutesPerEpisode, studio, beginYear, endYear, rating, description, source, tags, thumbnailUrl);
    }
    
    
}
