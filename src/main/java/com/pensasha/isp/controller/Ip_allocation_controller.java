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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /*
     * an IP subnet calculator tool, which takes an IP and a subnet mask and returns
     * details like the network address, broadcast address, and usable IP range.
     */
    @GetMapping("/subnetCalc")
    public ResponseEntity<Object> ipSubnetCalculator(@RequestParam("ipAddress") String ipAddress,
            @RequestParam("subnetMask") String subnetMask) {

        if (isValidIPAddress(ipAddress)) {

            String subnetClass = getSubnetClass(subnetMask);

            if (subnetClass != null) {

                try {
                    InetAddress ip = InetAddress.getByName(ipAddress);
                    InetAddress mask = InetAddress.getByName(subnetMask);

                    InetAddress networkAddress = calculateNetworkAddress(ip, mask);
                    InetAddress broadcastAddress = calculateBroadcastAddress(ip, mask);
                    String usableIPRange = calculateUsableIPRange(networkAddress, broadcastAddress);

                    return ResponseEntity.status(HttpStatus.OK).body("IP Address: " + ip.getHostAddress() +
                            "\nSubnet Mask: " + mask.getHostAddress() + "\nNetwork Address: "
                            + networkAddress.getHostAddress() +
                            "\nBroadcast Address: " + broadcastAddress.getHostAddress() + "\nUsable IP Range: "
                            + usableIPRange);

                } catch (UnknownHostException e) {
                    e.printStackTrace();

                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occured.");
                }

            } else {

                return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("The subnet mask provided is invalid.");
            }

        } else {

            return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body("The ip address provided is invalid.");
        }

    }

    // Sorting ip addresses from smallest to largest
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

    // Filter ip addresses from start ip address to the end ip address
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

    // Filter ip addresses using indexes of start and end ip provided
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

    // Method to check if an ip address is valid
    public static boolean isValidIPAddress(String ipAddress) {
        String ipRegex = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        Pattern pattern = Pattern.compile(ipRegex);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }

    // Method to calculate network address
    public static InetAddress calculateNetworkAddress(InetAddress ip, InetAddress mask) {
        byte[] ipBytes = ip.getAddress();
        byte[] maskBytes = mask.getAddress();
        byte[] networkBytes = new byte[ipBytes.length];

        for (int i = 0; i < ipBytes.length; i++) {
            networkBytes[i] = (byte) (ipBytes[i] & maskBytes[i]);
        }

        try {
            return InetAddress.getByAddress(networkBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Method to calculate broadcast address
    public static InetAddress calculateBroadcastAddress(InetAddress ip, InetAddress mask) {
        byte[] ipBytes = ip.getAddress();
        byte[] maskBytes = mask.getAddress();
        byte[] broadcastBytes = new byte[ipBytes.length];

        for (int i = 0; i < ipBytes.length; i++) {
            broadcastBytes[i] = (byte) (ipBytes[i] | ~maskBytes[i]);
        }

        try {
            return InetAddress.getByAddress(broadcastBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Method to calculate usable ip address range
    public static String calculateUsableIPRange(InetAddress networkAddress, InetAddress broadcastAddress) {
        byte[] startBytes = networkAddress.getAddress();
        byte[] endBytes = broadcastAddress.getAddress();

        startBytes[startBytes.length - 1] += 1; // Increment the last byte for the first usable IP
        endBytes[endBytes.length - 1] -= 1; // Decrement the last byte for the last usable IP

        try {
            InetAddress startIP = InetAddress.getByAddress(startBytes);
            InetAddress endIP = InetAddress.getByAddress(endBytes);
            return startIP.getHostAddress() + " - " + endIP.getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        return null;
    }

    // Method to get the subnet class either class A, B or C
    public static String getSubnetClass(String subnetMask) {
        
        // Regular expressions for valid subnet masks of Class A, B, and C
        String classARegex = "^(255\\.0\\.0\\.0)$";
        String classBRegex = "^(255\\.255\\.0\\.0)$";
        String classCRegex = "^(255\\.255\\.255\\.0)$";

        Pattern classAPattern = Pattern.compile(classARegex);
        Pattern classBPattern = Pattern.compile(classBRegex);
        Pattern classCPattern = Pattern.compile(classCRegex);

        Matcher classAMatcher = classAPattern.matcher(subnetMask);
        Matcher classBMatcher = classBPattern.matcher(subnetMask);
        Matcher classCMatcher = classCPattern.matcher(subnetMask);

        if (classAMatcher.matches()) {
            return "A";
        } else if (classBMatcher.matches()) {
            return "B";
        } else if (classCMatcher.matches()) {
            return "C";
        }

        return null;
    }

}
