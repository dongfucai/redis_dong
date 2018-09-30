package meituan.dong.redis;

/**
 * @Package Name : ${PACKAG_NAME}
 * @Author : 1766318593@qq.com
 * @Creation Date : 2018年09月24日下午12:46
 * @Function : todo
 */
public interface LockService {

    /**
     * 通过一个key 立刻获取锁
     *
     * @param key 获取锁的 key
     * @return 如果获取成功返回锁定值，否则返回 null
     */
    String lock(String key);

    /**
     * 通过一个key 再超时时间内获取锁，如果不能马上获取锁，则等待获取，直到时间超时
     *
     * @param key 获取锁的 key
     * @return 如果获取成功返回锁定值，否则返回 null
     */
    String tryLock(String key);

    /**
     * 解锁一个分布式锁
     *
     * @param key   分布式锁 key
     * @param value 获取分布式式锁的时候获取到的值，只有用获取锁时，获取到的锁定值才能解锁。
     * @return 是否解锁成功
     */
    boolean unLock(String key, String value);

}
