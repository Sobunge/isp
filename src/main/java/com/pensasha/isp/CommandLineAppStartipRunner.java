package com.pensasha.isp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.pensasha.isp.model.Ip_address;
import com.pensasha.isp.model.Status;
import com.pensasha.isp.service.Ip_allocation_service;

@Component
public class CommandLineAppStartipRunner implements CommandLineRunner {

    @Autowired
    private Ip_allocation_service ip_allocation_service;

    public void run(String... args) throws Exception {

        String ipPart = "192.168.1.";

        for (int i = 1; i < 255; i++) {

            if (!ip_allocation_service.doesIpAddressExist(ipPart + i)) {
                ip_allocation_service.addIpAddress(new Ip_address("192.168.1." + i, Status.AVAILABLE));
            }

        }

    }

}
