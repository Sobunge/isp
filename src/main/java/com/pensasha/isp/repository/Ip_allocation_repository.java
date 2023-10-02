package com.pensasha.isp.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pensasha.isp.model.Ip_address;
import com.pensasha.isp.model.Status;

import java.util.ArrayList;

public interface Ip_allocation_repository extends JpaRepository<Ip_address, String> {
    
    public ArrayList<Ip_address> findAllByStatus(Status status);

}
