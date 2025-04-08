package com.atmate.portal.integration.atmateintegration.utils;

import com.atmate.portal.integration.atmateintegration.database.ClientDataDTO;
import com.atmate.portal.integration.atmateintegration.database.entitites.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ClientDataUtils {
    public static String formatGender(String gender){
        if(gender.equals("MASCULINO"))
            return "M";
        if(gender.equals("FEMININO"))
            return "F";
        return "i";
    }

    public static LocalDate parseData(String data) {
        return LocalDate.parse(data, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    public static Address buildAddress(Client client, ClientDataDTO data){
        Address address = new Address();
        address.setClient(client);
        address.setStreet(data.getMorada());
       //TODO  door_number - pensar no door number
        String zipcode = data.getCodigo_postal().replace(" - ", "-").substring(0, 8);
        address.setZipCode(zipcode);
        address.setCity(data.getConcelho());
        address.setDistrict(data.getDistrito());
        address.setParish(data.getFreguesia());
        address.setCounty(data.getDistrito());
        address.setCountry(data.getPais_residencia());
        address.setAddressType(new AddressType(1, null, null, null)); //TODO pensar
        return address;
    }

    public static Contact buildContactPhone(Client client, String number){
        Contact contact = new Contact();
        contact.setContact(number);
        contact.setClient(client);
        contact.setContactType(new ContactType(2, null, null, null));
        contact.setIsDefaultContact(true);
        contact.setDescription("Telefone pessoal"); //TODO pensar no que meter

        return contact;
    }

    public static Contact buildContactEmail(Client client, String email){
        Contact contact = new Contact();
        contact.setContact(email);
        contact.setClient(client);
        contact.setContactType(new ContactType(1, null, null, null));
        contact.setIsDefaultContact(true);
        contact.setDescription("Email pessoal"); //TODO pensar no que meter
        return contact;
    }
}
