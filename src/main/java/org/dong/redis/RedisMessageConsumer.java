package org.dong.redis;

/**
 * @Package Name : ${PACKAG_NAME}
 * @Author : 1766318593@qq.com
 * @Creation Date : 2018年10月07日下午12:37
 * @Function : todo
 */
public interface RedisMessageConsumer {
    void handleMessage(String message);
}
