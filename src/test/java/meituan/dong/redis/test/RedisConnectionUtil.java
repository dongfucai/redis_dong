package meituan.dong.redis.test;

import meituan.dong.redis.RedisConnection;
import redis.clients.jedis.JedisPoolConfig;

import java.util.Date;

/**
 * @Package Name : ${PACKAG_NAME}
 * @Author : 1766318593@meituan.com
 * @Creation Date : 2018年09月24日下午2:09
 * @Function : todo
 */
public class RedisConnectionUtil {

    public static RedisConnection create() {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(50);
        jedisPoolConfig.setMaxIdle(10);
        jedisPoolConfig.setMinIdle(1);
        RedisConnection redisConnection = new RedisConnection();
        redisConnection.setIp("127.0.0.1");
        redisConnection.setPort(6379);
        redisConnection.setPwd("hhSbcpotThgWdnxJNhrzwstSP20DvYOldkjf");
        redisConnection.setClientName(Thread.currentThread().getName());
        redisConnection.setTimeOut(600);
        redisConnection.setJedisPoolConfig(jedisPoolConfig);
        return redisConnection;
    }

    public static void main(String []args) throws InterruptedException{
        System.out.println("curruent ms:"+System.currentTimeMillis());
        Thread.sleep(1000);
        System.out.println(new Date().getTime());

    }
}
