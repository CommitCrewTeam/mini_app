package com.miniecommerce.notification.adapter.inbound.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import com.miniecommerce.notification.app.port.inbound.NotifyOrderPlacedUseCase;
import org.junit.jupiter.api.Test;

class OrderPlacedListenerTest {

    @Test
    void step3_notifiesOrderPlacedThroughKafkaDrivingAdapter() {
        FakeNotifyOrderPlacedUseCase useCase = new FakeNotifyOrderPlacedUseCase();
        OrderPlacedListener listener = new OrderPlacedListener(useCase);

        listener.onOrderPlaced("order-placed-payload");

        assertThat(useCase.called).isTrue();
    }

    private static class FakeNotifyOrderPlacedUseCase implements NotifyOrderPlacedUseCase {

        private boolean called;

        @Override
        public String notifyOrderPlaced() {
            called = true;
            return "ORDER_PLACED_NOTIFIED_THROUGH_FAKE_USE_CASE";
        }
    }
}
