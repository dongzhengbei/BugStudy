package com.dzb.test.one1.controller;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author dongzhengbei
 * @version 1.0.0
 * @ClassName TestUtilsController.java
 * @Description TODO
 * @createTime 2020年03月16日 10:20:00
 */
public class TestUtilsController {
    //循环次数
    private static int LOOP_COUNT = 10000000;
    //线程数量
    private static int THREAD_COUNT = 10;
    //元素数量
    private static int ITEM_COUNT = 10;

    /**
     * @title 通过锁的方式锁住Map，然后做判断、读取现在的累计值、加1、保存累加后值的逻辑。
     * @description  功能ok，但无法充分发挥ConcurrentHashMap的威力
     */
    private Map<String, Long> normaluse() throws InterruptedException {
        ConcurrentHashMap<String, Long> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);

        forkJoinPool.execute(()-> IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(i->{
            //获得一个随机的Key
            String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
            synchronized(freqs){
                if(freqs.containsKey(key)){
                    //Key存在则+1
                    freqs.put(key, freqs.get(key) + 1);
                }else {
                    //Key不存在则初始化为1
                    freqs.put(key, 1L);
                }
            }
        }));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        return  freqs;
    }

    /**
     * @title 使用ConcurrentHashMap的原子性方法computeIfAbsent做符合逻辑操作，判断Key是否存在Value
     * @description 功能ok，ConcurrentHashMap的特性发挥
     */
    private Map<String, Long> gooduse() throws InterruptedException {
        ConcurrentHashMap<String, LongAdder> freqs = new ConcurrentHashMap<>(ITEM_COUNT);
        ForkJoinPool forkJoinPool = new ForkJoinPool(THREAD_COUNT);

        forkJoinPool.execute(()-> IntStream.rangeClosed(1, LOOP_COUNT).parallel().forEach(i->{
            //获得一个随机的Key
            String key = "item" + ThreadLocalRandom.current().nextInt(ITEM_COUNT);
            //利用computeIfAbsent()方法来实例化LongAddr，然后利用LongAddr来进行线程安全计数
            freqs.computeIfAbsent(key, k -> new LongAdder()).increment();
        }));
        forkJoinPool.shutdown();
        forkJoinPool.awaitTermination(1, TimeUnit.HOURS);
        //因为我们的Value是LongAddr而不是Long,所以需要做一次转换才能返回
        return  freqs.entrySet().stream().collect(Collectors.toMap(
                e->e.getKey(), e->e.getValue().longValue()
        ));
    }

}
