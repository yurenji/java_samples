package com.example;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class AllAsList {
    public static void main(String[] args) {
        var executor = Executors.newFixedThreadPool(3);
        var listeningExecutor = MoreExecutors.listeningDecorator(executor);

        // Create some ListenableFutures
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

        // Combine futures using allAsList
        ListenableFuture<List<Integer>> allFutures = Futures.allAsList(futures);

        // Add callback to handle the result
        allFutures.addListener(() -> {
            try {
                // This will throw an ExecutionException because one of the futures failed
                List<Integer> results = allFutures.get();
                System.out.println("All tasks completed successfully: " + results);
            } catch (ExecutionException e) {
                System.out.println("Combined future failed because: " + e.getCause().getMessage());
                
                // Let's check the status of individual futures
                System.out.println("\nChecking individual futures:");
                for (int i = 0; i < futures.size(); i++) {
                    try {
                        Integer result = futures.get(i).get();
                        System.out.printf("Future %d completed with result: %d%n", i + 1, result);
                    } catch (ExecutionException ex) {
                        System.out.printf("Future %d failed with error: %s%n", i + 1, ex.getCause().getMessage());
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                        System.out.printf("Future %d was interrupted%n", i + 1);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Operation was interrupted");
            } finally {
                executor.shutdown();
            }
        }, MoreExecutors.directExecutor());
    }
} 