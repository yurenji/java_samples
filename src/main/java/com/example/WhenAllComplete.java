package com.example;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class WhenAllComplete {
    public static void main(String[] args) {
        var executor = Executors.newFixedThreadPool(3);
        var listeningExecutor = MoreExecutors.listeningDecorator(executor);

        // Create some ListenableFutures (similar setup as before)
        List<ListenableFuture<Integer>> futures = Arrays.asList(
            listeningExecutor.submit(() -> {
                Thread.sleep(100);
                System.out.println("Task 1 completed");
                return 1;
            }),
            listeningExecutor.submit(() -> {
                Thread.sleep(200);
                System.out.println("Task 2 completed");
                return 2;
            }),
            listeningExecutor.submit(() -> {
                Thread.sleep(150);
                System.out.println("Task 3 throwing exception");
                throw new RuntimeException("Task 3 failed!");
            })
        );

        // Use whenAllComplete to handle both success and failure cases
        ListenableFuture<List<Integer>> allComplete = Futures.whenAllComplete(futures)
            .call(() -> {
                System.out.println("\nAll tasks completed (successfully or with failure)");
                
                // Check the status of each future
                for (int i = 0; i < futures.size(); i++) {
                    ListenableFuture<Integer> future = futures.get(i);
                    try {
                        Integer result = future.get();
                        System.out.printf("Future %d succeeded with result: %d%n", i + 1, result);
                    } catch (ExecutionException e) {
                        System.out.printf("Future %d failed with error: %s%n", i + 1, e.getCause().getMessage());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        System.out.printf("Future %d was interrupted%n", i + 1);
                    }
                }

                // Collect successful results (if you need them)
                return futures.stream()
                    .map(f -> {
                        try {
                            return f.get();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .toList();
            }, MoreExecutors.directExecutor());

        // Add a listener to handle the final result
        allComplete.addListener(() -> {
            try {
                List<Integer> results = allComplete.get();
                System.out.println("\nFinal results (null for failed tasks): " + results);
            } catch (Exception e) {
                System.out.println("Error getting final results: " + e.getMessage());
            } finally {
                executor.shutdown();
            }
        }, MoreExecutors.directExecutor());
    }
}