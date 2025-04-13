# Probabilistic Data Structures for Trending Topics

This repository demonstrates how to build an efficient trending topics detection system using three powerful probabilistic data structures:

1. **[Count-Min Sketch](part-one-countminsketch/README.md)** - For memory-efficient frequency counting
2. **[Bloom Filter](part-two-bloomfilter/README.md)** - For efficient stopword filtering
3. **[Top-K](part-three-topk/README.md)** - For maintaining only the most trending items (determined by a spike detection algorithm)

## Overview

This project shows the evolution of a trending topics detection system, demonstrating step-by-step how each probabilistic data structure contributes to building a complete solution.

Each module builds upon the previous one, adding new capabilities while maintaining memory efficiency:

- **Part One**: Implements basic word counting with Count-Min Sketch
- **Part Two**: Adds stopword filtering with Bloom Filters
- **Part Three**: Completes the system with trending detection using TopK

## Why Probabilistic Data Structures?

Traditional deterministic data structures (like hashmaps and arrays) require memory proportional to the data size. For high-volume data streams like social media, this becomes prohibitively expensive.

Probabilistic data structures offer:
- Constant memory usage regardless of data volume
- O(1) time complexity for operations
- Controlled error bounds
- Ability to handle virtually unlimited data streams

## Repository Structure

Each part is implemented as a separate Spring Boot application, showing how the system evolves:

```
├── part-one-countminsketch/     # Basic frequency counting
├── part-two-bloomfilter/        # + Stopword filtering
└── part-three-topk/             # + Trending detection
```

## Getting Started

1. Each module requires Redis 8 installed
2. Navigate to the specific module you want to run
3. Start the Spring Boot application with `./mvnw spring:run`

## Memory Comparison

| Data Structure    | Probabilistic     | Deterministic     |
|-------------------|-------------------|-------------------|
| Counting (CMS)    | ~7KB fixed        | Grows with data   |
| Filtering (Bloom) | ~2KB fixed        | ~26KB for 1300 words |
| Ranking (TopK)    | ~12KB fixed       | Grows with data   |
| **Total**         | **~21KB fixed**   | **Potentially GBs** |

Check each module's README for detailed explanations of the implementation. 