package com.example.learn.controller;

import com.example.learn.service.CacheService2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/data")
public class DataController {

    @Autowired
    private CacheService2 cacheService;

    @GetMapping("/{key}")
    public ResponseEntity<String> getData(@PathVariable String key) {
        String value = cacheService.getData(key);
        return value != null ? ResponseEntity.ok(value) : ResponseEntity.notFound().build();
    }

    // 使用了延时双删除策略
    @PostMapping("/update")
    public ResponseEntity<String> updateData(@RequestParam String key, @RequestParam String newValue) {
        cacheService.updateData(key, newValue);
        return ResponseEntity.ok("Data updated successfully");
    }
}