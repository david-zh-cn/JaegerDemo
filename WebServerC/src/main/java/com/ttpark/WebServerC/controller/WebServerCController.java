package com.ttpark.WebServerC.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ttpark.WebServerC.service.WebServerCService;

import java.util.List;
import java.util.Map;

@RestController
public class WebServerCController {

    @Autowired
    private WebServerCService webServerCService;

    @GetMapping("/queryUser")
    public List<Map<String, Object>> queryUser(@RequestParam(name = "tableName", required = true) String tableName) {
        return webServerCService.queryUser(tableName);
    }
}