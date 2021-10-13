package com.apex.ams.config.server.env;

import com.apex.ams.config.client.ConfigEnv;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@ConfigurationProperties("ams.config.server.jdbc")
public class JdbcEnvRepository implements ConfigEnvRepository, Ordered {

    private static final String DEFAULT_SQL = "SELECT NAME, VALUE from properties where APPLICATION=? and PROFILE=? and LABEL=?";
    private int order = Ordered.LOWEST_PRECEDENCE - 10;
    private final JdbcTemplate jdbc;
    private String sql = DEFAULT_SQL;
    private final PropertiesResultSetExtractor extractor = new PropertiesResultSetExtractor();
    private String defaultLabel = "master";
    private String defaultProfile = "default";
    private String defaultApplication = "application";
    private String defaultVersion = "1";
    private String defaultState = null;
    private String versionProperty = "config.version";
    private String stateProperty = "config.state";

    private Map<String, ConfigEnv> envCaches = new ConcurrentHashMap<>();

    public JdbcEnvRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public String getSql() {
        return this.sql;
    }

    @Override
    public ConfigEnv findOne(String application, String profile, String label) {
        if (StringUtils.isEmpty(application)) {
            application = defaultApplication;
        }
        if (StringUtils.isEmpty(label)) {
            label = defaultLabel;
        }
        if (StringUtils.isEmpty(profile)) {
            profile = defaultProfile;
        }
        if (!profile.startsWith(defaultProfile)) {
            profile = "default," + profile;
        }

        String cacheKey = encodeCacheKey(application, profile, label);
        ConfigEnv environment = envCaches.get(cacheKey);
        if (environment != null)
            return environment;

        environment = loadConfigEnv(application, profile, label);
        envCaches.put(cacheKey, environment);
        return environment;
    }

    protected ConfigEnv loadConfigEnv(String application, String profile, String label) {
        ConfigEnv environment;
        String[] profiles = StringUtils.commaDelimitedListToStringArray(profile);
        String version = null;
        String state = null;
        environment = new ConfigEnv(application, profiles, label, defaultVersion,
                defaultState);
        if (!application.startsWith("application")) {
            application = "application," + application;
        }
        List<String> applications = new ArrayList<String>(new LinkedHashSet<>(
                Arrays.asList(StringUtils.commaDelimitedListToStringArray(application))));
        List<String> envs = new ArrayList<String>(new LinkedHashSet<>(Arrays.asList(profiles)));
        Collections.reverse(applications);
        Collections.reverse(envs);
        for (String app : applications) {
            for (String env : envs) {
                Map<String, Object> next = (Map<String, Object>) jdbc.query(this.sql,
                        new Object[]{app, env, label}, this.extractor);
                if (!next.isEmpty()) {
                    version = (String) next.remove(versionProperty);
                    state = (String) next.remove(stateProperty);
                    environment.add(new MapPropertySource(app + "-" + env, next));
                }
            }
        }
        if (version != null)
            environment.setVersion(version);
        if (state != null)
            environment.setState(state);
        return environment;
    }

    private String encodeCacheKey(String application, String profile, String label) {
        return StringUtils.arrayToDelimitedString(new String[]{application, profile, label}, ":");
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }


    public String getDefaultLabel() {
        return defaultLabel;
    }

    public void setDefaultLabel(String defaultLabel) {
        this.defaultLabel = defaultLabel;
    }

    public String getDefaultProfile() {
        return defaultProfile;
    }

    public void setDefaultProfile(String defaultProfile) {
        this.defaultProfile = defaultProfile;
    }

    public String getDefaultApplication() {
        return defaultApplication;
    }

    public void setDefaultApplication(String defaultApplication) {
        this.defaultApplication = defaultApplication;
    }

    public String getDefaultVersion() {
        return defaultVersion;
    }

    public void setDefaultVersion(String defaultVersion) {
        this.defaultVersion = defaultVersion;
    }

    public String getDefaultState() {
        return defaultState;
    }

    public void setDefaultState(String defaultState) {
        this.defaultState = defaultState;
    }

    public String getVersionProperty() {
        return versionProperty;
    }

    public void setVersionProperty(String versionProperty) {
        this.versionProperty = versionProperty;
    }

    public String getStateProperty() {
        return stateProperty;
    }

    public void setStateProperty(String stateProperty) {
        this.stateProperty = stateProperty;
    }

    @Override
    public ConfigEnv refresh(String application, String profile, String label) {

        if (StringUtils.isEmpty(application)) {
            application = defaultApplication;
        }
        if (StringUtils.isEmpty(label)) {
            label = defaultLabel;
        }
        if (StringUtils.isEmpty(profile)) {
            profile = defaultProfile;
        }
        if (!profile.startsWith(defaultProfile)) {
            profile = "default," + profile;
        }


        envCaches.clear(); //--由于缓存key多个，并且重新加载成本不算太高，所以这里简化处理，直接清空所有缓存
        //envCaches.remove(cacheKey);
        ConfigEnv environment = loadConfigEnv(application, profile, label);
        String cacheKey = encodeCacheKey(application, profile, label);
        envCaches.put(cacheKey, environment);
        return environment;
    }
}

class PropertiesResultSetExtractor implements ResultSetExtractor<Map<String, Object>> {

    @Override
    public Map<String, Object> extractData(ResultSet rs)
            throws SQLException, DataAccessException {
        Map<String, Object> map = new LinkedHashMap<>();
        while (rs.next()) {
            map.put(rs.getString(1), rs.getString(2));
        }
        return map;
    }

}