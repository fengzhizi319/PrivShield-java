package com.privshield.agent.rest;

import com.privshield.agent.service.PrivacyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 隐私预算查询 REST 端点 — 对应 Python routers/budget.py。
 */
@RestController
public class BudgetController {

    private final PrivacyService service;

    public BudgetController(PrivacyService service) {
        this.service = service;
    }

    @GetMapping("/v1/privacy/budget")
    public ResponseEntity<Map<String, Double>> budget() {
        return ResponseEntity.ok(service.budgetRemaining());
    }
}
