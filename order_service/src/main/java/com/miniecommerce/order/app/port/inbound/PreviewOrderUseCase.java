package com.miniecommerce.order.app.port.inbound;

import com.miniecommerce.order.app.command.PreviewOrderCommand;
import com.miniecommerce.order.domain.OrderPreview;

public interface PreviewOrderUseCase {

    OrderPreview preview(PreviewOrderCommand command);
}