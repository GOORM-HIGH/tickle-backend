package com.profect.tickle.domain.performance.elk;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.profect.tickle.domain.performance.entity.Performance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PerformanceIndexService {
    private final ElasticsearchClient elasticsearchClient;

    public void index(Performance p) throws IOException {
        Map<String,Object> doc = new HashMap<>();
        doc.put("performance_id", p.getId());
        doc.put("performance_title", p.getTitle());
        doc.put("performance_date", p.getDate());
        doc.put("status_id", p.getStatus().getId());
        doc.put("updated_at", p.getUpdatedAt());
        doc.put("performance_img", p.getImg());
        if (p.getDeletedAt() != null) { // 삭제면 ES에도 삭제시각 필드 세팅
            doc.put("performance_deleted_at", p.getDeletedAt());
        }
        elasticsearchClient.index(i -> i.index("performances_v1").id(String.valueOf(p.getId())).document(doc));
    }

    public void markDeleted(Long id, Instant deletedAt) throws IOException {
        elasticsearchClient.update(u -> u.index("performances_v1").id(String.valueOf(id))
                .doc(Map.of("performance_deleted_at", deletedAt)), Map.class);
    }
}

