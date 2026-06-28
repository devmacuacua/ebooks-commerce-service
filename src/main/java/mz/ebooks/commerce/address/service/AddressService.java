package mz.ebooks.commerce.address.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mz.ebooks.commerce.address.dto.AddressDto;
import mz.ebooks.commerce.address.dto.CreateAddressRequest;
import mz.ebooks.commerce.address.entity.Address;
import mz.ebooks.commerce.address.repository.AddressRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;

    public List<AddressDto> getUserAddresses(String userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public AddressDto addAddress(String userId, CreateAddressRequest req) {
        if (req.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = Address.builder()
                .userId(userId)
                .name(req.getName())
                .street(req.getStreet())
                .number(req.getNumber())
                .complement(req.getComplement())
                .district(req.getDistrict())
                .city(req.getCity())
                .province(req.getProvince())
                .country(req.getCountry() != null ? req.getCountry() : "Moçambique")
                .postalCode(req.getPostalCode())
                .isDefault(req.isDefault())
                .build();

        return toDto(addressRepository.save(address));
    }

    @Transactional
    public AddressDto updateAddress(UUID id, String userId, CreateAddressRequest req) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));

        if (req.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        address.setName(req.getName());
        address.setStreet(req.getStreet());
        address.setNumber(req.getNumber());
        address.setComplement(req.getComplement());
        address.setDistrict(req.getDistrict());
        address.setCity(req.getCity());
        address.setProvince(req.getProvince());
        if (req.getCountry() != null) address.setCountry(req.getCountry());
        address.setPostalCode(req.getPostalCode());
        address.setDefault(req.isDefault());

        return toDto(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(UUID id, String userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        addressRepository.delete(address);
    }

    @Transactional
    public AddressDto setDefault(UUID id, String userId) {
        Address address = addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        addressRepository.clearDefaultForUser(userId);
        address.setDefault(true);
        return toDto(addressRepository.save(address));
    }

    private AddressDto toDto(Address a) {
        return AddressDto.builder()
                .id(a.getId())
                .userId(a.getUserId())
                .name(a.getName())
                .street(a.getStreet())
                .number(a.getNumber())
                .complement(a.getComplement())
                .district(a.getDistrict())
                .city(a.getCity())
                .province(a.getProvince())
                .country(a.getCountry())
                .postalCode(a.getPostalCode())
                .isDefault(a.isDefault())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
