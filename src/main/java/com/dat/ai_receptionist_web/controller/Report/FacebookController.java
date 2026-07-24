package com.dat.ai_receptionist_web.controller.Report;

import com.dat.ai_receptionist_web.dto.Report.FacebookDTO;
import com.dat.ai_receptionist_web.service.Report.FacebookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/facebook")
@RequiredArgsConstructor
public class FacebookController {
    // Mã này phải khớp chính xác với ô "Xác minh mã" trên giao diện Facebook
    private static final String VERIFY_TOKEN = "dat";

    private final FacebookService facebookService;

    private final RestTemplate restTemplate;

    @Value("${facebook.app-secret}")
    private String pageAccessToken;

    @Value("${facebook.app-id}")
    private String pageId;

    /**
     * 1. API Xác minh (GET): Facebook sẽ gọi API này ĐÚNG 1 LẦN khi bạn bấm nút "Xác minh và lưu"
     */
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            log.info("✅ Facebook xác minh Webhook thành công!");
            // Bắt buộc phải trả về chính cái chuỗi challenge mà Facebook gửi sang
            return ResponseEntity.ok(challenge);
        } else {
            log.error("❌ Xác minh Webhook thất bại! Sai token.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lỗi xác thực");
        }
    }

    @GetMapping("/video-insights")
    public ResponseEntity<List<FacebookDTO.VideoInsightsParsed>> getVideoInsights() {
        String apiVideoUrl = "https://graph.facebook.com/v25.0/" + pageId
                + "/videos?fields=description,length,views,video_insights&access_token="
                + pageAccessToken;

        try {
            // 2. Thực hiện HTTP Call tới Facebook
            FacebookDTO.VideosResponse response = restTemplate.getForObject(apiVideoUrl, FacebookDTO.VideosResponse.class);

            // 3. Parse dữ liệu
            List<FacebookDTO.VideoInsightsParsed> parsedData = facebookService.parseVideosResponse(response);

            return ResponseEntity.ok(parsedData);

        } catch (Exception e) {
            log.error("Lỗi khi call Facebook Graph API: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/post-insights")
    public ResponseEntity<List<FacebookDTO.PostInsightsParsed>> getPostInsights() {
        // 1. Khai báo URL dạng Template (đặt các biến vào trong {})
        String apiPostsUrl = "https://graph.facebook.com/v25.0/{pageId}/posts?fields={fields}&access_token={accessToken}";

        // 2. Giữ nguyên gốc chuỗi fields có chứa dấu {} của Facebook, không cần đổi thành %7B
        String fields = "message,shares,comments.summary(total_count),insights.metric(post_reactions_by_type_total,post_clicks_by_type),attachments{media_type}";

        try {
            // 3. Truyền trực tiếp các tham số vào getForObject.
            // Spring Boot sẽ tự động thế các biến này vào URL và mã hóa (encode) cực kỳ chuẩn xác
            FacebookDTO.PostsResponse response = restTemplate.getForObject(
                    apiPostsUrl,
                    FacebookDTO.PostsResponse.class,
                    pageId,          // Thế vào {pageId}
                    fields,          // Thế vào {fields}
                    pageAccessToken  // Thế vào {accessToken}
            );

            // 4. Parse dữ liệu
            List<FacebookDTO.PostInsightsParsed> parsedData = facebookService.parsePostsResponse(response);
            return ResponseEntity.ok(parsedData);

        } catch (Exception e) {
            log.error("Lỗi khi call Facebook Graph API: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 2. API Nhận dữ liệu (POST): Facebook sẽ gọi API này LIÊN TỤC mỗi khi có ai Like/Comment trên Fanpage
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(@RequestBody String payload) {
        log.info("🔥 Facebook Webhook Event Nhận Được:\n{}", payload);

        // TODO: Đạt có thể bóc tách cục JSON (payload) ở đây, hoặc ném nó vào RabbitMQ

        // BẮT BUỘC: Luôn trả về 200 OK ngay lập tức để Facebook biết server bạn đã nhận được tin,
        // nếu không Facebook sẽ cho là server sập và spam gửi lại liên tục.
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
