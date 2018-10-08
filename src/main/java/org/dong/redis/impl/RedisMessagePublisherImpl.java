package org.dong.redis.impl;

import org.dong.redis.RedisConnection;
import org.dong.redis.RedisMessagePublisher;
import redis.clients.jedis.Jedis;

/**
 * @Package Name : ${PACKAG_NAME}
 * @Author : 1766318593@qq.com
 * @Creation Date : 2018年10月07日下午12:25
 * @Function : todo
 */
public class RedisMessagePublisherImpl implements RedisMessagePublisher {

    private RedisConnection redisConnection;

    /**
     * 可以同时向多个频道发送数据
     */
    private String[] channels;


    public void setRedisConnection(RedisConnection redisConnection) {
        this.redisConnection = redisConnection;
    }

    public void setChannels(String[] channels) {
        this.channels = channels;
    }

    @Override
    public boolean sendMessage(String message){
        Jedis jedis = null;
        jedis = redisConnection.getJedis();
        try {
            if (jedis != null && channels != null && channels.length > 0 && message != null){
                for (String channel : channels) {
                    jedis.publish(channel, message);
                }
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
        return false;
    }

    public static void main(String[] args) {

    }

}
