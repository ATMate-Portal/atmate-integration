package com.atmate.portal.integration.atmateintegration.utils;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProfileUtil {
    private final Environment environment;

    public ProfileUtil(Environment environment) {
        this.environment = environment;
    }

    public boolean isDev() {
        return environment.acceptsProfiles("dev");
    }
}

