package com.dat.backend_v2_1.config;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.RabbitListenerErrorHandler;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

/**
 * Global error handler for RabbitMQ message processing
 * Ensures that errors are properly handled and don't cause undefined behavior
 */
@Component("rabbitMQErrorHandler")
@Slf4j
public class RabbitMQErrorHandler implements RabbitListenerErrorHandler {

    @Override
    public Object handleError(Message amqpMessage, Channel channel,
                            org.springframework.messaging.Message<?> message,
                            ListenerExecutionFailedException exception) throws Exception {

        log.error("RabbitMQ message processing failed. Queue: {}, Exchange: {}, Routing Key: {}",
                amqpMessage.getMessageProperties().getConsumerQueue(),
                amqpMessage.getMessageProperties().getReceivedExchange(),
                amqpMessage.getMessageProperties().getReceivedRoutingKey());

        log.error("Message content: {}", new String(amqpMessage.getBody()));
        log.error("Error details: ", exception);

        // For message conversion errors or validation errors, reject and don't requeue
        if (isNonRetryableError(exception)) {
            log.warn("Non-retryable error detected. Message will be rejected and not requeued.");
            throw new AmqpRejectAndDontRequeueException("Non-retryable error: " + exception.getMessage(), exception);
        }

        // For other errors, also reject to prevent infinite loops
        log.error("Processing error detected. Message will be rejected to prevent infinite retries.");
        throw new AmqpRejectAndDontRequeueException("Processing failed: " + exception.getMessage(), exception);
    }

    /**
     * Determines if an error is non-retryable (e.g., validation errors, conversion errors)
     */
    private boolean isNonRetryableError(Exception exception) {
        // Check for common non-retryable exceptions
        String errorMessage = exception.getMessage().toLowerCase();

        return errorMessage.contains("conversion") ||
               errorMessage.contains("invalid") ||
               errorMessage.contains("validation") ||
               errorMessage.contains("illegal") ||
               errorMessage.contains("cannot convert") ||
               exception instanceof IllegalArgumentException;
    }
}
