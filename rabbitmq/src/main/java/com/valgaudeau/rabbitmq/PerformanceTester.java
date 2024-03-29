package com.valgaudeau.rabbitmq;

import com.valgaudeau.rabbitmq.services.ServiceOne;
import com.valgaudeau.rabbitmq.services.ServiceTwo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class PerformanceTester {

    private final ServiceOne serviceOne;
    private final ServiceTwo serviceTwo;

    @Autowired
    public PerformanceTester(ServiceOne serviceOne, ServiceTwo serviceTwo) {
        this.serviceOne = serviceOne;
        this.serviceTwo = serviceTwo;
    }

    public void runTestNumberOfMessages(int numberOfMessages) {
        Instant start = Instant.now();

        for (int i = 1; i <= numberOfMessages; i++) {
            String message = "Test Message " + i;
            serviceOne.sendMessage(message);
        }

        // We have to wait for all messages to be processed
        // The @RabbitListener annotated method in ServiceTwo is invoked in a separate thread
        // and operates asynchronously
        while (serviceTwo.getProcessedMessageCount() < numberOfMessages) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Once we've waited for all messages to be processed, we resume this thread
        // Not perfect, but it's vey close to the real time performance
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("Total time taken: " + duration.toMillis() + " milliseconds");
    }

    public void runTestConcurrentUsers(int concurrentUsers) {
        Instant start = Instant.now();
        executeConcurrentTest(concurrentUsers);
        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("Total time taken: " + duration.toMillis() + " milliseconds");
    }

    private void executeConcurrentTest(int concurrentUsers) {
        System.out.println("Simulating " + concurrentUsers + " concurrent users...");
        Instant start = Instant.now();
        ExecutorService executorService = Executors.newFixedThreadPool(concurrentUsers);
        int messagesPerSecond = 1;
        int messagesPerUser = messagesPerSecond * 10; // Total number of messages we want to send, which controls time

        for (int i = 1; i <= concurrentUsers; i++) {
            int finalI = i;
            // we use executorService to submit tasks for each of our simulated concurrentUsers on separate threads
            executorService.submit(() -> {
                Instant userStart = Instant.now();
                for (int j = 1; j <= messagesPerUser; j++) {
                    String message = "Test Message " + j;
                    serviceOne.sendMessage(message);
                    sleep(1000); // milliseconds before user sends next message
                }
                Instant userEnd = Instant.now();
                Duration userDuration = Duration.between(userStart, userEnd);
                System.out.println("User " + finalI + " - Average Response Time: "
                        + userDuration.dividedBy(messagesPerUser).toMillis() + " milliseconds");
            });
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Instant end = Instant.now();
        Duration duration = Duration.between(start, end);
        System.out.println("Time taken for " + concurrentUsers + " users: " + duration.toMillis() + " milliseconds");
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
