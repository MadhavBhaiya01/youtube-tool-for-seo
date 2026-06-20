package com.YouTubeTools.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.YouTubeTools.Model.SearchVideo;
import com.YouTubeTools.Model.Video;
import com.YouTubeTools.Model.VideoDetails;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class YouTubeService {

    private final WebClient.Builder webClientBuilder;

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${youtube.api.base.url}")
    private String baseUrl;

    @Value("${youtube.api.max.related.videos}")
    private int maxRelatedVideos;

    /**
     * Search videos by title
     */
    public SearchVideo searchVideos(String videoTitle) {

        List<String> videoIds = searchForVideoIds(videoTitle);

        if (videoIds.isEmpty()) {
            return SearchVideo.builder()
                    .primaryVideo(null)
                    .relatedVideos(Collections.emptyList())
                    .build();
        }

        String primaryVideoId = videoIds.get(0);

        List<String> relatedVideoIds = videoIds.size() > 1
                ? videoIds.subList(
                        1,
                        Math.min(videoIds.size(), maxRelatedVideos + 1))
                : Collections.emptyList();

        Video primaryVideo = getVideoById(primaryVideoId);

        List<Video> relatedVideos = new ArrayList<>();

        for (String id : relatedVideoIds) {

            Video video = getVideoById(id);

            if (video != null) {
                relatedVideos.add(video);
            }
        }

        return SearchVideo.builder()
                .primaryVideo(primaryVideo)
                .relatedVideos(relatedVideos)
                .build();
    }

    /**
     * Search YouTube and get video IDs
     */
    private List<String> searchForVideoIds(String videoTitle) {

        SearchApiResponse response = webClientBuilder
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search")
                        .queryParam("part", "snippet")
                        .queryParam("q", videoTitle)
                        .queryParam("type", "video")
                        .queryParam("maxResults", maxRelatedVideos + 1)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(SearchApiResponse.class)
                .block();

        if (response == null
                || response.items == null
                || response.items.isEmpty()) {

            return Collections.emptyList();
        }

        List<String> videoIds = new ArrayList<>();

        for (SearchItem item : response.items) {

            if (item != null
                    && item.id != null
                    && item.id.videoId != null) {

                videoIds.add(item.id.videoId);
            }
        }

        return videoIds;
    }

    /**
     * Get complete video details
     */
    public VideoDetails getVideoDetails(String videoId) {

        VideoApiResponse response = webClientBuilder
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/videos")
                        .queryParam("part", "snippet")
                        .queryParam("id", videoId)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(VideoApiResponse.class)
                .block();

        if (response == null
                || response.items == null
                || response.items.isEmpty()) {

            return null;
        }

        Snippet snippet = response.items.get(0).snippet;

        if (snippet == null) {
            return null;
        }

        String thumbnailUrl = "";

        if (snippet.thumbnails != null) {
            thumbnailUrl = snippet.thumbnails.getBestThumbnailUrl();
        }

        return VideoDetails.builder()
                .id(videoId)
                .title(snippet.title)
                .description(snippet.description)
                .tags(snippet.tags == null
                        ? Collections.emptyList()
                        : snippet.tags)
                .thumbnailUrl(thumbnailUrl)
                .channelTitle(snippet.channelTitle)
                .publishedAt(snippet.publishedAt)
                .build();
    }

    /**
     * Get single video information
     */
    private Video getVideoById(String videoId) {

        VideoApiResponse response = webClientBuilder
                .baseUrl(baseUrl)
                .build()
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/videos")
                        .queryParam("part", "snippet")
                        .queryParam("id", videoId)
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(VideoApiResponse.class)
                .block();

        if (response == null
                || response.items == null
                || response.items.isEmpty()) {

            return null;
        }

        Snippet snippet = response.items.get(0).snippet;

        if (snippet == null) {
            return null;
        }

        return Video.builder()
                .id(videoId)
                .title(snippet.title)
                .channelTitle(snippet.channelTitle)
                .tags(snippet.tags == null
                        ? Collections.emptyList()
                        : snippet.tags)
                .build();
    }

    // =========================================================
    // SEARCH API RESPONSE
    // =========================================================

    @Data
    static class SearchApiResponse {
        List<SearchItem> items;
    }

    @Data
    static class SearchItem {
        Id id;
    }

    @Data
    static class Id {
        String videoId;
    }

    // =========================================================
    // VIDEO API RESPONSE
    // =========================================================

    @Data
    static class VideoApiResponse {
        List<VideoItem> items;
    }

    @Data
    static class VideoItem {
        Snippet snippet;
    }

    @Data
    static class Snippet {

        String title;
        String description;
        String channelTitle;
        String publishedAt;

        List<String> tags;

        Thumbnails thumbnails;
    }

    @Data
    static class Thumbnails {

        Thumbnail maxres;
        Thumbnail high;
        Thumbnail medium;

        @JsonProperty("default")
        Thumbnail defaultThumbnail;

        public String getBestThumbnailUrl() {

            if (maxres != null)
                return maxres.url;

            if (high != null)
                return high.url;

            if (medium != null)
                return medium.url;

            if (defaultThumbnail != null)
                return defaultThumbnail.url;

            return "";
        }
    }

    @Data
    static class Thumbnail {
        String url;
    }
}