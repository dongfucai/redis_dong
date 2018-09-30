package meituan.dong.redis.test;

import meituan.dong.redis.RedisConnection;
import meituan.dong.redis.impl.LockServiceImpl;

/**
 * @Package Name : ${PACKAG_NAME}
 * @Author : 1766318593@qq.com
 * @Creation Date : 2018年09月24日下午2:08
 * @Function : todo
 */
public class RedisLockTest {

    public static void main(String[] args) {
        for (int i = 0; i < 9; i++) {
            new Thread(new Runnable() {
                public void run() {
                    RedisConnection redisConnection = RedisConnectionUtil.create();
                    LockServiceImpl lockServiceRedis = new LockServiceImpl();
                    lockServiceRedis.setRedisConnection(redisConnection);
                    lockServiceRedis.setDbIndex(15);

                    lockServiceRedis.setLockExpirseTime(20);

                    lockServiceRedis.setTryExpirseTime(20);
                    String key = "20171228";
                   // String value = lockServiceRedis.lock(key);
                    String value = lockServiceRedis.tryLock(key);
                    try {
                        if (value != null) {
                            System.out.println(Thread.currentThread().getName() + " lock key = " + key + " success! ");
                            Thread.sleep(2 * 1000);
                        } else {
                            System.out.println(Thread.currentThread().getName() + " lock key = " + key + " failure! ");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        if (value == null) {
                            value = "";
                        }
                        System.out.println(Thread.currentThread().getName() + " unlock key = " + key + " " + lockServiceRedis.unLock(key, value));

                    }
                }
            }).start();
        }
    }


}
