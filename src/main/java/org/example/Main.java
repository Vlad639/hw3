package org.example;

import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Crawler crawler = new Crawler("Java_(programming_language)", "Sun-4", 3, 1, TimeUnit.MINUTES);
        crawler.start();

    }
}
