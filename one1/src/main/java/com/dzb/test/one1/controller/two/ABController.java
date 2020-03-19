package com.dzb.test.one1.controller.two;

import com.dzb.test.one1.controller.two.model.Data;
import com.dzb.test.one1.controller.two.model.Interseting;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * @author dongzhengbei
 * @version 1.0.0
 * @ClassName ABController.java
 * @Description TODO
 * @createTime 2020年03月17日 20:27:00
 */
@RestController
@RequestMapping("ab/*")
@Slf4j
public class ABController {


    @GetMapping("interseting")
    public String interseting(){
        Interseting interseting = new Interseting();
        new Thread(()->interseting.add()).start();
        new Thread(()->interseting.compare()).start();
        return "OK";
    }


    @GetMapping("dataWrong")
    public int dataWrong(@RequestParam(value = "count", defaultValue = "1000000") int count){
        Data.reset();
        //多线程循环一定次数调用Data类不同实例的wrong方法
        IntStream.rangeClosed(1, count).parallel().forEach(i->new Data().wrong());
        return Data.getCounter();
    }

    @GetMapping("dataRight")
    public int dataRight(@RequestParam(value = "count", defaultValue = "1000000") int count){
        Data.reset();
        //多线程循环一定次数调用Data类不同实例的wrong方法
        IntStream.rangeClosed(1, count).parallel().forEach(i->new Data().right());
        return Data.getCounter();
    }

    private List<Integer> data = new ArrayList<>();

    //不涉及共享资源的慢方法
    private void slow()  {
        try{
            TimeUnit.MICROSECONDS.sleep(10);
        }catch (InterruptedException e){
        }
    }

    //错误的加锁方法
    @GetMapping("wrong")
    public int wrong(){
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i->{
            //加锁粒度太粗了
            synchronized (this){
                slow();
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
    }

    //正确的加锁方法
    @GetMapping("right")
    public int right(){
        long begin = System.currentTimeMillis();
        IntStream.rangeClosed(1, 1000).parallel().forEach(i->{
            slow();
            //只对List加锁
            synchronized (data){
                data.add(i);
            }
        });
        log.info("took:{}", System.currentTimeMillis() - begin);
        return data.size();
    }

}
