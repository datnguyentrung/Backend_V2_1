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
    // MÃ£ nÃ y pháº£i khá»›p chÃ­nh xÃ¡c vá»›i Ã´ "XÃ¡c minh mÃ£" trÃªn giao diá»‡n Facebook
    private static final String VERIFY_TOKEN = "dat";

    private final FacebookService facebookService;

    private final RestTemplate restTemplate;

    @Value("${facebook.app-secret}")
    private String pageAccessToken;

    @Value("${facebook.app-id}")
    private String pageId;

    /**
     * 1. API XÃ¡c minh (GET): Facebook sáº½ gá»i API nÃ y ÄÃšNG 1 Láº¦N khi báº¡n báº¥m nÃºt "XÃ¡c minh vÃ  lÆ°u"
     */
    /**
     * Tác dụng: Thực hiện logic verifyWebhook của lớp hiện tại.
     * Input: Nhận String mode, String token, String challenge từ caller hoặc request.
     * Output: Trả về ResponseEntity<String> theo kết quả xử lý.
     */
    @GetMapping("/webhook")
    public ResponseEntity<String> verifyWebhook(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {

        if ("subscribe".equals(mode) && VERIFY_TOKEN.equals(token)) {
            log.info("âœ… Facebook xÃ¡c minh Webhook thÃ nh cÃ´ng!");
            // Báº¯t buá»™c pháº£i tráº£ vá» chÃ­nh cÃ¡i chuá»—i challenge mÃ  Facebook gá»­i sang
            return ResponseEntity.ok(challenge);
        } else {
            log.error("âŒ XÃ¡c minh Webhook tháº¥t báº¡i! Sai token.");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Lá»—i xÃ¡c thá»±c");
        }
    }

    /**
     * Tác dụng: Thực hiện logic getVideoInsights của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về ResponseEntity<List<FacebookDTO.VideoInsightsParsed>> theo kết quả xử lý.
     */
    @GetMapping("/video-insights")
    public ResponseEntity<List<FacebookDTO.VideoInsightsParsed>> getVideoInsights() {
        String apiVideoUrl = "https://graph.facebook.com/v25.0/" + pageId
                + "/videos?fields=description,length,views,video_insights&access_token="
                + pageAccessToken;

        try {
            // 2. Thá»±c hiá»‡n HTTP Call tá»›i Facebook
            FacebookDTO.VideosResponse response = restTemplate.getForObject(apiVideoUrl, FacebookDTO.VideosResponse.class);

            // 3. Parse dá»¯ liá»‡u
            List<FacebookDTO.VideoInsightsParsed> parsedData = facebookService.parseVideosResponse(response);

            return ResponseEntity.ok(parsedData);

        } catch (Exception e) {
            log.error("Lá»—i khi call Facebook Graph API: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Tác dụng: Thực hiện logic getPostInsights của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về ResponseEntity<List<FacebookDTO.PostInsightsParsed>> theo kết quả xử lý.
     */
    @GetMapping("/post-insights")
    public ResponseEntity<List<FacebookDTO.PostInsightsParsed>> getPostInsights() {
        // 1. Khai bÃ¡o URL dáº¡ng Template (Ä‘áº·t cÃ¡c biáº¿n vÃ o trong {})
        String apiPostsUrl = "https://graph.facebook.com/v25.0/{pageId}/posts?fields={fields}&access_token={accessToken}";

        // 2. Giá»¯ nguyÃªn gá»‘c chuá»—i fields cÃ³ chá»©a dáº¥u {} cá»§a Facebook, khÃ´ng cáº§n Ä‘á»•i thÃ nh %7B
        String fields = "message,shares,comments.summary(total_count),insights.metric(post_reactions_by_type_total,post_clicks_by_type),attachments{media_type}";

        try {
            // 3. Truyá»n trá»±c tiáº¿p cÃ¡c tham sá»‘ vÃ o getForObject.
            // Spring Boot sáº½ tá»± Ä‘á»™ng tháº¿ cÃ¡c biáº¿n nÃ y vÃ o URL vÃ  mÃ£ hÃ³a (encode) cá»±c ká»³ chuáº©n xÃ¡c
            FacebookDTO.PostsResponse response = restTemplate.getForObject(
                    apiPostsUrl,
                    FacebookDTO.PostsResponse.class,
                    pageId,          // Tháº¿ vÃ o {pageId}
                    fields,          // Tháº¿ vÃ o {fields}
                    pageAccessToken  // Tháº¿ vÃ o {accessToken}
            );

            // 4. Parse dá»¯ liá»‡u
            List<FacebookDTO.PostInsightsParsed> parsedData = facebookService.parsePostsResponse(response);
            return ResponseEntity.ok(parsedData);

        } catch (Exception e) {
            log.error("Lá»—i khi call Facebook Graph API: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 2. API Nháº­n dá»¯ liá»‡u (POST): Facebook sáº½ gá»i API nÃ y LIÃŠN Tá»¤C má»—i khi cÃ³ ai Like/Comment trÃªn Fanpage
     */
    /**
     * Tác dụng: Thực hiện logic receiveWebhook của lớp hiện tại.
     * Input: Nhận String payload từ caller hoặc request.
     * Output: Trả về ResponseEntity<String> theo kết quả xử lý.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> receiveWebhook(@RequestBody String payload) {
        log.info("ðŸ”¥ Facebook Webhook Event Nháº­n ÄÆ°á»£c:\n{}", payload);

        // TODO: Äáº¡t cÃ³ thá»ƒ bÃ³c tÃ¡ch cá»¥c JSON (payload) á»Ÿ Ä‘Ã¢y, hoáº·c nÃ©m nÃ³ vÃ o RabbitMQ

        // Báº®T BUá»˜C: LuÃ´n tráº£ vá» 200 OK ngay láº­p tá»©c Ä‘á»ƒ Facebook biáº¿t server báº¡n Ä‘Ã£ nháº­n Ä‘Æ°á»£c tin,
        // náº¿u khÃ´ng Facebook sáº½ cho lÃ  server sáº­p vÃ  spam gá»­i láº¡i liÃªn tá»¥c.
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}


