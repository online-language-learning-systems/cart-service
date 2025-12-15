package com.hub.cart_service.kafka.processor;

import com.hub.cart_service.model.enumeration.CourseChangeType;
import com.hub.cart_service.kafka.event.CourseStructureUpdatedEvent;
import com.hub.cart_service.service.CartItemService;
import com.hub.cart_service.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
@Slf4j
public class CartConsumer {

    private final CartItemService cartItemService;

    @Bean
    public Consumer<CourseStructureUpdatedEvent> handleCourseStructureUpdatedEvent() {
        return event -> {
            log.info("📥 Received CourseStructureUpdatedEvent: {}", event);

            if (event.changeType() == CourseChangeType.DELETED) {
                cartItemService.findAllByCourseId(event.courseId())
                        .forEach(cartItem -> cartItemService.removeCourseFromCart(cartItem.getCourseId()));
                log.info("Removed courseId {} from all carts", event.courseId());
            }

            // TODO: xử lý UPDATE hoặc CREATED nếu muốn
        };
    }
}
