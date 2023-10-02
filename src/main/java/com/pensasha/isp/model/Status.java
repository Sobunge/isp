package com.pensasha.isp.model;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum Status {
    
    AVAILABLE("Available"),
    ALLOCATED("Allocated"),
    RESERVED("RESERVED");

    public String status;

}
