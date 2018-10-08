//package org.dong.redis.mock;
//
///**
// * @Package Name : ${PACKAG_NAME}
// * @Author : 1766318593@qq.com
// * @Creation Date : 2018年09月24日下午6:44
// * @Function : todo
// */
//
//
//
//import LockServiceImpl;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.log4j.spi.LoggerFactory;
//
//import java.util.Objects;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.locks.ReentrantLock;
//import java.util.logging.Logger;
//
///**
// * Created by BingZhong on 2017/7/29.
// *
// * 基于Redis实现的分布式锁
// */
//public final class RedisLockHelper {
//
//    private static Log logger = LogFactory.getLog(RedisLockHelper.class);
//
//    /**
//     * redis操作帮助类,可以是其他封装了redis操作的类
//     */
//    private RedisHelper redisHelper;
//
//    public static final long DEFAULT_TIMEOUT = 30 * 1000;
//
//    public static final long DEFAULT_SLEEP_TIME = 100;
//
//    private RedisLockHelper(RedisHelper redisHelper) {
//        this.redisHelper = redisHelper;
//    }
//
//    public static RedisLockHelper getInstance(RedisHelper redisHelper) {
//        return new RedisLockHelper(redisHelper);
//    }
//
//    /**
//     * 创建锁
//     *
//     * @param mutex     互斥量
//     * @param timeout   锁的超时时间
//     * @param sleepTime 线程自旋尝试获取锁时的休眠时间
//     * @param timeUnit  时间单位
//     */
//    public RedisLock newLock(String mutex, long timeout, long sleepTime, TimeUnit timeUnit) {
//        logger.info("创建分布式锁,互斥量为{}", mutex);
//        return new RedisLock(mutex, timeout, sleepTime, timeUnit);
//    }
//
//    public RedisLock newLock(String mutex, long timeout, TimeUnit timeUnit) {
//        return newLock(mutex, timeout, DEFAULT_SLEEP_TIME, timeUnit);
//    }
//
//    public RedisLock newLock(String mutex) {
//        return newLock(mutex, DEFAULT_TIMEOUT, TimeUnit.MILLISECONDS);
//    }
//
//    public class RedisLock {
//        /**
//         * 用于创建redis健值对的键，相当于互斥量
//         */
//        private final String mutex;
//
//        /**
//         * 锁过期的绝对时间
//         */
//        private volatile long lockExpiresTime = 0;
//
//        /**
//         * 锁的超时时间
//         */
//        private final long timeout;
//
//        /**
//         * 每次循环获取锁的休眠时间
//         */
//        private final long sleepTime;
//
//        /**
//         * 锁的线程持有者
//         */
//        private volatile Thread lockHolder = null;
//
//        private final ReentrantLock threadLock = new ReentrantLock();
//
//        public RedisLock(String mutex, long timeout, long sleepTime, TimeUnit timeUnit) {
//            this.mutex = mutex;
//            this.timeout = timeUnit.toMillis(timeout);
//            this.sleepTime = timeUnit.toMillis(sleepTime);
//        }
//
//        /**
//         * 加锁,将会一直尝试获取锁,直到超时
//         */
//        public boolean lock(long acquireTimeout, TimeUnit timeUnit) throws InterruptedException {
//            acquireTimeout = timeUnit.toMillis(acquireTimeout);
//            long acquireTime = acquireTimeout + System.currentTimeMillis();
//            threadLock.tryLock(acquireTimeout, timeUnit);
//            try {
//                while (true) {
//                    boolean hasLock = tryLock();
//                    if (hasLock) {
//                        //获取锁成功
//                        return true;
//                    } else if (acquireTime < System.currentTimeMillis()) {
//                        break;
//                    }
//                    Thread.sleep(sleepTime);
//                }
//            } finally {
//                if (threadLock.isHeldByCurrentThread()) {
//                    threadLock.unlock();
//                }
//            }
//
//            return false;
//        }
//
//        /**
//         * 尝试获取锁,无论是否获取到锁都将直接返回而不会阻塞
//         * 不支持重入锁
//         */
//        public boolean tryLock() {
//            if (lockHolder == Thread.currentThread()) {
//                throw new IllegalMonitorStateException("不支持重入锁");
//            }
//            long currentTime = System.currentTimeMillis();
//            String expires = String.valueOf(timeout + currentTime);
//            //尝试设置互斥量
//            if (redisHelper.setNx(mutex, expires) > 0) {
//                setLockStatus(expires);
//                return true;
//            } else {
//                String currentLockTime = redisHelper.get(mutex);
//                //检查锁是否超时
//                if (Objects.nonNull(currentLockTime) && Long.parseLong(currentLockTime) < currentTime) {
//                    //获取旧的锁时间并设置互斥量
//                    String oldLockTime = redisHelper.getSet(mutex, expires);
//                    //判断获取到的旧值是否一致,不一致证明已经有另外的进程(线程)成功获取到了锁
//                    if (Objects.nonNull(oldLockTime) && Objects.equals(oldLockTime, currentLockTime)) {
//                        setLockStatus(expires);
//                        return true;
//                    }
//                }
//
//                return false;
//            }
//        }
//
//        /**
//         * 该锁是否被锁住
//         */
//        public boolean isLock() {
//            String currentLockTime = redisHelper.get(mutex);
//            //存在互斥量且锁还为过时即锁住
//            return Objects.nonNull(currentLockTime) && Long.parseLong(currentLockTime) > System.currentTimeMillis();
//        }
//
//        public String getMutex() {
//            return mutex;
//        }
//
//        /**
//         * 解锁
//         */
//        public boolean unlock() {
//            //只有锁的持有线程才能解锁
//            if (lockHolder == Thread.currentThread()) {
//                //判断锁是否超时，没有超时才将互斥量删除
//                if (lockExpiresTime > System.currentTimeMillis()) {
//                    redisHelper.del(mutex);
//                    logger.info("删除互斥量[{}]", mutex);
//                }
//                lockHolder = null;
//                logger.info("释放[{}]锁成功", mutex);
//
//                return true;
//            } else {
//                throw new IllegalMonitorStateException("没有获取到锁的线程无法执行解锁操作");
//            }
//        }
//
//        private void setLockStatus(String expires) {
//            lockExpiresTime = Long.parseLong(expires);
//            lockHolder = Thread.currentThread();
//            logger.info("获取[{}]锁成功", mutex);
//        }
//    }
//}
//
//
//
