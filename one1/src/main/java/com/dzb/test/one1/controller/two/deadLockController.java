package com.dzb.test.one1.controller.two;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author dongzhengbei
 * @version 1.0.0
 * @ClassName deadLockController.java
 * @Description TODO
 * @createTime 2020年03月18日 16:18:00
 */
@RestController
@RequestMapping("deadLock/*")
@Slf4j
public class deadLockController {

    private ConcurrentHashMap<String, Item> items  = new ConcurrentHashMap<>();

    //初始化10个商品对象来模拟商品清单
    public deadLockController(){
        IntStream.range(0, 10).forEach(i->items.put("item" + i, new Item("item" + i)));
    }

    /**
     * @description 1. 定义一个商品类型：包含商品名、库存剩余和商品的库存锁三个属性
     */
    @Data
    @RequiredArgsConstructor
    @ToString(exclude="lock") //ToString不包含这个字段
    static class Item {
        final String name; //商品名
        int remaining = 1000; //库存剩余
//        @ToString.Exclude //ToString不包含这个字段
        ReentrantLock lock = new ReentrantLock();
    }

    /**
     * @description 2. 模拟选购：每次从商品清单中随机选购3个商品
     */
    private List<Item> createCart(){
        return IntStream.rangeClosed(1, 3).mapToObj(i->"item" + ThreadLocalRandom.current().nextInt(items.size()))
                .map(name->items.get(name)).collect(Collectors.toList());
    }

    /**
     * @description 3. 声明一个List保存所有获得的锁，遍历购物车中商品依次尝试获得商品锁
     *    最长等待10秒，获得全部锁；如果有无法获得锁的情况则解锁之前获得的所有锁，返回false下单失败
     *          获得全部锁后再扣减库存
     */
    private boolean createOrder(List<Item> order){
        //存放所有获得的锁
        List<ReentrantLock> locks = new ArrayList<>();

        for (Item item : order){
            try {
                //获得锁10秒超时
                if(item.lock.tryLock(10, TimeUnit.SECONDS)){
                    locks.add(item.lock);
                }else {
                    locks.forEach(ReentrantLock::unlock);
                    return false;
                }
            } catch (InterruptedException e) {
            }
        }
        //锁全部拿到之后执行扣减库存业务逻辑
        try{
            order.forEach(item -> item.remaining--);
        }finally {
            locks.forEach(ReentrantLock::unlock);
        }
        return true;
    }


    /**
     * @description 4. 模拟在多线程情况下进行100次创建购物车和下单操作
     *     最后通过日志输出成功的下单次数、总剩余的商品个数、100次下单耗时，以及下单完成后的商品库存明细
     */
    @GetMapping("wrong")
    public long wrong(){
        long begin = System.currentTimeMillis();

        long success = IntStream.rangeClosed(1, 100).parallel()
                .mapToObj(i->{
                    List<Item> cart = createCart();
                    return createOrder(cart);
                }).filter(result -> result)
                .count();
        log.info("success:{} totalRemaining:{} took:{}ms items:{}",
                success,
                items.entrySet().stream().map(item->item.getValue().remaining).reduce(0, Integer::sum),
                System.currentTimeMillis() - begin, items);
        return success;
    }

    /**
     * @description 避免死锁：为购物车中的商品排序，让所有线程获取商品锁的顺序固定。
     */
    @GetMapping("right")
    public long right(){
        long begin = System.currentTimeMillis();

        long success = IntStream.rangeClosed(1, 100).parallel()
                .mapToObj(i->{
                    List<Item> cart = createCart().stream()
                            .sorted(Comparator.comparing(Item::getName))
                            .collect(Collectors.toList());
                    return createOrder(cart);
                }).filter(result -> result)
                .count();
        log.info("success:{} totalRemaining:{} took:{}ms items:{}",
                success,
                items.entrySet().stream().map(item->item.getValue().remaining).reduce(0, Integer::sum),
                System.currentTimeMillis() - begin, items);
        return success;
    }
}
