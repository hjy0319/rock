package com.hujy.rock.lock.config.redisson;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.ClusterServersConfig;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

/**
 * redissonClient初始化配置
 */
@Configuration
@EnableConfigurationProperties(RedissonConfigProperties.class)
@ConditionalOnClass({Redisson.class})
public class RedissonConfig {

    @Resource
    private RedissonConfigProperties redissonConfigProperties;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 单节点模式
        RedissonConfigProperties.Single single = redissonConfigProperties.getSingle();
        if (single != null && single.isEnable()) {
            config.useSingleServer()
                    .setAddress(getCompleteAddress(single.getAddress()));
            return Redisson.create(config);
        }

        // 哨兵模式
        RedissonConfigProperties.Sentinel sentinel = redissonConfigProperties.getSentinel();
        if (sentinel != null && sentinel.isEnable()) {
            SentinelServersConfig sentinelServersConfig = config.useSentinelServers()
                    .setMasterName(sentinel.getMasterName());
            for (String a : sentinel.getSentinelAddresses()) {
                sentinelServersConfig.addSentinelAddress(getCompleteAddress(a));
            }

            return Redisson.create(config);
        }
        // 集群模式
        RedissonConfigProperties.Cluster cluster = redissonConfigProperties.getCluster();
        if (cluster != null && cluster.isEnable()) {
            ClusterServersConfig clusterServersConfig = config.useClusterServers();
            for (String a : cluster.getClusterNodeAddresses()) {
                clusterServersConfig.addNodeAddress(getCompleteAddress(a));
            }
            return Redisson.create(config);
        }

        return null;

    }

    private static String getCompleteAddress(String address) {
        return address.startsWith("redis://") ? address : "redis://" + address;
    }

}
