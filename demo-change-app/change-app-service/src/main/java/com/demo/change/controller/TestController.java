package com.demo.change.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;



@RestController
public class TestController {
    
    @GetMapping("/test")
    @PreAuthorize("hasAuthority('change-mgmt/change-request:view')")
    public String getMethodName() {
        return "Success";
    }


    
}
