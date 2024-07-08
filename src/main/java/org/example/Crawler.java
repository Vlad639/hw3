package org.example;

import me.tongfei.progressbar.ProgressBar;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Чтобы найти самый короткий путь, нам нужно проверить все возможные переходы с
 * одной страницы на другую, построим для этого дерево страниц.
 * <p>
 * Этот класс - многопоточный обходчик страниц, принцип работы следующий:
 * Генерирует дерево из посещённых страниц.
 * С помощью заданного количества потоков заполняет поочерёдно уровни дерева от корня до заданного уровня.
 * Прогресс обхода каждого уровня отображается с помощью прогресс-бара.
 * <p>
 */
public class Crawler extends Thread {

    private final String title;
    private final String target;
    private final int maxDepth;
    private final long timeoutMls;

    private final Queue<Node<String>> currentLevel = new LinkedBlockingQueue<>();
    private final Queue<Node<String>> nextLevel = new LinkedBlockingQueue<>();
    private final List<CrawlerThread> crawlers = new CopyOnWriteArrayList<>();

    private static final int THREADS_NUM = 100;

    private volatile Node<String> bestResult;
    private boolean stop;

    public Crawler(String title, String target, int maxDepth, long timeout, TimeUnit timeUnit) {
        this.title = title;
        this.target = target;
        this.maxDepth = maxDepth;
        this.timeoutMls = timeUnit.toMillis(timeout);
    }

    @Override
    public void run() {
        Node<String> root = new Node<>(title, 0, null);
        currentLevel.add(root);

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                stopThreads();
                stop = true;
            }
        }, timeoutMls);

        for (int level = 0; level < maxDepth; level++) {
            try (ProgressBar progressBar = new ProgressBar("Graph level " + level, currentLevel.size())) {
                if (stop) {
                    stopThreads();
                    break;
                }

                for (int i = 0; i < THREADS_NUM; i++) {
                    crawlers.add(new CrawlerThread(target, currentLevel, nextLevel, progressBar, this));
                }
                crawlers.forEach(CrawlerThread::start);

                for (CrawlerThread thread : crawlers) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (stop) {
                    stopThreads();
                    break;
                }

                crawlers.clear();
                currentLevel.addAll(nextLevel);
                nextLevel.clear();
            }
        }

        printBestResult();
    }

    private void stopThreads() {
        crawlers.forEach(CrawlerThread::stopThread);
    }

    public synchronized void addFoundedTarget(Node<String> node) {
        if (bestResult == null || node.getLevel() < bestResult.getLevel()) {
            bestResult = node;
        }
    }

    public void printBestResult() {
        System.out.println();
        if (bestResult == null) {
            System.out.printf("'%s' not found", target);
        } else {
            List<String> result = new ArrayList<>();
            Node<String> current = bestResult;
            while (current != null) {
                result.add(current.getValue());
                current = current.getParent();
            }

            Collections.reverse(result);

            System.out.printf("'%s' found: %s", target, String.join(" -> ", result));
        }
    }
}
