# Part Three: Top-K

## Complete Trending Topics System with Top-K

This final module completes our trending topics detection system by adding TopK, a probabilistic data structure that efficiently maintains only the most frequent items in a stream.

### What is Top-K?
Top-K is a probabilistic data structure that tracks the most frequent elements in a data stream using significantly less memory than sorting the entire dataset. It automatically decays old counts to focus on recent trends.

### How it completes our trending system:

1. **Spike Detection**
   - Compares current frequency (from CMS) with historical average
   - Calculates spike score: `(current - pastAvg) / pastAvg`
   - Uses TopK to maintain only the highest trending items

2. **Implementation Details**
   ```java
   // Initialize TopK
   redisService.initTopK("spiking-topk", 20, 2000, 6, 0.9);
   
   // Calculate spike score
   long current = redisService.cmsQuery(now, term);
   long pastAvg = (redisService.cmsQuery(prev1, term) + 
                   redisService.cmsQuery(prev2, term) + 
                   redisService.cmsQuery(prev3, term)) / 3;
   
   double spikeScore = (current - pastAvg) / (double) pastAvg;
   
   // Update TopK with spike score
   redisService.topkIncrBy("spiking-topk", term, (int)spikeScore);
   ```

3. **Memory Comparison**
   - **Probabilistic (TopK)**: Fixed memory based on configuration (~12KB)
   - **Deterministic (Full sorted list)**: Grows with number of terms
   - Complete probabilistic system: ~21KB total (CMS + Bloom Filter + TopK)

4. **Complete Workflow**
   1. Ingest text data from stream
   2. Clean and normalize text
   3. Filter stopwords using Bloom Filter
   4. Count frequencies with Count-Min Sketch
   5. Calculate spike scores
   6. Maintain top trending items with TopK

5. **Advantages**
   - Full trending topic system with minimal memory usage
   - Automatic maintenance of only the most relevant trending terms
   - Built-in decay to focus on recent trends
   - Constant memory usage regardless of stream volume

6. **Performance Considerations**
   - All operations maintain O(1) time complexity
   - Memory usage remains constant even with billions of words
   - System can handle high-throughput streams efficiently

### Summary of Probabilistic vs Deterministic Approach

This three-part implementation demonstrates how probabilistic data structures can create an efficient trending topics system:

1. **Memory Usage**
   - Probabilistic: ~21KB fixed memory
   - Deterministic: Potentially GBs for large streams

2. **Time Complexity**
   - Both approaches offer fast operations, but probabilistic structures maintain constant time regardless of data size

3. **Accuracy**
   - Probabilistic: Small, controllable error rates
   - Deterministic: Exact counts but at much higher resource cost

4. **Scalability**
   - Probabilistic: Scales to virtually unlimited streams with fixed resources
   - Deterministic: Resource requirements grow with data volume

This implementation showcases how probabilistic data structures can solve real-world big data problems efficiently. 