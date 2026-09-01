package com.miniecommerce.inventory.app.port.inbound;

import com.miniecommerce.inventory.domain.Phone;
import reactor.core.publisher.Flux;

import java.util.List;

public interface GetItemsForOrderPreviewUseCase {
    Flux<Phone> getItemsForOrderPreview(List<Long> ids);
}