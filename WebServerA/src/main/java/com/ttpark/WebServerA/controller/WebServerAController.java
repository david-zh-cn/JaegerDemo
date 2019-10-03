package com.ttpark.WebServerA.controller;

import com.ttpark.WebServerA.redisUtil.RedisUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ttpark.WebServerA.service.WebServerAService;

import java.util.List;
import java.util.Map;

@RestController
public class WebServerAController {

    @Autowired
    private WebServerAService webServerAService;

    @Autowired
    RedisUtils redisUtils;

    @GetMapping("/queryUser")
    public List<Map<String, Object>> queryUser() {
        // 在redis中设置mysql查询语句
        redisUtils.set("tableName", "users");
        return webServerAService.queryUser();
    }
}