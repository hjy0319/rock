package com.hujy.rock.lock.config.redisson;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * redisson连接属性
 */
@ConfigurationProperties(prefix = "rock.redisson", ignoreUnknownFields = false)
@Data
public class RedissonConfigProperties {

    private RedissonConfigProperties.Single single;

    private RedissonConfigProperties.Sentinel sentinel;

    private RedissonConfigProperties.Cluster cluster;

    @Data
    public static class Single {
        private boolean enable = false;
        private String address;
    }

    @Data
    public static class Sentinel {
        private boolean enable = false;
        private String masterName;
        private List<String> sentinelAddresses;
    }

    @Data
    public static class Cluster {
        private boolean enable = false;
        private List<String> clusterNodeAddresses;
    }

}
