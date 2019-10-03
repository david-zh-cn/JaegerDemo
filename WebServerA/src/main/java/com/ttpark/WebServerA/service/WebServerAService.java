package com.ttpark.WebServerA.service;

import org.apache.dubbo.config.annotation.Reference;
import org.springframework.stereotype.Service;
import com.ttpark.DubboCommon.DubboCommonServer;

import java.util.List;
import java.util.Map;

@Service
public class WebServerAService {

    @Reference
    private DubboCommonServer dubboCommonServer;

    public List<Map<String, Object>> queryUser(){
        return dubboCommonServer.queryUser();
    }
}
