package com.miniecommerce.order.adapter.outbound.stub;

import com.miniecommerce.order.app.port.outbound.SaveOrderPort;
import org.springframework.stereotype.Component;

@Component
public class StubSaveOrderAdapter implements SaveOrderPort {

    @Override
    public String save() {
        return "ORDER_SAVED_BY_STUB_ADAPTER";
    }
}
