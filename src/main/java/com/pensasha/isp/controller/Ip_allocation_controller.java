package com.pensasha.isp.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pensasha.isp.model.Customer;
import com.pensasha.isp.model.Ip_address;
import com.pensasha.isp.model.Status;
import com.pensasha.isp.service.Ip_allocation_service;

import java.util.*;

@RestController
@RequestMapping("/api")
public class Ip_allocation_controller {

    @Autowired
    private Ip_allocation_service ip_allocation_service;

    // Allocate IP Address
    @PostMapping("/allocate")
    public ResponseEntity<Object> allocateIpAddress(@RequestBody Customer customer) {

        ArrayList<Ip_address> allAvailableIp = ip_allocation_service.gettingIpAddressByStatus(Status.AVAILABLE);

        if (allAvailableIp.size() > 0) {
            Ip_address ipAddress = allAvailableIp.get(0);
            ipAddress.setStatus(Status.ALLOCATED);
            customer.setIpAddress(ipAddress);
            ipAddress.setCustomer(customer);

            try {

                ip_allocation_service.addIpAddress(ipAddress);

                return ResponseEntity.status(HttpStatus.CREATED).body(ipAddress);
            } catch (Exception e) {
                return ResponseEntity.badRequest().build();
            }

        } else {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No IPs are available");
        }

    }

    // Release IP Address
    @PutMapping("/release/{ipAddress}")
    public ResponseEntity<Object> releaseIpAddress(@PathVariable String ipAddress) {

        if (ip_allocation_service.doesIpAddressExist(ipAddress)) {

            Ip_address ip = ip_allocation_service.getIpAddress(ipAddress);

            if (ip.getStatus() == Status.ALLOCATED) {

                ip.setStatus(Status.AVAILABLE);
                ip.setCustomer(null);

                ip_allocation_service.addIpAddress(ip);

                return ResponseEntity.ok().build();

            } else {
                return ResponseEntity.notFound().build();
            }

        } else {

            return ResponseEntity.notFound().build();
        }

    }

    // List of allocated IPs
    @GetMapping("/allocated")
    public ResponseEntity<Object> listAllocatedIpAddress() {

        ArrayList<Ip_address> allocatedIpAddresses = ip_allocation_service.gettingIpAddressByStatus(Status.ALLOCATED);

        return ResponseEntity.status(HttpStatus.OK).body(allocatedIpAddresses);

    }

    // List Available IPs
    @GetMapping("/available")
    public ResponseEntity<Object> listAvailableIpAddress() {

        ArrayList<Ip_address> availableIpAddress = ip_allocation_service.gettingIpAddressByStatus(Status.AVAILABLE);

        return ResponseEntity.status(HttpStatus.OK).body(availableIpAddress);

    }

    @GetMapping("/reserved")
    public ResponseEntity<Object> listReservedIpAddresses() {
        ArrayList<Ip_address> reservedIpAddress = ip_allocation_service.gettingIpAddressByStatus(Status.RESERVED);

        return ResponseEntity.status(HttpStatus.OK).body(reservedIpAddress);
    }

    // Filter available IPs providing start and end ip
    @GetMapping("/available/filter")
    public List<String> filterAvailableIp(@RequestParam("start") String start,
            @RequestParam("end") String end) {

        List<String> ipAddresses = new ArrayList<>();
        List<Ip_address> ip_addresses = ip_allocation_service.gettingIpAddressByStatus(Status.AVAILABLE);

        for (Ip_address ip : ip_addresses) {
            ipAddresses.add(ip.getIp());
        }

        sortIps(ipAddresses);

        return filterIps(start, end, ipAddresses, ip_allocation_service.gettingAllIpAddresses());

    }

    // Filter allocated IPs providing start and end ip
    @GetMapping("/allocated/filer")
    public List<String> filterAllocatedIps(@RequestParam("start") String start,
            @RequestParam("end") String end) {

        List<String> ipAddresses = new ArrayList<>();
        List<Ip_address> ip_addresses = ip_allocation_service.gettingIpAddressByStatus(Status.ALLOCATED);
        for (Ip_address ip : ip_addresses) {
            ipAddresses.add(ip.getIp());
        }

        sortIps(ipAddresses);

        return filterIps(start, end, ipAddresses, ip_allocation_service.gettingAllIpAddresses());

    }

    // Filter reserved IPs providing start and end ip
    @GetMapping("/reserved/filer")
    public List<String> filterReservedIps(@RequestParam("start") String start,
            @RequestParam("end") String end) {

        List<String> ipAddresses = new ArrayList<>();
        List<Ip_address> ip_addresses = ip_allocation_service.gettingIpAddressByStatus(Status.AVAILABLE);
        for (Ip_address ip : ip_addresses) {
            ipAddresses.add(ip.getIp());
        }

        sortIps(ipAddresses);

        return filterIps(start, end, ipAddresses, ip_allocation_service.gettingAllIpAddresses());

    }

    public List<String> sortIps(List<String> ipAddresses) {
        Collections.sort(ipAddresses, (a, b) -> {
            int[] aOct = Arrays.stream(a.split("\\.")).mapToInt(Integer::parseInt).toArray();
            int[] bOct = Arrays.stream(b.split("\\.")).mapToInt(Integer::parseInt).toArray();
            int r = 0;
            for (int i = 0; i < aOct.length && i < bOct.length; i++) {
                r = Integer.compare(aOct[i], bOct[i]);
                if (r != 0) {
                    return r;
                }
            }
            return r;
        });

        return ipAddresses;
    }

    public List<String> filterIps(String start, String end, List<String> ipAddresses, List<Ip_address> allIpAddresses) {

        List<String> filteredIpAddresses = new ArrayList<>();

        if (ipAddresses.contains(start) && ipAddresses.contains(end)) {

            int startIndex = ipAddresses.indexOf(start);
            int endIndex = ipAddresses.indexOf(end);

            filteredIpAddresses = filter(startIndex, endIndex, ipAddresses);

        } else {

            List<String> ips = new ArrayList<>();
            for (Ip_address ip : allIpAddresses) {
                ips.add(ip.getIp());
            }

            if (ips.contains(start) && ips.contains(end)) {

                sortIps(ips);

                int startIndex = ips.indexOf(start);
                int endIndex = ips.indexOf(end);

                filteredIpAddresses = filter(startIndex, endIndex, ips);
                for (String ip : ips) {
                    if (!ipAddresses.contains(ip)) {
                        filteredIpAddresses.remove(ip);
                    }
                }

            }

        }

        return filteredIpAddresses;

    }

    public List<String> filter(int startIndex, int endIndex,
            List<String> ipAddresses) {

        List<String> filteredIpAddresses = new ArrayList<>();

        if (startIndex < endIndex) {

            for (int i = startIndex; i <= endIndex; i++) {
                filteredIpAddresses.add(ipAddresses.get(i));
            }

        } else {

            for (int i = endIndex; i <= startIndex; i++) {
                filteredIpAddresses.add(ipAddresses.get(i));
            }

        }

        return filteredIpAddresses;

    }

}
