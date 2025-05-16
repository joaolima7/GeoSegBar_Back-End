package com.geosegbar.infra.danger_level.web;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.geosegbar.common.response.WebResponseEntity;
import com.geosegbar.entities.DangerLevelEntity;
import com.geosegbar.infra.danger_level.services.DangerLevelService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/danger-levels")
@RequiredArgsConstructor
public class DangerLevelController {

    private final DangerLevelService dangerLevelService;

    @GetMapping
    public ResponseEntity<WebResponseEntity<List<DangerLevelEntity>>> getAllDangerLevels() {
        List<DangerLevelEntity> dangerLevels = dangerLevelService.findAll();
        WebResponseEntity<List<DangerLevelEntity>> response = WebResponseEntity.success(
                dangerLevels, "Danger levels retrieved successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebResponseEntity<DangerLevelEntity>> getDangerLevelById(@PathVariable Long id) {
        DangerLevelEntity dangerLevel = dangerLevelService.findById(id);
        WebResponseEntity<DangerLevelEntity> response = WebResponseEntity.success(
                dangerLevel, "Danger level retrieved successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<WebResponseEntity<DangerLevelEntity>> getDangerLevelByName(@PathVariable String name) {
        DangerLevelEntity dangerLevel = dangerLevelService.findByName(name);
        WebResponseEntity<DangerLevelEntity> response = WebResponseEntity.success(
                dangerLevel, "Danger level retrieved successfully!");
        return ResponseEntity.ok(response);
    }
}
