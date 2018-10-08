package org.dong.redis.test.message;

import org.dong.redis.RedisConnection;
import org.dong.redis.impl.RedisMessageConsumerImpl;
import org.dong.redis.test.RedisConnectionUtil;
import org.junit.Test;

/**
 * @Package Name : ${PACKAG_NAME}
 * @Author : 1766318593@qq.com
 * @Creation Date : 2018年10月07日下午1:25
 * @Function : todo
 */
public class RedisConsumerTest {

    @Test
    public void consumerTest() {
        RedisConnection redisConnection = RedisConnectionUtil.create();
        new RedisMessageConsumerImpl(redisConnection, new String[]{"channel1", "channel2"}) {
            @Override
            public void handleMessage(String message) {
                System.out.println(message);
            }
        };
    }
    public static void main(String[] args) {

    }

}
