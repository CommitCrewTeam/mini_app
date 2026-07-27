package com.miniecommerce.notification.app.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniecommerce.notification.app.port.inbound.NotifyOrderPlacedUseCase;
import com.miniecommerce.notification.app.port.outbound.SendOrderPlacedNotificationPort;
import org.junit.jupiter.api.Test;

class NotificationServiceWalkingSkeletonTest {

    @Test
    void step2_notifiesOrderPlacedThroughDrivingPortAndUsesDrivenPort() {
        SendOrderPlacedNotificationPort sendNotificationPort = new FakeSendOrderPlacedNotificationAdapter();
        NotifyOrderPlacedUseCase useCase = new NotificationService(sendNotificationPort);

        String result = useCase.notifyOrderPlaced();

        assertThat(result).isEqualTo("ORDER_PLACED_NOTIFICATION_SENT_BY_FAKE_ADAPTER");
    }

    private static class FakeSendOrderPlacedNotificationAdapter implements SendOrderPlacedNotificationPort {

        @Override
        public String send() {
            return "ORDER_PLACED_NOTIFICATION_SENT_BY_FAKE_ADAPTER";
        }
    }
}
