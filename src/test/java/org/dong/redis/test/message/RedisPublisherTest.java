package org.dong.redis.test.message;

import org.dong.redis.RedisConnection;
import org.dong.redis.impl.RedisMessagePublisherImpl;
import org.dong.redis.test.RedisConnectionUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @Package Name : ${PACKAG_NAME}
 * @Author : 1766318593@qq.com
 * @Creation Date : 2018年10月07日下午1:23
 * @Function : todo
 */
public class RedisPublisherTest {

    private RedisMessagePublisherImpl messagePublisherRedis;

    @Before
    public void before() {
        RedisConnection redisConnection = RedisConnectionUtil.create();
        messagePublisherRedis = new RedisMessagePublisherImpl();
        messagePublisherRedis.setRedisConnection(redisConnection);
        messagePublisherRedis.setChannels(new String[]{"channel1", "channel2"});
    }

    @Test
    public void publisherTest() {
        for (int i = 0; i < 3; i++) {
            Assert.assertTrue(messagePublisherRedis.sendMessage("message" + i));
        }
    }
    public static void main(String[] args) {

    }

}
