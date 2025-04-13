package dev.raphaeldelio.countminsketchprobabilisticdatastructure.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
    public String did;

    @JsonProperty("time_us")
    public long timeUs;

    public String kind;
    public Commit commit;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Commit {
        public String rev;
        public String operation;
        public String collection;
        public String rkey;
        public Record record;
        public String cid;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Record {
        @JsonProperty("$type")
        public String type;

        public String createdAt;
        public String text;
        public List<String> langs;
        public List<Facet> facets;
        public Reply reply;
        public Embed embed;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Reply {
        public PostRef parent;
        public PostRef root;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PostRef {
        public String cid;
        public String uri;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Facet {
        @JsonProperty("$type")
        public String type;

        public List<Feature> features;
        public Index index;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Feature {
        @JsonProperty("$type")
        public String type;
        public String did;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Index {
        public int byteStart;
        public int byteEnd;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embed {
        @JsonProperty("$type")
        public String type;
        public List<EmbedImage> images;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class EmbedImage {
        public String alt;
        public AspectRatio aspectRatio;
        public Image image;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AspectRatio {
        public int height;
        public int width;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        @JsonProperty("$type")
        public String type;
        public Ref ref;
        public String mimeType;
        public int size;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Ref {
        @JsonProperty("$link")
        public String link;
    }
}