package com.example;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class WhenAllSucceed {
    public static void main(String[] args) {
        var executor = Executors.newFixedThreadPool(3);
        var listeningExecutor = MoreExecutors.listeningDecorator(executor);

        // First example: All futures succeed
        System.out.println("Example 1: All futures succeed");
        List<ListenableFuture<Integer>> successfulFutures = Arrays.asList(
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
                System.out.println("Task 3 completed");
                return 3;
            })
        );

        // Using whenAllSucceed with successful futures
        ListenableFuture<Integer> successfulResult = Futures.whenAllSucceed(successfulFutures)
            .call(() -> {
                System.out.println("\nAll tasks succeeded! Calculating sum...");
                int sum = 0;
                for (ListenableFuture<Integer> future : successfulFutures) {
                    sum += future.get(); // Safe to call get() as we know they all succeeded
                }
                return sum;
            }, MoreExecutors.directExecutor());

        // Second example: One future fails
        System.out.println("\nExample 2: One future fails");
        List<ListenableFuture<Integer>> mixedFutures = Arrays.asList(
            listeningExecutor.submit(() -> {
                Thread.sleep(100);
                System.out.println("Task 4 completed");
                return 4;
            }),
            listeningExecutor.submit(() -> {
                Thread.sleep(150);
                System.out.println("Task 5 throwing exception");
                throw new RuntimeException("Task 5 failed!");
            }),
            listeningExecutor.submit(() -> {
                Thread.sleep(200);
                System.out.println("Task 6 completed");
                return 6;
            })
        );

        // Using whenAllSucceed with mixed futures (some will fail)
        ListenableFuture<Integer> mixedResult = Futures.whenAllSucceed(mixedFutures)
            .call(() -> {
                System.out.println("This won't be printed because one task failed");
                return -1;
            }, MoreExecutors.directExecutor());

        // Add listeners to handle both cases
        successfulResult.addListener(() -> {
            try {
                int sum = successfulResult.get();
                System.out.println("Sum of successful futures: " + sum);
            } catch (Exception e) {
                System.out.println("Unexpected error in successful case: " + e.getMessage());
            }
        }, MoreExecutors.directExecutor());

        mixedResult.addListener(() -> {
            try {
                mixedResult.get();
            } catch (ExecutionException e) {
                System.out.println("As expected, combined future failed because: " + e.getCause().getMessage());
                
                // Check individual futures
                System.out.println("\nStatus of individual futures:");
                for (int i = 0; i < mixedFutures.size(); i++) {
                    try {
                        Integer result = mixedFutures.get(i).get();
                        System.out.printf("Future %d succeeded with result: %d%n", i + 1, result);
                    } catch (ExecutionException ex) {
                        System.out.printf("Future %d failed with error: %s%n", i + 1, ex.getCause().getMessage());
                    } catch (InterruptedException ex) {
                        System.out.printf("Future %d was interrupted%n", i + 1);
                    }
                }
            } catch (InterruptedException e) {
                System.out.println("Operation was interrupted");
            } finally {
                executor.shutdown();
            }
        }, MoreExecutors.directExecutor());
    }
} 