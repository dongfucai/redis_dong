package meituan.dong.redis.impl;

import meituan.dong.redis.LockService;
import meituan.dong.redis.RedisConnection;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import redis.clients.jedis.Jedis;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

/**
 * @Package Name : ${PACKAG_NAME}
 * @Author : 1766318593@qq.com
 * @Creation Date : 2018年09月24日下午12:47
 * @Function : todo
 */

/**
 * 首先，为了确保分布式锁可用，我们至少要确保锁的实现同时满足以下四个条件：
 *
 * 互斥性。在任意时刻，只有一个客户端能持有锁。
 * 不会发生死锁。即使有一个客户端在持有锁的期间崩溃而没有主动解锁，也能保证后续其他客户端能加锁。
 * 具有容错性。只要大部分的Redis节点正常运行，客户端就可以加锁和解锁。
 * 解铃还须系铃人。加锁和解锁必须是同一个客户端，客户端自己不能把别人加的锁给解了。
 */
public class LockServiceImpl implements LockService {


    private static Log log = LogFactory.getLog(LockServiceImpl.class);

    private static String SET_SUCCESS = "OK";

    private static String KEY_PRE = "REDIS_LOCK_";

    private DateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    private RedisConnection redisConnection;

    private Integer dbIndex;

    /**
     * 锁的失效时间(s)
     */
    private Integer lockExpirseTime;

    /**
     * 指定一个线程的加锁的超时时间，超过了多久，就不等待加锁了 (s)
     */
    private Integer tryExpirseTime;

    public void setRedisConnection(RedisConnection redisConnection) {
        this.redisConnection = redisConnection;
    }

    public void setDbIndex(Integer dbIndex) {
        this.dbIndex = dbIndex;
    }

    public void setLockExpirseTime(Integer lockExpirseTime) {
        this.lockExpirseTime = lockExpirseTime;
    }

    public void setTryExpirseTime(Integer tryExpirseTime) {
        this.tryExpirseTime = tryExpirseTime;
    }


    /**
     * 第一个为key，我们使用key来当锁，因为key是唯一的。
     * 第二个为value，我们传的是requestId，很多童鞋可能不明白，有key作为锁不就够了吗，为什么还要用到value？原因就是我们在上面讲到可靠性时，分布式锁要满足第四个条件解铃还须系铃人，通过给value赋值为requestId，我们就知道这把锁是哪个请求加的了，在解锁的时候就可以有依据。requestId可以使用UUID.randomUUID().toString()方法生成。
     * 第三个为nxxx，这个参数我们填的是NX，意思是SET IF NOT EXIST，即当key不存在时，我们进行set操作；若key已经存在，则不做任何操作；
     * 第四个为expx，这个参数我们传的是PX，意思是我们要给这个key加一个过期的设置，具体时间由第五个参数决定。
     * 第五个为time，与第四个参数相呼应，代表key的过期时间。
     * 总的来说，执行上面的set()方法就只会导致两种结果：1. 当前没有锁（key不存在），那么就进行加锁操作，并对锁设置个有效期，同时value表示加锁的客户端。2. 已有锁存在，不做任何操作。
     *
     * 心细的童鞋就会发现了，我们的加锁代码满足我们可靠性里描述的三个条件。
     * 首先，set()加入了NX参数，可以保证如果已有key存在，则函数不会调用成功，也就是只有一个客户端能持有锁，满足互斥性。
     * 其次，由于我们对锁设置了过期时间，即使锁的持有者后续发生崩溃而没有解锁，锁也会因为到了过期时间而自动解锁（即key被删除），不会发生死锁。
     * 最后，因为我们将value赋值为requestId，代表加锁的客户端请求标识，那么在客户端在解锁的时候就可以进行校验是否是同一个客户端。
     * 由于我们只考虑Redis单机部署的场景，所以容错性我们暂不考虑。
     * @param key 获取锁的 key
     * @return
     */
    @Override
    public String lock(String key){
        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            key = KEY_PRE + key;
            String value = fetchLockValue();
            if (SET_SUCCESS.equals(jedis.set(key, value, "NX", "EX", lockExpirseTime))) {
                log.debug("Reids Lock key : " + key + ",value : " + value);
                return value;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }


    /**
     *
     *      * 通过一个key 再超时时间内获取锁，如果不能马上获取锁，则等待获取，直到时间超时
     *      *
     *      * @param key 获取锁的 key
     *      * @return 如果获取成功返回锁定值，否则返回 null
     *
             获取锁的 key
     * @param key
     * @return
     */
    @Override
    public String tryLock(String key){

        Jedis jedis = null;
        long tryCount = 0;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            key = KEY_PRE + key;
            String value = fetchLockValue();
           // Long firstTryTime = new Date().getTime();
            Long firstTryTime = System.currentTimeMillis();
            do {
                if (SET_SUCCESS.equals(jedis.set(key, value, "NX", "EX", lockExpirseTime))) {

                    System.out.println(Thread.currentThread().getName() + " try get lock success count=" + (++tryCount));
                    log.debug("Reids Lock key : " + key + ",value : " + value);
                    return value;
                }
                ++tryCount;
                log.info("Redis lock failure,waiting try next");
                try {
                    // 随机休眠[50,300) 毫秒
                    // 避免出现大量锁同时竞争
                    //Thread.sleep(100);
                    Thread.sleep(this.random(250) + 50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
           // } while ((new Date().getTime() - tryExpirseTime * 1000) < firstTryTime);
            } while ((System.currentTimeMillis() - firstTryTime) < tryExpirseTime * 1000);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return null;
    }



    @Override
    public boolean unLock(String key, String value){

        Long RELEASE_SUCCESS = 1L;
        Jedis jedis = null;
        try {
            jedis = redisConnection.getJedis();
            jedis.select(dbIndex);
            key = KEY_PRE + key;
            String command = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
            if (RELEASE_SUCCESS.equals(jedis.eval(command, Collections.singletonList(key), Collections.singletonList(value)))) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }

    /**
     * 生成加锁的唯一字符串
     *
     * @return 唯一字符串
     */
    private String fetchLockValue() {
        return UUID.randomUUID().toString() + "_" + df.format(new Date());
    }

    private int random(int max) {
        return (int) (Math.random() * max);
    }


//    错误示例1
//    比较常见的错误示例就是使用jedis.setnx()和jedis.expire()组合实现加锁，代码如下：

    public static void wrongGetLock1(Jedis jedis, String lockKey, String requestId, int expireTime) {

        Long result = jedis.setnx(lockKey, requestId);
        if (result == 1) {
            // 若在这里程序突然崩溃，则无法设置过期时间，将发生死锁
            jedis.expire(lockKey, expireTime);
        }

    }

//    setnx()方法作用就是SET IF NOT EXIST，expire()方法就是给锁加一个过期时间。
//    乍一看好像和前面的set()方法结果一样，然而由于这是两条Redis命令，不具有原子性，
//    如果程序在执行完setnx()之后突然崩溃，导致锁没有设置过期时间。那么将会发生死锁。
//    网上之所以有人这样实现，是因为低版本的jedis并不支持多参数的set()方法。
//
//    错误示例2
//
//    这一种错误示例就比较难以发现问题，而且实现也比较复杂。实现思路：使用jedis.setnx()命令实现加锁，其中key是锁，value是锁的过期时间。执行过程：
//            1. 通过setnx()方法尝试加锁，如果当前锁不存在，返回加锁成功。
//            2. 如果锁已经存在则获取锁的过期时间，和当前时间比较，如果锁已经过期，则设置新的过期时间，返回加锁成功。
//    代码如下：

    /**
     *
     * @param jedis
     * @param lockKey
     * @param expireTime
     * @return
     */
    public static boolean wrongGetLock2(Jedis jedis, String lockKey, int expireTime) {

        long expires = System.currentTimeMillis() + expireTime;
        String expiresStr = String.valueOf(expires);

        // 如果当前锁不存在，返回加锁成功
        if (jedis.setnx(lockKey, expiresStr) == 1) {
            return true;
        }

        // 如果锁存在，获取锁的过期时间
        String currentValueStr = jedis.get(lockKey);
        if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {
            // 锁已过期，获取上一个锁的过期时间，并设置现在锁的过期时间
            String oldValueStr = jedis.getSet(lockKey, expiresStr);
            if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
                // 考虑多线程并发的情况，只有一个线程的设置值和当前值相同，它才有权利加锁
                return true;
            }
        }

        // 其他情况，一律返回加锁失败
        return false;

    }
//    那么这段代码问题在哪里？1. 由于是客户端自己生成过期时间，所以需要强制要求分布式下每个客户端的时间必须同步。
//            2. 当锁过期的时候，如果多个客户端同时执行jedis.getSet()方法，那么虽然最终只有一个客户端可以加锁，
//                 但是这个客户端的锁的过期时间可能被其他客户端覆盖。
//            3. 锁不具备拥有者标识，即任何客户端都可以解锁。



    public static void wrongReleaseLock1(Jedis jedis, String lockKey) {
        jedis.del(lockKey);
    }
//    最常见的解锁代码就是直接使用jedis.del()方法删除锁，
//    这种不先判断锁的拥有者而直接解锁的方式，会导致任何客户端都可以随时进行解锁，即使这把锁不是它的。

    public static void wrongReleaseLock2(Jedis jedis, String lockKey, String requestId) {
        // 判断加锁与解锁是不是同一个客户端
        if (requestId.equals(jedis.get(lockKey))) {
            // 若在此时，这把锁突然不是这个客户端的，则会误解锁
            jedis.del(lockKey);
        }
    }



    public static void main(String[] args) {

    }

}
