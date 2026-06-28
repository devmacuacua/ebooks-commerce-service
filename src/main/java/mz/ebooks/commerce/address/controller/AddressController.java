package mz.ebooks.commerce.address.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import mz.ebooks.commerce.address.dto.AddressDto;
import mz.ebooks.commerce.address.dto.CreateAddressRequest;
import mz.ebooks.commerce.address.service.AddressService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/commerce/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDto>> getUserAddresses(
            @RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(addressService.getUserAddresses(userId));
    }

    @PostMapping
    public ResponseEntity<AddressDto> addAddress(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody CreateAddressRequest req) {
        return ResponseEntity.ok(addressService.addAddress(userId, req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AddressDto> updateAddress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id,
            @Valid @RequestBody CreateAddressRequest req) {
        return ResponseEntity.ok(addressService.updateAddress(id, userId, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        addressService.deleteAddress(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<AddressDto> setDefault(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable UUID id) {
        return ResponseEntity.ok(addressService.setDefault(id, userId));
    }
}
