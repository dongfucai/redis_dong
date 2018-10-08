package org.dong.redis.impl;

import org.dong.redis.RedisConnection;
import org.dong.redis.RedisMessageConsumer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 * @Package Name : ${PACKAG_NAME}
 * @Author : 1766318593@qq.com
 * @Creation Date : 2018年10月07日下午12:47
 * @Function : todo
 */
public abstract class RedisMessageConsumerImpl implements RedisMessageConsumer {


    /**
     * 构造函数
     * @param redisConnection redis 连接类
     * @param channels 订阅的频道列表
     */
    public RedisMessageConsumerImpl(RedisConnection redisConnection, String[] channels) {
        Jedis jedis = null;
        try {
            if (channels != null && channels.length > 0) {
                jedis = redisConnection.getJedis();
                jedis.subscribe(new JedisPubSub() {
                    @Override
                    public void onMessage(String channel, String message) {
                        System.out.println("receive " + message + " from " + channel);
                        handleMessage(message);
                    }
                }, channels);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }

    public static void main(String[] args) {

    }

}
