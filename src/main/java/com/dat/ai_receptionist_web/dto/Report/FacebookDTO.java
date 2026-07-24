package com.dat.ai_receptionist_web.dto.Report;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

public class FacebookDTO {

    // ==================================================================
    // ========================== VIDEOS ===============================
    // ==================================================================

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Videos {
        String id;
        String description;
        Double length;       // số thực, vd: 26.145
        Long views;          // = fb_reels_total_plays
        VideoInsightsWrapper video_insights;
    }

    // Wrapper cho { "data": [...] }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VideoInsightsWrapper {
        List<VideoInsightItem> data;
    }

    // Mỗi phần tử trong video_insights.data[]
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VideoInsightItem {
        String name;       // vd: "post_video_likes_by_reaction_type"
        String period;     // "lifetime"
        List<InsightValue> values;
        String title;
        String description;
        String id;
    }

    // Mỗi phần tử trong values[] — value có thể là Long, Map, hoặc Object
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InsightValue {
        Object value; // dùng Object vì value có thể là: Long, Map<String, Long>, Map<String, Double>
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AggregatedInsights {
        int videoCount;
        long totalPlays;
        long totalUniqueReach;
        long totalReactions;
        long totalComments;
        long totalShares;
        long totalViewTimeMs;
        double avgEngagementRate;
        String topVideoId;
        long topVideoPlays;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VideoSummary {
        String videoId;
        String description;
        Double lengthSeconds;
        Long totalPlays;
        Long uniqueReach;
        Long replayCount;
        Long newFollowers;
        Long totalReactions;
        Long comments;
        Long shares;
        double engagementRate;      // %
        double avgWatchTimeSeconds;
        double watchThroughRate;    // %
        int biggestDropOffSecond;
    }

    // DTO đã được parse/flatten để dùng trong service/response
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VideoInsightsParsed {
        // Metadata từ Videos object
        String videoId;
        String description;
        Double lengthSeconds;
        Long views;

        // post_video_likes_by_reaction_type
        Map<String, Long> likesByReactionType;

        // post_video_avg_time_watched (ms)
        Long avgTimeWatchedMs;

        // post_video_social_actions
        Long shares;
        Long comments;

        // post_video_view_time (ms)
        Long totalViewTimeMs;

        // post_impressions_unique
        Long uniqueReach;

        // blue_reels_play_count
        Long reelsPlayCount;

        // fb_reels_total_plays
        Long totalPlays;

        // fb_reels_replay_count
        Long replayCount;

        // post_video_retention_graph — key là giây (String "0".."40"), value là tỉ lệ 0.0-1.0
        Map<String, Double> retentionGraph;

        // post_video_followers
        Long newFollowers;
    }

    // Response wrapper cho toàn bộ danh sách video
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class VideosResponse {
        List<Videos> data;
        Paging paging;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Paging {
        Cursors cursors;
        String next;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Cursors {
        String before;
        String after;
    }

    // ==================================================================
    // =========================== POSTS ===============================
    // ==================================================================
    // Khớp với response của:
    // GET /{page-id}/posts?fields=message,shares,comments.summary(total_count),
    //      insights.metric(post_reactions_by_type_total,post_clicks_by_type),
    //      attachments{media_type}

    /**
     * Một Post lấy từ Facebook Graph API.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Posts {
        String id;
        String message;
        SharesInfo shares;              // có thể null vì FB chỉ trả field này khi post có >=1 share
        CommentsInfo comments;
        PostInsightsWrapper insights;
        AttachmentsWrapper attachments; // có thể null nếu post không có đính kèm
    }

    // { "count": 3 }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class SharesInfo {
        Long count;
    }

    // { "data": [], "summary": { "total_count": 5 } }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CommentsInfo {
        List<Object> data;     // FB trả "data": [] khi không request expand chi tiết comment
        CommentsSummary summary;
    }

    // { "total_count": 5 }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CommentsSummary {
        Long total_count;
    }

    // insights của Post có thêm "paging" (previous/next) so với insights của Video
    // { "data": [...], "paging": { "previous": "...", "next": "..." } }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PostInsightsWrapper {
        List<PostInsightItem> data;
        InsightsPaging paging;
    }

    // Mỗi phần tử trong insights.data[]
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PostInsightItem {
        String name;        // "post_reactions_by_type_total" | "post_clicks_by_type"
        String period;       // "lifetime"
        List<InsightValue> values;
        String title;
        String description;
        String id;
    }

    // paging riêng của insights (khác với Paging ở cấp top-level danh sách post)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class InsightsPaging {
        String previous;
        String next;
    }

    // { "data": [ { "media_type": "video" } ] }
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AttachmentsWrapper {
        List<AttachmentItem> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AttachmentItem {
        String media_type;   // "video" | "album" | "photo" | ...
    }

    // Response wrapper cho toàn bộ danh sách post (top-level "data" + "paging")
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PostsResponse {
        List<Posts> data;
        Paging paging;   // tái sử dụng Paging (cursors + next) đã định nghĩa ở phần Video
    }

    // DTO đã được parse/flatten để dùng trong service/response
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PostInsightsParsed {
        // Metadata từ Posts object
        String postId;
        String message;

        Long shareCount;     // từ shares.count, 0 nếu không có
        Long commentCount;   // từ comments.summary.total_count

        // post_reactions_by_type_total -> {"like":16,"love":5,"wow":1,"haha":2,"sorry":1}
        Map<String, Long> reactionsByType;

        // post_clicks_by_type -> {"other clicks":22,"photo view":195,"link clicks":1}
        Map<String, Long> clicksByType;

        // attachments.data[].media_type
        List<String> mediaTypes;
    }

    // Tóm tắt Post dễ dùng cho report/UI
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class PostSummary {
        String postId;
        String message;             // đã truncate
        String primaryMediaType;    // attachment đầu tiên, null nếu post không có đính kèm
        Long totalReactions;
        Long comments;
        Long shares;
        Long totalClicks;
        long totalEngagement;       // reactions + comments + shares
        Map<String, Long> reactionBreakdown;
        Map<String, Long> clickBreakdown;
    }

    // Tổng hợp metrics của nhiều post (dùng cho dashboard overview)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class AggregatedPostInsights {
        int postCount;
        long totalReactions;
        long totalComments;
        long totalShares;
        long totalClicks;
        long totalEngagement;
        double avgEngagementPerPost;
        String topPostId;
        long topPostEngagement;
    }
}