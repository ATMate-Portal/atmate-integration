package com.atmate.portal.integration.atmateintegration.database.repos;


import com.atmate.portal.integration.atmateintegration.database.entitites.Address;
import com.atmate.portal.integration.atmateintegration.database.entitites.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {
    boolean existsByStreetAndZipCodeAndClient(String street, String zipCode, Client client);
}
