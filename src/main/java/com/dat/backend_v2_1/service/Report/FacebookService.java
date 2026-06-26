package com.dat.backend_v2_1.service.Report;

import com.dat.backend_v2_1.dto.Report.FacebookDTO.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FacebookService {

    private final ObjectMapper objectMapper;

    // ==================================================================
    // ========================= VIDEO ENTRY POINT =======================
    // ==================================================================

    /**
     * Parse toàn bộ response từ Facebook Graph API
     * Input: VideosResponse (raw JSON đã deserialize)
     * Output: List<VideoInsightsParsed> đã flatten
     */
    public List<VideoInsightsParsed> parseVideosResponse(VideosResponse response) {
        if (response == null || response.getData() == null) {
            log.warn("VideosResponse is null or has no data");
            return Collections.emptyList();
        }

        return response.getData().stream()
                .map(this::parseVideoWithInsights)
                .collect(Collectors.toList());
    }

    /**
     * Parse một Video object thành VideoInsightsParsed đầy đủ
     */
    public VideoInsightsParsed parseVideoWithInsights(Videos video) {
        if (video == null) {
            log.warn("Video object is null");
            return VideoInsightsParsed.builder().build();
        }

        List<VideoInsightItem> items = Optional.ofNullable(video.getVideo_insights())
                .map(VideoInsightsWrapper::getData)
                .orElse(Collections.emptyList());

        VideoInsightsParsed parsed = parseInsights(items);

        // Gán thêm thông tin cơ bản từ Video
        parsed.setVideoId(video.getId());
        parsed.setDescription(video.getDescription());
        parsed.setLengthSeconds(video.getLength());
        parsed.setViews(video.getViews());

        return parsed;
    }

    // ==================== VIDEO CORE PARSE ====================

    /**
     * Parse danh sách VideoInsightItem thành VideoInsightsParsed
     * Mỗi item có name, values[0].value có thể là Long, Map<String,Long>, Map<String,Double>
     */
    public VideoInsightsParsed parseInsights(List<VideoInsightItem> items) {
        if (items == null || items.isEmpty()) {
            return VideoInsightsParsed.builder().build();
        }

        VideoInsightsParsed.VideoInsightsParsedBuilder builder = VideoInsightsParsed.builder();

        for (VideoInsightItem item : items) {
            if (item == null || item.getName() == null) continue;

            Object rawValue = extractRawValue(item);

            try {
                switch (item.getName()) {
                    case "post_video_likes_by_reaction_type" -> builder.likesByReactionType(parseReactionMap(rawValue));

                    case "post_video_avg_time_watched" -> builder.avgTimeWatchedMs(parseLong(rawValue));

                    case "post_video_social_actions" -> parseSocialActions(rawValue, builder);

                    case "post_video_view_time" -> builder.totalViewTimeMs(parseLong(rawValue));

                    case "post_impressions_unique" -> builder.uniqueReach(parseLong(rawValue));

                    case "blue_reels_play_count" -> builder.reelsPlayCount(parseLong(rawValue));

                    case "fb_reels_total_plays" -> builder.totalPlays(parseLong(rawValue));

                    case "fb_reels_replay_count" -> builder.replayCount(parseLong(rawValue));

                    case "post_video_retention_graph" -> builder.retentionGraph(parseRetentionGraph(rawValue));

                    case "post_video_followers" -> builder.newFollowers(parseLong(rawValue));

                    default -> log.debug("Unknown insight metric: {}", item.getName());
                }
            } catch (Exception e) {
                log.error("Failed to parse insight [{}] with value [{}]: {}",
                        item.getName(), rawValue, e.getMessage());
            }
        }

        return builder.build();
    }

    // ==================== VIDEO VALUE EXTRACTORS ====================

    /**
     * Lấy value đầu tiên từ values[] của một InsightItem
     * Facebook luôn trả về values là array có 1 phần tử với period=lifetime
     */
    private Object extractRawValue(VideoInsightItem item) {
        if (item.getValues() == null || item.getValues().isEmpty()) {
            return null;
        }
        InsightValue insightValue = item.getValues().getFirst();
        if (insightValue == null) return null;
        return insightValue.getValue();
    }

    /**
     * Parse Long từ Object
     * Facebook trả về số có thể là Integer (Jackson default) hoặc Long
     */
    private Long parseLong(Object value) {
        switch (value) {
            case null -> {
                return null;
            }
            case Long l -> {
                return l;
            }
            case Integer i -> {
                return i.longValue();
            }
            case Double d -> {
                return d.longValue();
            }
            case Number n -> {
                return n.longValue();
            }
            case String s -> {
                try {
                    return Long.parseLong(s);
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse '{}' as Long", s);
                    return null;
                }
            }
            default -> {
            }
        }
        log.warn("Unexpected type for Long: {}", value.getClass().getSimpleName());
        return null;
    }

    /**
     * Parse Map<String, Long> cho reaction types
     * Input: {"REACTION_LIKE": 16, "REACTION_LOVE": 5, ...}
     * Output: Map với key là reaction type, value là count
     */
    @SuppressWarnings("unchecked")
    private Map<String, Long> parseReactionMap(Object value) {
        if (value == null) return Collections.emptyMap();

        // Empty object {} -> không có reaction nào
        if (value instanceof Map<?, ?> rawMap) {
            if (rawMap.isEmpty()) return Collections.emptyMap();

            Map<String, Long> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Long count = null;
                Object v = entry.getValue();
                if (v instanceof Number n) {
                    count = n.longValue();
                } else {
                    try {
                        count = Long.parseLong(String.valueOf(v));
                    } catch (NumberFormatException ex) {
                        log.warn("Cannot parse reaction count for key '{}': {}", key, v);
                    }
                }
                if (count != null) result.put(key, count);
            }
            return result;
        }

        log.warn("Unexpected type for reaction map: {}", value.getClass().getSimpleName());
        return Collections.emptyMap();
    }

    /**
     * Parse social actions: {"SHARE": 1, "COMMENT": 9}
     * Tách thành shares và comments riêng biệt
     */
    private void parseSocialActions(Object value,
                                    VideoInsightsParsed.VideoInsightsParsedBuilder builder) {
        if (value == null) {
            builder.shares(0L).comments(0L);
            return;
        }

        if (value instanceof Map<?, ?> rawMap) {
            if (rawMap.isEmpty()) {
                builder.shares(0L).comments(0L);
                return;
            }

            long shares = 0L;
            long comments = 0L;

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                long count = entry.getValue() instanceof Number n ? n.longValue() : 0L;

                switch (key) {
                    case "SHARE" -> shares = count;
                    case "COMMENT" -> comments = count;
                    default -> log.debug("Unknown social action type: {}", key);
                }
            }

            builder.shares(shares).comments(comments);
            return;
        }

        log.warn("Unexpected type for social actions: {}", value.getClass().getSimpleName());
        builder.shares(0L).comments(0L);
    }

    /**
     * Parse retention graph: {"0": 0.993, "1": 0.993, ..., "28": 0.098}
     * Key là giây (String), value là tỉ lệ giữ chân (0.0 - 1.0)
     * Số key phụ thuộc vào độ dài video nên không cố định
     */
    private Map<String, Double> parseRetentionGraph(Object value) {
        if (value == null) return Collections.emptyMap();

        if (value instanceof Map<?, ?> rawMap) {
            if (rawMap.isEmpty()) return Collections.emptyMap();

            // Sort theo key numeric để dễ đọc
            Map<String, Double> result = new TreeMap<>(
                    Comparator.comparingInt(k -> {
                        try {
                            return Integer.parseInt(k);
                        } catch (NumberFormatException e) {
                            return Integer.MAX_VALUE;
                        }
                    })
            );

            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String second = String.valueOf(entry.getKey());
                Object v = entry.getValue();
                Double ratio = null;

                if (v instanceof Double d) {
                    ratio = d;
                } else if (v instanceof Number n) {
                    ratio = n.doubleValue();
                } else {
                    try {
                        ratio = Double.parseDouble(String.valueOf(v));
                    } catch (NumberFormatException ex) {
                        log.warn("Cannot parse retention value for second '{}': {}", second, v);
                    }
                }

                if (ratio != null) result.put(second, ratio);
            }
            return result;
        }

        log.warn("Unexpected type for retention graph: {}", value.getClass().getSimpleName());
        return Collections.emptyMap();
    }

    // ==================== VIDEO COMPUTED METRICS ====================

    /**
     * Tính tổng reactions từ likesByReactionType
     */
    public long computeTotalReactions(VideoInsightsParsed parsed) {
        if (parsed.getLikesByReactionType() == null) return 0L;
        return parsed.getLikesByReactionType().values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    /**
     * Tính engagement rate = (reactions + comments + shares) / totalPlays * 100
     * Trả về 0.0 nếu totalPlays = 0
     */
    public double computeEngagementRate(VideoInsightsParsed parsed) {
        long totalPlays = Optional.ofNullable(parsed.getTotalPlays()).orElse(0L);
        if (totalPlays == 0L) return 0.0;

        long reactions = computeTotalReactions(parsed);
        long comments = Optional.ofNullable(parsed.getComments()).orElse(0L);
        long shares = Optional.ofNullable(parsed.getShares()).orElse(0L);

        return (double) (reactions + comments + shares) / totalPlays * 100.0;
    }

    /**
     * Tính average watch time (giây) từ avgTimeWatchedMs
     */
    public double computeAvgWatchTimeSeconds(VideoInsightsParsed parsed) {
        Long ms = parsed.getAvgTimeWatchedMs();
        return ms == null ? 0.0 : ms / 1000.0;
    }

    /**
     * Tính watch-through rate = avgWatchTime / videoLength * 100
     * Cho biết người xem trung bình xem được bao nhiêu % video
     */
    public double computeWatchThroughRate(VideoInsightsParsed parsed) {
        Double lengthSeconds = parsed.getLengthSeconds();
        if (lengthSeconds == null || lengthSeconds == 0.0) return 0.0;

        double avgWatchSeconds = computeAvgWatchTimeSeconds(parsed);
        return (avgWatchSeconds / lengthSeconds) * 100.0;
    }

    /**
     * Lấy điểm giữ chân tại giây thứ N (nearest key)
     * Trả về null nếu không có data
     */
    public Double getRetentionAtSecond(VideoInsightsParsed parsed, int second) {
        Map<String, Double> graph = parsed.getRetentionGraph();
        if (graph == null || graph.isEmpty()) return null;
        return graph.get(String.valueOf(second));
    }

    /**
     * Tìm điểm drop-off lớn nhất trong retention graph
     * Trả về second nơi người xem bỏ video nhiều nhất
     */
    public int findBiggestDropOffSecond(VideoInsightsParsed parsed) {
        Map<String, Double> graph = parsed.getRetentionGraph();
        if (graph == null || graph.size() < 2) return -1;

        List<Map.Entry<String, Double>> entries = new ArrayList<>(graph.entrySet());
        int maxDropSecond = -1;
        double maxDrop = Double.MIN_VALUE;

        for (int i = 1; i < entries.size(); i++) {
            double prev = entries.get(i - 1).getValue();
            double curr = entries.get(i).getValue();
            double drop = prev - curr;

            if (drop > maxDrop) {
                maxDrop = drop;
                maxDropSecond = parseSecond(entries.get(i).getKey());
            }
        }

        return maxDropSecond;
    }

    /**
     * Tóm tắt VideoInsightsParsed thành VideoSummary dễ dùng cho report/UI
     */
    public VideoSummary buildSummary(VideoInsightsParsed parsed) {
        return VideoSummary.builder()
                .videoId(parsed.getVideoId())
                .description(truncate(parsed.getDescription()))
                .lengthSeconds(parsed.getLengthSeconds())
                .totalPlays(parsed.getTotalPlays())
                .uniqueReach(parsed.getUniqueReach())
                .replayCount(parsed.getReplayCount())
                .newFollowers(parsed.getNewFollowers())
                .totalReactions(computeTotalReactions(parsed))
                .comments(Optional.ofNullable(parsed.getComments()).orElse(0L))
                .shares(Optional.ofNullable(parsed.getShares()).orElse(0L))
                .engagementRate(roundTo2(computeEngagementRate(parsed)))
                .avgWatchTimeSeconds(roundTo2(computeAvgWatchTimeSeconds(parsed)))
                .watchThroughRate(roundTo2(computeWatchThroughRate(parsed)))
                .biggestDropOffSecond(findBiggestDropOffSecond(parsed))
                .build();
    }

    // ==================== VIDEO AGGREGATION ====================

    /**
     * Tổng hợp metrics của nhiều video (dùng cho dashboard overview)
     */
    public AggregatedInsights aggregate(List<VideoInsightsParsed> parsedList) {
        if (parsedList == null || parsedList.isEmpty()) {
            return AggregatedInsights.builder().build();
        }

        long totalPlays = sumLong(parsedList, VideoInsightsParsed::getTotalPlays);
        long totalReach = sumLong(parsedList, VideoInsightsParsed::getUniqueReach);
        long totalComments = sumLong(parsedList, VideoInsightsParsed::getComments);
        long totalShares = sumLong(parsedList, VideoInsightsParsed::getShares);
        long totalReactions = parsedList.stream()
                .mapToLong(this::computeTotalReactions)
                .sum();
        long totalViewTimeMs = sumLong(parsedList, VideoInsightsParsed::getTotalViewTimeMs);

        double avgEngagement = parsedList.stream()
                .mapToDouble(this::computeEngagementRate)
                .average()
                .orElse(0.0);

        VideoInsightsParsed topVideo = parsedList.stream()
                .max(Comparator.comparingLong(
                        v -> Optional.ofNullable(v.getTotalPlays()).orElse(0L)))
                .orElse(null);

        return AggregatedInsights.builder()
                .videoCount(parsedList.size())
                .totalPlays(totalPlays)
                .totalUniqueReach(totalReach)
                .totalReactions(totalReactions)
                .totalComments(totalComments)
                .totalShares(totalShares)
                .totalViewTimeMs(totalViewTimeMs)
                .avgEngagementRate(roundTo2(avgEngagement))
                .topVideoId(topVideo != null ? topVideo.getVideoId() : null)
                .topVideoPlays(topVideo != null
                        ? Optional.ofNullable(topVideo.getTotalPlays()).orElse(0L)
                        : 0L)
                .build();
    }

    // ==================================================================
    // ========================= POST ENTRY POINT ========================
    // ==================================================================

    /**
     * Parse toàn bộ response Posts từ Facebook Graph API
     * Input: PostsResponse (raw JSON đã deserialize)
     * Output: List<PostInsightsParsed> đã flatten
     */
    public List<PostInsightsParsed> parsePostsResponse(PostsResponse response) {
        if (response == null || response.getData() == null) {
            log.warn("PostsResponse is null or has no data");
            return Collections.emptyList();
        }

        return response.getData().stream()
                .map(this::parsePostWithInsights)
                .collect(Collectors.toList());
    }

    /**
     * Parse một Post object thành PostInsightsParsed đầy đủ
     */
    public PostInsightsParsed parsePostWithInsights(Posts post) {
        if (post == null) {
            log.warn("Post object is null");
            return PostInsightsParsed.builder()
                    .reactionsByType(Collections.emptyMap())
                    .clicksByType(Collections.emptyMap())
                    .mediaTypes(Collections.emptyList())
                    .build();
        }

        List<PostInsightItem> items = Optional.ofNullable(post.getInsights())
                .map(PostInsightsWrapper::getData)
                .orElse(Collections.emptyList());

        PostInsightsParsed parsed = parsePostInsights(items);

        // Gán thêm thông tin cơ bản từ Post
        parsed.setPostId(post.getId());
        parsed.setMessage(post.getMessage());

        Long shareCount = Optional.ofNullable(post.getShares())
                .map(SharesInfo::getCount)
                .orElse(0L);
        parsed.setShareCount(shareCount);

        Long commentCount = Optional.ofNullable(post.getComments())
                .map(CommentsInfo::getSummary)
                .map(CommentsSummary::getTotal_count)
                .orElse(0L);
        parsed.setCommentCount(commentCount);

        List<String> mediaTypes = Optional.ofNullable(post.getAttachments())
                .map(AttachmentsWrapper::getData)
                .orElse(Collections.emptyList())
                .stream()
                .map(AttachmentItem::getMedia_type)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        parsed.setMediaTypes(mediaTypes);

        return parsed;
    }

    // ==================== POST CORE PARSE ====================

    /**
     * Parse danh sách PostInsightItem thành PostInsightsParsed (chỉ phần insights,
     * chưa có postId/message/shareCount/commentCount/mediaTypes)
     * Mỗi item có name, values[0].value là Map<String, Long>
     */
    public PostInsightsParsed parsePostInsights(List<PostInsightItem> items) {
        PostInsightsParsed.PostInsightsParsedBuilder builder = PostInsightsParsed.builder()
                .reactionsByType(Collections.emptyMap())
                .clicksByType(Collections.emptyMap())
                .mediaTypes(Collections.emptyList());

        if (items == null || items.isEmpty()) {
            return builder.build();
        }

        for (PostInsightItem item : items) {
            if (item == null || item.getName() == null) continue;

            Object rawValue = extractRawPostValue(item);

            try {
                switch (item.getName()) {
                    case "post_reactions_by_type_total" -> builder.reactionsByType(parseStringLongMap(rawValue));

                    case "post_clicks_by_type" -> builder.clicksByType(parseStringLongMap(rawValue));

                    default -> log.debug("Unknown post insight metric: {}", item.getName());
                }
            } catch (Exception e) {
                log.error("Failed to parse post insight [{}] with value [{}]: {}",
                        item.getName(), rawValue, e.getMessage());
            }
        }

        return builder.build();
    }

    // ==================== POST VALUE EXTRACTORS ====================

    /**
     * Lấy value đầu tiên từ values[] của một PostInsightItem
     */
    private Object extractRawPostValue(PostInsightItem item) {
        if (item.getValues() == null || item.getValues().isEmpty()) {
            return null;
        }
        InsightValue insightValue = item.getValues().getFirst();
        if (insightValue == null) return null;
        return insightValue.getValue();
    }

    /**
     * Parse Map<String, Long> tổng quát, dùng chung cho reactionsByType và clicksByType
     * Input: {"like": 16, "love": 5} hoặc {"other clicks": 22, "photo view": 195}
     * Input cũng có thể là {} (empty object) khi post không có reaction/click nào
     */
    private Map<String, Long> parseStringLongMap(Object value) {
        if (value == null) return Collections.emptyMap();

        if (value instanceof Map<?, ?> rawMap) {
            if (rawMap.isEmpty()) return Collections.emptyMap();

            Map<String, Long> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                String key = String.valueOf(entry.getKey());
                Long count = null;
                Object v = entry.getValue();
                if (v instanceof Number n) {
                    count = n.longValue();
                } else {
                    try {
                        count = Long.parseLong(String.valueOf(v));
                    } catch (NumberFormatException ex) {
                        log.warn("Cannot parse count for key '{}': {}", key, v);
                    }
                }
                if (count != null) result.put(key, count);
            }
            return result;
        }

        log.warn("Unexpected type for map: {}", value.getClass().getSimpleName());
        return Collections.emptyMap();
    }

    // ==================== POST COMPUTED METRICS ====================

    /**
     * Tính tổng reactions từ reactionsByType
     */
    public long computeTotalPostReactions(PostInsightsParsed parsed) {
        if (parsed.getReactionsByType() == null) return 0L;
        return parsed.getReactionsByType().values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    /**
     * Tính tổng clicks từ clicksByType
     */
    public long computeTotalPostClicks(PostInsightsParsed parsed) {
        if (parsed.getClicksByType() == null) return 0L;
        return parsed.getClicksByType().values().stream()
                .mapToLong(Long::longValue)
                .sum();
    }

    /**
     * Tính tổng tương tác = reactions + comments + shares
     */
    public long computeTotalEngagement(PostInsightsParsed parsed) {
        long reactions = computeTotalPostReactions(parsed);
        long comments = Optional.ofNullable(parsed.getCommentCount()).orElse(0L);
        long shares = Optional.ofNullable(parsed.getShareCount()).orElse(0L);
        return reactions + comments + shares;
    }

    /**
     * Tóm tắt PostInsightsParsed thành PostSummary dễ dùng cho report/UI
     */
    public PostSummary buildPostSummary(PostInsightsParsed parsed) {
        String primaryMediaType = Optional.ofNullable(parsed.getMediaTypes())
                .filter(list -> !list.isEmpty())
                .map(List::getFirst)
                .orElse(null);

        return PostSummary.builder()
                .postId(parsed.getPostId())
                .message(truncate(parsed.getMessage()))
                .primaryMediaType(primaryMediaType)
                .totalReactions(computeTotalPostReactions(parsed))
                .comments(Optional.ofNullable(parsed.getCommentCount()).orElse(0L))
                .shares(Optional.ofNullable(parsed.getShareCount()).orElse(0L))
                .totalClicks(computeTotalPostClicks(parsed))
                .totalEngagement(computeTotalEngagement(parsed))
                .reactionBreakdown(parsed.getReactionsByType())
                .clickBreakdown(parsed.getClicksByType())
                .build();
    }

    // ==================== POST AGGREGATION ====================

    /**
     * Tổng hợp metrics của nhiều post (dùng cho dashboard overview)
     */
    public AggregatedPostInsights aggregatePosts(List<PostInsightsParsed> parsedList) {
        if (parsedList == null || parsedList.isEmpty()) {
            return AggregatedPostInsights.builder().build();
        }

        long totalReactions = parsedList.stream()
                .mapToLong(this::computeTotalPostReactions)
                .sum();
        long totalComments = sumLongPost(parsedList, PostInsightsParsed::getCommentCount);
        long totalShares = sumLongPost(parsedList, PostInsightsParsed::getShareCount);
        long totalClicks = parsedList.stream()
                .mapToLong(this::computeTotalPostClicks)
                .sum();
        long totalEngagement = totalReactions + totalComments + totalShares;

        double avgEngagement = (double) totalEngagement / parsedList.size();

        PostInsightsParsed topPost = parsedList.stream()
                .max(Comparator.comparingLong(this::computeTotalEngagement))
                .orElse(null);

        return AggregatedPostInsights.builder()
                .postCount(parsedList.size())
                .totalReactions(totalReactions)
                .totalComments(totalComments)
                .totalShares(totalShares)
                .totalClicks(totalClicks)
                .totalEngagement(totalEngagement)
                .avgEngagementPerPost(roundTo2(avgEngagement))
                .topPostId(topPost != null ? topPost.getPostId() : null)
                .topPostEngagement(topPost != null ? computeTotalEngagement(topPost) : 0L)
                .build();
    }

    // ==================== HELPERS ====================

    private int parseSecond(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double roundTo2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private String truncate(String text) {
        if (text == null) return null;
        return text.length() <= 100 ? text : text.substring(0, 100) + "...";
    }

    private long sumLong(List<VideoInsightsParsed> list,
                         java.util.function.Function<VideoInsightsParsed, Long> getter) {
        return list.stream()
                .mapToLong(v -> Optional.ofNullable(getter.apply(v)).orElse(0L))
                .sum();
    }

    private long sumLongPost(List<PostInsightsParsed> list,
                             java.util.function.Function<PostInsightsParsed, Long> getter) {
        return list.stream()
                .mapToLong(v -> Optional.ofNullable(getter.apply(v)).orElse(0L))
                .sum();
    }
}