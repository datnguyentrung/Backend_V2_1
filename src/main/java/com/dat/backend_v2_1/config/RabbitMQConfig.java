package com.dat.backend_v2_1.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.ConditionalRejectingErrorHandler;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ErrorHandler;


@Configuration
public class RabbitMQConfig {
    // === CÁC HẰNG SỐ CHUNG ===
    public static final String EXCHANGE_NAME = "taekwondo_exchange"; // Đổi tên chung chung

    // === HẰNG SỐ CHO ĐIỂM DANH ===
    public static final String ATTENDANCE_QUEUE = "attendance_queue";
    public static final String ATTENDANCE_ROUTING_KEY = "attendance.key";

    // === HẰNG SỐ CHO ĐÁNH GIÁ ===
    public static final String EVALUATION_QUEUE = "evaluation_queue";
    public static final String EVALUATION_ROUTING_KEY = "evaluation.key";

    // === TRẠM TRUNG CHUYỂN (DÙNG CHUNG) ===
    @Bean
    TopicExchange exchange() {
        // 1 Exchange này sẽ xử lý TẤT CẢ các loại tin nhắn
        return new TopicExchange(EXCHANGE_NAME);
    }

    // === CẤU HÌNH CHO HÀNG ĐỢI ĐIỂM DANH ===
    @Bean
    Queue attendanceQueue() { // Đổi tên hàm thành "attendanceQueue"
        return new Queue(ATTENDANCE_QUEUE, true);
    }

    @Bean
    Binding attendanceBinding(Queue attendanceQueue, TopicExchange exchange) {
        return BindingBuilder.bind(attendanceQueue).to(exchange).with(ATTENDANCE_ROUTING_KEY);
    }

    // === CẤU HÌNH CHO HÀNG ĐỢI ĐÁNH GIÁ ===
    @Bean
    Queue evaluationQueue() {
        return new Queue(EVALUATION_QUEUE, true);
    }

    @Bean
    Binding evaluationBinding(Queue evaluationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(evaluationQueue).to(exchange).with(EVALUATION_ROUTING_KEY);
    }

    /**
     * Định nghĩa một MessageConverter sử dụng JSON (Jackson2)
     * Bean này sẽ được Spring Boot tự động sử dụng cho RabbitTemplate
     * để chuyển đổi DTO của bạn (ví dụ: StudentMarkAttendance) sang JSON.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * (Tùy chọn nhưng được khuyến nghị)
     * Cấu hình RabbitTemplate để CHẮC CHẮN sử dụng MessageConverter JSON ở trên.
     * Spring Boot thường tự động làm điều này nếu phát hiện Bean MessageConverter,
     * nhưng cấu hình rõ ràng sẽ đảm bảo nó hoạt động.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter()); // Sử dụng JSON converter
        return rabbitTemplate;
    }

    /**
     * Custom RabbitMQ Error Handler that ensures errors are properly thrown
     * and not swallowed, preventing undefined behavior
     */
    @Bean
    public ErrorHandler customRabbitErrorHandler() {
        return new ConditionalRejectingErrorHandler(new CustomFatalExceptionStrategy());
    }

    /**
     * Custom RabbitMQ Listener Container Factory with proper error handling
     */
    @Bean
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setErrorHandler(customRabbitErrorHandler());

        // Additional configuration to ensure errors are not ignored
        factory.setDefaultRequeueRejected(false); // Don't requeue on error
        factory.setMissingQueuesFatal(true); // Fail if queue is missing

        return factory;
    }

    /**
     * Custom Fatal Exception Strategy that treats all exceptions as fatal
     * to ensure they are properly thrown and not ignored
     */
    public static class CustomFatalExceptionStrategy extends ConditionalRejectingErrorHandler.DefaultExceptionStrategy {

        @Override
        public boolean isFatal(Throwable t) {
            // Log the error for debugging
            System.err.println("RabbitMQ Error Handler - Fatal error detected: " + t.getMessage());
            t.printStackTrace();

            // Treat all exceptions as fatal to ensure they are thrown
            // This prevents the consumer from continuing execution after errors
            return true;
        }
    }
}
