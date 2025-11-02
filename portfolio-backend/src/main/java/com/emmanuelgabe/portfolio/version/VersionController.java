package com.emmanuelgabe.portfolio.version;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class VersionController {

    @Value("${info.app.version:0.0.1-SNAPSHOT}")
    private String version;

    @Value("${info.app.name:portfolio-backend}")
    private String appName;

    @GetMapping("/version")
    public Map<String, String> getVersion() {
        Map<String, String> versionInfo = new HashMap<>();
        versionInfo.put("version", version);
        versionInfo.put("application", appName);
        return versionInfo;
    }
}
