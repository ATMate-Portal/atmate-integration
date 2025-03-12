package com.atmate.portal.integration.atmateintegration.database.services;

import com.atmate.portal.integration.atmateintegration.database.entitites.AddressType;
import com.atmate.portal.integration.atmateintegration.database.repos.AddressTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AddressTypeService {

    private final AddressTypeRepository addressTypeRepository;

    @Autowired
    public AddressTypeService(AddressTypeRepository addressTypeRepository) {
        this.addressTypeRepository = addressTypeRepository;
    }

    // Criar um novo tipo de endereço
    public AddressType createAddressType(AddressType addressType) {
        return addressTypeRepository.save(addressType);
    }

    // Ler todos os tipos de endereço
    public List<AddressType> getAllAddressTypes() {
        return addressTypeRepository.findAll();
    }

    // Ler um tipo de endereço por ID
    public Optional<AddressType> getAddressTypeById(Integer id) {
        return addressTypeRepository.findById(id);
    }

    // Atualizar um tipo de endereço
    public AddressType updateAddressType(Integer id, AddressType addressTypeDetails) {
        AddressType addressType = addressTypeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tipo de endereço não encontrado com ID: " + id));

        // Atualizar os campos do tipo de endereço
        addressType.setDescription(addressTypeDetails.getDescription());

        return addressTypeRepository.save(addressType);
    }

    // Deletar um tipo de endereço
    public void deleteAddressType(Integer id) {
        if (!addressTypeRepository.existsById(id)) {
            throw new RuntimeException("Tipo de endereço não encontrado com ID: " + id);
        }
        addressTypeRepository.deleteById(id);
    }
}
