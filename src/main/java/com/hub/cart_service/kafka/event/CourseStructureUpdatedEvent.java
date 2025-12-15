package com.hub.cart_service.kafka.event;

import com.hub.cart_service.model.enumeration.CourseChangeType;

import java.time.Instant;

public record CourseStructureUpdatedEvent(
        Long courseId,
        CourseChangeType changeType, // CREATED, UPDATED, DELETED
        int totalModules,
        int totalLessons,
        Instant updatedAt
) {
}
