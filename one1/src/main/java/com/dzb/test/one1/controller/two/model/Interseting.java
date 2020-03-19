package com.dzb.test.one1.controller.two.model;

import lombok.extern.slf4j.Slf4j;

/**
 * @author dongzhengbei
 * @version 1.0.0
 * @ClassName Interseting.java
 * @Description TODO
 * @createTime 2020年03月17日 20:28:00
 */
@Slf4j
public class Interseting {

    volatile  int a = 1;
    volatile  int b = 1;

//    public void add(){
    public synchronized void add(){
        log.info("add start");
        for (int i = 0; i < 10000; i++){
            a++;
            b++;
        }
        log.info("add done");
    }

//    public void compare(){
    public synchronized void compare(){
        log.info("compare start");
        for (int i = 0; i < 10000; i++){
            if(a < b){
                log.info("a:{},b:{},{}", a, b, a > b);
            }
        }
        log.info("compare done");
    }
}
