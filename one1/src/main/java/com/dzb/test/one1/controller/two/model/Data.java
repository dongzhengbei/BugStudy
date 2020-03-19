package com.dzb.test.one1.controller.two.model;

import lombok.Getter;

/**
 * @author dongzhengbei
 * @version 1.0.0
 * @ClassName Data.java
 * @Description TODO
 * @createTime 2020年03月18日 10:09:00
 */
public class Data {
    @Getter
    private static int counter = 0;
    private static Object locker = new Object();

    public void right(){
        synchronized (locker){
            counter++;
        }
    }

    public static int reset(){
        counter = 0;
        return counter;
    }

    public synchronized void  wrong(){
        counter++;
    }
}
