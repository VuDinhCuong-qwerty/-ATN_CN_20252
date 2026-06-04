package com.iam.jobScheduled.connect;

import java.util.List;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import com.iam.jobScheduled.connect.output.Province;
import com.iam.jobScheduled.connect.output.Ward;

@HttpExchange
public interface RegionClient {
    
    @GetExchange("/p/")
    List<Province> getAllProvinces(
        @RequestParam(name = "depth", defaultValue = "1") int depth
    );

    @GetExchange("/p/{code}")
    List<Province> getPrvinces(
        @RequestParam(name = "depth", defaultValue = "1") int depth,
        @PathVariable(name = "code") Long code
    );

    @GetExchange("/w")
    List<Ward> getWardsByProvinceCode(
        @RequestParam(name = "province", defaultValue = "") Long provinceCode
    );
    
}
