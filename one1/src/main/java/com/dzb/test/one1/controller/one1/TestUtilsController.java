package com.dzb.test.one1.controller.one1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StopWatch;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
@RestController
public class TestUtilsController {

    private static Logger log = LoggerFactory.getLogger(TestUtilsController.class);


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
     * @title 使用ConcurrentHashMap的原子性方法computeIfAbsent做符合逻辑操作，判断Key是否存在Value:
     *      不存在则把Lambda表达式运行后的结果放入Map作为Value，也就是新创建一个LongAdder对象，最后返回Value
     *         由于方法返回的Value是LongAddr，是一个线程安全的累加器，因此可以直接调用其increment方法进行累加
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


    /**
     * @title test以上两段代码的性能
     * @description 使用stopWatch测试性能，最后跟一个断言判断Map中元素的个数以及所有Value的和，是否符合预期来校验代码的正确性
     */
    @GetMapping("good")
    public String good() throws InterruptedException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start("normaluse");
        Map<String, Long> normaluse = normaluse();
        stopWatch.stop();
        //校验元素数量
        Assert.isTrue(normaluse.size() == ITEM_COUNT, "normaluse size error");
        //校验累计总数
        Assert.isTrue(normaluse.entrySet().stream().mapToLong(item->item.getValue())
                .reduce(0, Long::sum) == LOOP_COUNT, "normaluse count error");

        stopWatch.start("gooduse");
        Map<String, Long> gooduse = gooduse();
        stopWatch.stop();
        Assert.isTrue(gooduse.size() == ITEM_COUNT, "gooduse size error");
        Assert.isTrue(gooduse.entrySet().stream()
        .mapToLong(item -> item.getValue())
        .reduce(0, Long::sum) == LOOP_COUNT, "gooduse count error");
        log.info(stopWatch.prettyPrint());
        return "OK";
    }

}
