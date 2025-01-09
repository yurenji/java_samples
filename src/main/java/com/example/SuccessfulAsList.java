package com.example;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class SuccessfulAsList {
    public static void main(String[] args) {
        var executor = Executors.newFixedThreadPool(3);
        
        // Create some ListenableFutures
        List<ListenableFuture<Integer>> futures = Arrays.asList(
            MoreExecutors.listeningDecorator(executor).submit(() -> {
                Thread.sleep(100);
                return 1;
            }),
            MoreExecutors.listeningDecorator(executor).submit(() -> {
                Thread.sleep(200);
                return 2;
            }),
            MoreExecutors.listeningDecorator(executor).submit(() -> {
                Thread.sleep(150);
                throw new RuntimeException("Failed!"); // This will cause failure
            })
        );

        // Combine the futures using Futures.successfulAsList
        ListenableFuture<List<Integer>> combinedFuture = Futures.successfulAsList(futures);

        // Transform the result
        ListenableFuture<List<Integer>> transformedFuture = Futures.transform(
            combinedFuture,
            result -> {
                System.out.println("Got result: " + result);
                return result;
            },
            MoreExecutors.directExecutor()
        );

        // Add a listener to handle the transformed result
        transformedFuture.addListener(() -> {
            try {
                // This will throw an exception if the transformation future failed
                List<Integer> results = transformedFuture.get();
                System.out.println("Transformed results: " + results);
            } catch (ExecutionException e) {
                System.out.println("Transformation failed with: " + e.getCause().getMessage());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, MoreExecutors.directExecutor());

        // Shutdown the executor
        executor.shutdown();
    }
}