package com.rongan.security_system.controller;

import com.rongan.security_system.entity.EmergencyPlan;
import com.rongan.security_system.service.EmergencyPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应急预案控制器 - 提供预案的增删改查接口
 */
@RestController
@RequestMapping("/api/plan")
public class EmergencyPlanController {

    @Autowired
    private EmergencyPlanService planService;

    /**
     * 查询所有应急预案
     */
    @GetMapping("/list")
    public List<EmergencyPlan> list() {
        return planService.getAllPlans();
    }

    /**
     * 保存应急预案
     */
    @PostMapping("/save")
    public ResponseEntity<?> save(@RequestBody EmergencyPlan plan,
                                  @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        return ResponseEntity.ok(planService.savePlan(plan));
    }

    /**
     * 删除应急预案
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                    @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"admin".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("无权限");
        }
        planService.deletePlan(id);
        return ResponseEntity.ok().build();
    }
}
