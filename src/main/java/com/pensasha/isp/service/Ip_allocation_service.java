package com.pensasha.isp.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pensasha.isp.model.Ip_address;
import com.pensasha.isp.model.Status;
import com.pensasha.isp.repository.Ip_allocation_repository;

import java.util.*;

@Service
public class Ip_allocation_service {

    @Autowired
    private Ip_allocation_repository ip_allocation_repository;

    // Adding an ip address to the database
    public Ip_address addIpAddress(Ip_address ipAddress) {
        return ip_allocation_repository.save(ipAddress);
    }

    // Does an ip address exist
    public Boolean doesIpAddressExist(String ipAddress) {
        return ip_allocation_repository.existsById(ipAddress);
    }

    // Getting ip address by status
    public ArrayList<Ip_address> gettingIpAddressByStatus(Status status) {
        return ip_allocation_repository.findAllByStatus(status);
    }

    // Get an ip address
    public Ip_address getIpAddress(String ipAddress) {
        return ip_allocation_repository.findById(ipAddress).get();
    }

    // Get all ip addresses
    public List<Ip_address> gettingAllIpAddresses(){
        return ip_allocation_repository.findAll();
    }
}
