package com.miniecommerce.seed;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "seed")
public class SeedTargetsProperties {

    private List<Target> targets = new ArrayList<>();

    public List<Target> getTargets() {
        return targets;
    }

    public void setTargets(List<Target> targets) {
        this.targets = targets;
    }

    public static class Target {

        private String name;
        private String url;
        private String username;
        private String password;
        private String changeLog;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getChangeLog() {
            return changeLog;
        }

        public void setChangeLog(String changeLog) {
            this.changeLog = changeLog;
        }
    }
}
