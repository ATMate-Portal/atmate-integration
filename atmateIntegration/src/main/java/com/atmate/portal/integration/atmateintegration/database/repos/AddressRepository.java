package com.atmate.portal.integration.atmateintegration.database.repos;


import com.atmate.portal.integration.atmateintegration.database.entitites.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {

}
