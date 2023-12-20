package com.grameenphone.wipro.task_executor.util;

public class KV<A, B> {
    public A key;
    public B value;

    public KV(A a, B b) {
        key = a;
        value = b;
    }
}