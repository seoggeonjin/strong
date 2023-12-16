package net.ua.util;

public class Counter {
    private int c = 0;
    public Counter(int c) {
        this.c = c;
    }
    public Counter() {
        this(0);
    }
    public void pp(int c) {
        this.c += c;
    }
    public int pp() {
        pp(1);
        return c;
    }
    public int get() {
        return c;
    }
    public void set(int c) {
        this.c = c;
    }
}
