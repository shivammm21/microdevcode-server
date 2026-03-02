package com.microdevcode.code.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "code.execution")
@Data
public class CodeExecutionConfig {
    
    private boolean dockerEnabled = true;
    private int defaultTimeoutSeconds = 30;
    private int maxMemoryMB = 256;
    private String tempDirectory = System.getProperty("java.io.tmpdir") + "/microdevcode/";
    private boolean cleanupEnabled = true;
    private int maxConcurrentExecutions = 10;
    
    // Security settings
    private boolean networkAccessEnabled = false;
    private boolean fileSystemAccessEnabled = false;
    private String[] allowedLanguages = {"java", "python", "javascript", "cpp", "c"};
    
    // Docker settings
    private String dockerRegistry = "docker.io";
    private boolean pullLatestImages = false;
}