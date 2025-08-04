package com.profect.tickle.domain.performance.controller;

import com.profect.tickle.domain.performance.service.KopisPerformanceImporterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "공연", description = "공연 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/kopis")
public class KopisAdminController {

    private final KopisPerformanceImporterService importerService;

    @PostMapping("/import")
    @Operation(summary = "공연 OPNE API 받아오기", description = "kopis 에 있는 공연을 데이터를 받아옵니다.")
    public ResponseEntity<Void> importPerformances() throws Exception {
        importerService.importPerformances();
        return ResponseEntity.ok().build();
    }
}
