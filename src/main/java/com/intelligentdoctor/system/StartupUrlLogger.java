package com.intelligentdoctor.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class StartupUrlLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupUrlLogger.class);

    private final Environment environment;

    public StartupUrlLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void printUrls() {
        String port = environment.getProperty("server.port", "8080");
        log.info("Intelligent Doctor 患者端: http://localhost:{}/", port);
        log.info("Intelligent Doctor 管理后台: http://localhost:{}/admin.html", port);
        log.info("Intelligent Doctor 系统状态: http://localhost:{}/api/system/profile", port);
    }
}
