package com.YouTubeTools.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.YouTubeTools.Model.VideoDetails;
import com.YouTubeTools.Service.ThumbnailService;
import com.YouTubeTools.Service.YouTubeService;

@Controller
public class YouTubeVideoController {

    private final YouTubeService youTubeService;
    private final ThumbnailService thumbnailService;

    public YouTubeVideoController(
            YouTubeService youTubeService,
            ThumbnailService thumbnailService) {

        this.youTubeService = youTubeService;
        this.thumbnailService = thumbnailService;
    }

    @GetMapping("/youtube/video-details")
    public String showVideoForm() {
        return "video-details";
    }

    @PostMapping("/youtube/video-details")
    public String fetchVideoDetails(
            @RequestParam("videoUrlOrId") String videoUrlOrId,
            Model model) {

        String videoId = thumbnailService.extractVideoId(videoUrlOrId);

        if (videoId == null) {
            model.addAttribute(
                    "error",
                    "Invalid YouTube URL or Video ID");
            return "video-details";
        }

        VideoDetails details = youTubeService.getVideoDetails(videoId);

        if (details == null) {

            model.addAttribute(
                    "error",
                    "Video not found or unavailable");

        } else {

            model.addAttribute(
                    "videoDetails",
                    details);
        }

        model.addAttribute(
                "videoUrlOrId",
                videoUrlOrId);

        return "video-details";
    }
}