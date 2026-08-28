package com.miniecommerce.inventory.adapter.inbound.rest;

import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.inventory.adapter.inbound.rest.dto.PhoneRequest;
import com.miniecommerce.inventory.adapter.inbound.rest.mapper.PhoneRestMapper;
import com.miniecommerce.inventory.app.port.inbound.CreatePhoneUseCase;
import com.miniecommerce.inventory.app.port.inbound.GetAllPhonesUseCase;
import com.miniecommerce.inventory.domain.Phone;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/phones")
public class PhoneController {

    private final GetAllPhonesUseCase getAllPhonesUseCase;
    private final CreatePhoneUseCase createPhoneUseCase;
    private final PhoneRestMapper phoneRestMapper;

    public PhoneController(GetAllPhonesUseCase getAllPhonesUseCase,
                           CreatePhoneUseCase createPhoneUseCase,
                           PhoneRestMapper phoneRestMapper) {
        this.getAllPhonesUseCase = getAllPhonesUseCase;
        this.createPhoneUseCase = createPhoneUseCase;
        this.phoneRestMapper = phoneRestMapper;
    }

    @GetMapping
    public Mono<ApiResponse<List<Phone>>> getAllPhones() {
        return getAllPhonesUseCase.getAllPhones()
                .collectList()
                .map(ApiResponse::success);
    }

    @PostMapping
    public Mono<ApiResponse<Phone>> createPhone(@RequestBody PhoneRequest request) {
        Phone phone = phoneRestMapper.toPhone(request);
        return createPhoneUseCase.createPhone(phone)
                .map(created -> ApiResponse.success(created, "Phone created"));
    }
}
