package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.Address;
import com.atmate.portal.integration.atmateintegration.database.repos.AddressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    @Autowired
    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    // Criar um novo endereço
    public Address createAddress(Address address) {
        return addressRepository.save(address);
    }

    // Ler todos os endereços
    public List<Address> getAllAddresses() {
        return addressRepository.findAll();
    }

    // Ler um endereço por ID
    public Optional<Address> getAddressById(Integer id) {
        return addressRepository.findById(id);
    }

    // Atualizar um endereço
    public Address updateAddress(Integer id, Address addressDetails) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Endereço não encontrado com ID: " + id));

        address = addressDetails;

        return addressRepository.save(address);
    }

    // Deletar um endereço
    public void deleteAddress(Integer id) {
        if (!addressRepository.existsById(id)) {
            throw new RuntimeException("Endereço não encontrado com ID: " + id);
        }
        addressRepository.deleteById(id);
    }
}

