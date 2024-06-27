package com.example.learn.controller;

import com.example.learn.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CacheController {

    @Autowired
    private CacheService cacheService;

    @GetMapping("/get")
    public String getData(@RequestParam String key) {
        return cacheService.getData(key);
    }

    // 双写策略 同步缓存
    @PostMapping("/update")
    public ResponseEntity<String> updateData(@RequestParam String key, @RequestParam String newValue) {
        cacheService.updateData(key, newValue);
        return ResponseEntity.ok("Data updated successfully");
    }
}