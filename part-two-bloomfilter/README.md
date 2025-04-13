# Part Two: Bloom Filter

## Trending Topics with Bloom Filter

This module builds upon the previous Count-Min Sketch implementation by adding Bloom Filters to efficiently filter out stopwords and improve trending topic quality.

### What is a Bloom Filter?
A Bloom Filter is a space-efficient probabilistic data structure used to test whether an element is a member of a set. It may produce false positives (indicating an element is in the set when it's not), but never false negatives.

### How it improves our trending topics system:

1. **Stopword Filtering**
   - Initializes a Bloom Filter with common stopwords (a, the, and, etc.)
   - Filters out these words before counting frequencies
   - Improves quality by focusing on meaningful content words

2. **Implementation Details**
   ```java
   // Initialize Bloom Filter
   redisService.createBloomFilter("stopwords-bf", 1300, 0.01);
   
   // Add stopwords to the filter
   redisService.addMultiToBloomFilter("stopwords-bf", stopwords);
   
   // Check if word is a stopword
   if (!redisService.isInBloomFilter("stopwords-bf", word)) {
       // Process meaningful words only
       redisService.cmsIncrBy("words-bucket-cms:" + timeBucket, word, 1);
   }
   ```

3. **Memory Comparison**
   - **Probabilistic (Bloom Filter)**: ~2KB for 1300 words with 1% false positive rate
   - **Deterministic (Set)**: Grows linearly with number of stopwords (~59KB for 1300 words)

4. **Enhanced Workflow**
   - Text data from stream → Clean and normalize → Filter stopwords with Bloom Filter → Count with CMS → Basic trending analysis

5. **Advantages**
   - Improved trending topic quality by removing noise words
   - Constant memory usage regardless of input data volume
   - Fast O(1) lookups for stopword checking

6. **Limitations**
   - Still using basic trending detection (current vs. historical)
   - No automatic ranking of top trending items
   - Small chance of false positives in stopword filtering

### Next Steps
The final module will add a TopK data structure to efficiently track and maintain only the most trending topics without storing the entire dataset. 