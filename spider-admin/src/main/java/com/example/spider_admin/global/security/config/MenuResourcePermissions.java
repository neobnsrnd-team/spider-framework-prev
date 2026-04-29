package com.example.spider_admin.global.security.config;

import com.example.spider_admin.global.security.constant.MenuAccessLevel;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "menu-resource")
@Getter
@Setter
public class MenuResourcePermissions {

    private Map<String, Map<String, String>> permissions = Collections.emptyMap();

    public Set<String> getDerivedResourceAuthorities(String menuId, MenuAccessLevel level) {
        Map<String, String> entry = permissions.getOrDefault(menuId, Collections.emptyMap());
        Set<String> authorities = new LinkedHashSet<>();
        authorities.addAll(split(entry.getOrDefault("R", "")));
        if (level == MenuAccessLevel.WRITE) {
            authorities.addAll(split(entry.getOrDefault("W", "")));
        }
        return authorities;
    }

    private Set<String> split(String value) {
        if (value == null || value.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
