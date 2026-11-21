package com.pcc.document.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pcc.storage")
public class StorageProperties {
    private String root;
    private long maxFileSize;
    private List<String> allowedMime;
}
