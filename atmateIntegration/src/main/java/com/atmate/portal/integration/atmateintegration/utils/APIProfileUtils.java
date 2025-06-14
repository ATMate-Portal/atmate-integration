package com.atmate.portal.integration.atmateintegration.utils;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class APIProfileUtils {
    private final Environment environment;

    public APIProfileUtils(Environment environment) {
        this.environment = environment;
    }

    public boolean isDev() {
        return environment.acceptsProfiles(Profiles.of("dev"));
    }

}

