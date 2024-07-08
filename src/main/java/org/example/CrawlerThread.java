package org.example;

import me.tongfei.progressbar.ProgressBar;

import java.util.Queue;
import java.util.Set;

/**
 * Поток обходчика страниц, ищет ссылки на странице и добавляет их на следующий уровень дерева.
 * Википедия имеет ограничение в 200 запросов в секунду, при превышении этого лимита получим исключение.
 * Через 1,5 секунды пробуем снова выполнить запрос.
 */
public class CrawlerThread extends Thread {

    private final WikiClient client = new WikiClient();
    private final Queue<Node<String>> currentLevel;
    private final Queue<Node<String>> nextLevel;
    private final ProgressBar progressBar;
    private final String target;
    private final Crawler crawler;

    private boolean stop;

    public CrawlerThread(String target, Queue<Node<String>> currentLevel, Queue<Node<String>> nextLevel, ProgressBar progressBar, Crawler crawler) {
        this.target = target;
        this.currentLevel = currentLevel;
        this.nextLevel = nextLevel;
        this.progressBar = progressBar;
        this.crawler = crawler;
    }

    @Override
    public void run() {
        while (!stop) {
            Node<String> parentNode = currentLevel.poll();
            if (parentNode == null) {
                return;
            }

            String title = parentNode.getValue();
            if (target.equalsIgnoreCase(title)) {
                crawler.addFoundedTarget(parentNode);
                progressBar.step();
                return;
            }

            boolean complete = false;
            Set<String> links = null;
            while (!complete) {
                if (stop) {
                    return;
                }
                try {
                    links = client.getByTitle(title);
                    complete = true;
                } catch (Exception e) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }

            links.forEach(link -> nextLevel.add(new Node<>(link, parentNode.getLevel() + 1, parentNode)));
            progressBar.step();
        }
    }

    public void stopThread() {
        stop = true;
    }
}
