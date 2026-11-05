package com.miniecommerce.inventory.adapter.inbound.rest;

import com.miniecommerce.common.exception.AppException;
import com.miniecommerce.common.exception.ErrorCode;
import com.miniecommerce.common.response.ApiResponse;
import com.miniecommerce.inventory.adapter.inbound.rest.dto.PhoneRequest;
import com.miniecommerce.inventory.adapter.inbound.rest.mapper.PhoneRestMapper;
import com.miniecommerce.inventory.app.port.inbound.phone.PhoneUseCase;
import com.miniecommerce.inventory.domain.Phone;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/phones")
public class PhoneController {

    private final PhoneUseCase phoneUseCase;
    private final PhoneRestMapper phoneRestMapper;

    public PhoneController(PhoneUseCase phoneUseCase,
                           PhoneRestMapper phoneRestMapper) {
        this.phoneUseCase = phoneUseCase;
        this.phoneRestMapper = phoneRestMapper;
    }

    @GetMapping
    public Mono<ApiResponse<List<Phone>>> getAllPhones() {
        return phoneUseCase.getAllPhones()
                .collectList()
                .map(ApiResponse::success);
    }

    @GetMapping("/by-ids")
    public Mono<ApiResponse<List<Phone>>> getItemsForOrderPreview(@RequestParam("ids") List<Long> ids) {
        return phoneUseCase.getItemsForOrderPreview(ids)
                .collectList()
                .map(ApiResponse::success);
    }

    @GetMapping("/{id}")
    public Mono<ApiResponse<Integer>> getStock(@PathVariable Long id) {
        return phoneUseCase.getStock(id)
                .map(ApiResponse::success)
                .switchIfEmpty(Mono.error(new AppException(
                        ErrorCode.NOT_FOUND,
                        "Phone not found")));
    }

    @PostMapping
    public Mono<ApiResponse<Phone>> createPhone(@RequestBody PhoneRequest request) {
        Phone phone = phoneRestMapper.toPhone(request);
        return phoneUseCase.createPhone(phone)
                .map(created -> ApiResponse.success(created, "Phone created"));
    }
}