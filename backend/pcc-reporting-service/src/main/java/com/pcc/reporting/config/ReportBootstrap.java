package com.pcc.reporting.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pcc.reporting.entity.ReportDefinition;
import com.pcc.reporting.report.ReportRegistry;
import com.pcc.reporting.repository.ReportDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Keeps report_definitions in sync with the code registry. */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportBootstrap implements ApplicationRunner {

    private final ReportDefinitionRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        for (var def : ReportRegistry.DEFS) {
            var existing = repo.findByReportKey(def.key());
            ReportDefinition d = existing.orElseGet(ReportDefinition::new);
            d.setReportKey(def.key());
            d.setName(def.name());
            d.setDomain(def.domain());
            d.setSourceService(def.service());
            d.setSourcePath(def.path());
            d.setPermission(def.permission());
            d.setColumnsJson(mapper.writeValueAsString(def.columns().stream()
                    .map(c -> java.util.Map.of("key", c.key(), "label", c.label(), "type", c.type()))
                    .toList()));
            d.setExportable(def.exportable());
            d.setActive(true);
            repo.save(d);
        }
        log.info("Report definitions synced: {}", ReportRegistry.DEFS.size());
    }
}
