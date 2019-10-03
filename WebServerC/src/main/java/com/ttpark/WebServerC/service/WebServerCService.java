package com.ttpark.WebServerC.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class WebServerCService {

    @Autowired
    JdbcTemplate jdbcTemplate;

    public List<Map<String, Object>> queryUser(String tableName) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM " + tableName);
        return list;
    }
}
