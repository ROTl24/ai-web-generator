package com.uloaix.xiaolu_aicode.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStore;
import dev.langchain4j.community.store.memory.chat.redis.StoreType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;

/**
 * redis 的持久化对话记忆
 */
@Configuration
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
@Slf4j
public class RedisChatMemoryStoreConfig {

    private String host;

    private int port;

    /**
     * Redis ACL 用户名（可选）
     */
    private String username;

    private String password;

    private long ttl;

    /**
     * Redis 存储类型：
     * - JSON：依赖 RedisJSON（Redis Stack），会使用 JSON.GET/JSON.SET
     * - STRING：纯 Redis 字符串（GET/SET），不依赖 RedisJSON
     *
     * 默认（不配置时）：自动探测是否支持 RedisJSON，支持则用 JSON，不支持则回退到 STRING。
     */
    private StoreType storeType;

    @Bean
    public RedisChatMemoryStore redisChatMemoryStore() {
        StoreType effectiveStoreType = resolveStoreType();
        return RedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .ttl(ttl)
                .user(StringUtils.hasText(username) ? username : null)
                .password(StringUtils.hasText(password) ? password : null)
                .storeType(effectiveStoreType)
                .build();
    }

    private StoreType resolveStoreType() {
        if (storeType != null) {
            // 如果用户显式配置 JSON，但 Redis 不支持，则自动回退，避免运行期 JSON.GET 异常把 SSE 打断
            if (storeType == StoreType.JSON && !isRedisJsonSupported()) {
                log.warn("检测到当前 Redis 不支持 RedisJSON（JSON.GET 不可用），已自动将 ChatMemoryStore 从 JSON 回退到 STRING。");
                return StoreType.STRING;
            }
            return storeType;
        }
        // 未配置时：优先 JSON，失败回退 STRING
        return isRedisJsonSupported() ? StoreType.JSON : StoreType.STRING;
    }

    /**
     * 探测 RedisJSON 是否可用：对一个不存在的 key 执行 JSON.GET。
     * - 支持 RedisJSON：返回 null（key 不存在）或正常结果
     * - 不支持 RedisJSON：抛 unknown command 'JSON.GET'
     */
    private boolean isRedisJsonSupported() {
        if (!StringUtils.hasText(host) || port <= 0) {
            return false;
        }
        HostAndPort hostAndPort = new HostAndPort(host, port);
        DefaultJedisClientConfig.Builder configBuilder = DefaultJedisClientConfig.builder();
        if (StringUtils.hasText(username)) {
            configBuilder.user(username);
        }
        if (StringUtils.hasText(password)) {
            configBuilder.password(password);
        }

        try (JedisPooled jedis = new JedisPooled(hostAndPort, configBuilder.build())) {
            jedis.jsonGet("__langchain4j_json_support_probe__");
            return true;
        } catch (JedisDataException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toUpperCase().contains("UNKNOWN COMMAND") && msg.toUpperCase().contains("JSON.GET")) {
                return false;
            }
            // 其它错误（权限/协议/网络抖动等）：为了保证服务可用性，回退到 STRING
            log.warn("RedisJSON 探测失败，将回退到 STRING storeType，msg={}", msg);
            return false;
        } catch (Exception e) {
            log.warn("RedisJSON 探测异常，将回退到 STRING storeType", e);
            return false;
        }
    }
}
