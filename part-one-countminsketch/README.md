# Part One: Count-Min Sketch

## Trending Topics with Count-Min Sketch

This module demonstrates the first step in building an efficient trending topics detection system using probabilistic data structures. It focuses on efficient word frequency counting using Count-Min Sketch.

### What is Count-Min Sketch?
Count-Min Sketch is a probabilistic data structure for approximate frequency counting that uses significantly less memory than deterministic counters while providing good accuracy with controlled error bounds.

### How it works in this module:

1. **Data Ingestion**
   - Connects to a Bluesky social media WebSocket stream
   - Processes text data from posts
   - Cleans and normalizes text (removes punctuation, converts to lowercase)

2. **Frequency Counting**
   - Uses Count-Min Sketch to track word frequencies
   - Organizes counts into time-based buckets (per minute)
   - Also tracks word pairs to maintain contextual information

3. **Implementation Details**
   ```java
   // Initialize CMS with width and depth parameters
   redisService.createCms("words-bucket-cms:" + timeBucket, 1000, 7);
   
   // Track word frequencies
   redisService.cmsIncrBy("words-bucket-cms:" + timeBucket, word, 1);
   
   // Query word frequencies
   long count = redisService.cmsQuery("words-bucket-cms:" + timeBucket, word);
   ```

4. **Memory Comparison**
   - **Probabilistic (CMS)**: Fixed memory usage of ~7KB (1000 width × 7 depth)
   - **Deterministic (ZSET)**: Memory grows linearly with unique words
   - For comparison, both approaches are implemented side-by-side

5. **Advantages**
   - O(1) time complexity for updates and queries
   - Memory usage doesn't grow with data size
   - Suitable for high-throughput real-time applications

6. **Limitations**
   - Provides approximate counts with small error probability
   - Cannot provide exact set membership testing
   - Basic trending detection without advanced filtering

### Next Steps
The next module will introduce Bloom Filters to efficiently filter out stopwords and improve the quality of trending topics detection. 