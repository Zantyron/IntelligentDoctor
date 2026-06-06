package com.intelligentdoctor.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String defaultHospitalId = "hospital-demo";
    private History history = new History();
    private Ai ai = new Ai();
    private VectorStore vectorStore = new VectorStore();
    private Registration registration = new Registration();
    private Stream stream = new Stream();

    public String getDefaultHospitalId() {
        return defaultHospitalId;
    }

    public void setDefaultHospitalId(String defaultHospitalId) {
        this.defaultHospitalId = defaultHospitalId;
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(History history) {
        this.history = history;
    }

    public Ai getAi() {
        return ai;
    }

    public void setAi(Ai ai) {
        this.ai = ai;
    }

    public VectorStore getVectorStore() {
        return vectorStore;
    }

    public void setVectorStore(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration registration) {
        this.registration = registration;
    }

    public Stream getStream() {
        return stream;
    }

    public void setStream(Stream stream) {
        this.stream = stream;
    }

    public static class History {

        private String provider = "mongodb";

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }
    }

    public static class Ai {

        private String provider = "openai";
        private String baseUrl = "https://api.openai.com/v1";
        private String apiKey = "";
        private boolean useDemoKey;
        private String chatModel = "gpt-4o-mini";
        private String embeddingBaseUrl = "https://api.openai.com/v1";
        private String embeddingApiKey = "";
        private String embeddingModel = "text-embedding-3-small";
        private Integer embeddingDimensions = 1024;

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public boolean isUseDemoKey() {
            return useDemoKey;
        }

        public void setUseDemoKey(boolean useDemoKey) {
            this.useDemoKey = useDemoKey;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public String getEmbeddingBaseUrl() {
            return embeddingBaseUrl;
        }

        public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
            this.embeddingBaseUrl = embeddingBaseUrl;
        }

        public String getEmbeddingApiKey() {
            return embeddingApiKey;
        }

        public void setEmbeddingApiKey(String embeddingApiKey) {
            this.embeddingApiKey = embeddingApiKey;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public Integer getEmbeddingDimensions() {
            return embeddingDimensions;
        }

        public void setEmbeddingDimensions(Integer embeddingDimensions) {
            this.embeddingDimensions = embeddingDimensions;
        }

        public String resolvedApiKey() {
            if (useDemoKey && (apiKey == null || apiKey.isBlank())) {
                return "demo";
            }
            return apiKey;
        }

        public String resolvedEmbeddingApiKey() {
            if (embeddingApiKey != null && !embeddingApiKey.isBlank()) {
                return embeddingApiKey;
            }
            return resolvedApiKey();
        }
    }

    public static class VectorStore {

        private String provider = "pinecone";
        private String namespacePrefix = "intelligent-doctor";
        private Pinecone pinecone = new Pinecone();

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getNamespacePrefix() {
            return namespacePrefix;
        }

        public void setNamespacePrefix(String namespacePrefix) {
            this.namespacePrefix = namespacePrefix;
        }

        public Pinecone getPinecone() {
            return pinecone;
        }

        public void setPinecone(Pinecone pinecone) {
            this.pinecone = pinecone;
        }
    }

    public static class Pinecone {

        private String apiKey = "";
        private String indexHost = "";
        private String apiVersion = "2026-04";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getIndexHost() {
            return indexHost;
        }

        public void setIndexHost(String indexHost) {
            this.indexHost = indexHost;
        }

        public String getApiVersion() {
            return apiVersion;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }
    }

    public static class Registration {

        private String stockProvider = "redis";
        private String eventProvider = "kafka";
        private String kafkaTopic = "registration.reserved";
        @Min(5)
        private long draftExpireMinutes = 30;

        public String getStockProvider() {
            return stockProvider;
        }

        public void setStockProvider(String stockProvider) {
            this.stockProvider = stockProvider;
        }

        public String getEventProvider() {
            return eventProvider;
        }

        public void setEventProvider(String eventProvider) {
            this.eventProvider = eventProvider;
        }

        public String getKafkaTopic() {
            return kafkaTopic;
        }

        public void setKafkaTopic(String kafkaTopic) {
            this.kafkaTopic = kafkaTopic;
        }

        public long getDraftExpireMinutes() {
            return draftExpireMinutes;
        }

        public void setDraftExpireMinutes(long draftExpireMinutes) {
            this.draftExpireMinutes = draftExpireMinutes;
        }
    }

    public static class Stream {

        @Min(8)
        private int chunkSize = 26;
        @Min(1)
        private long chunkDelayMillis = 35;

        public int getChunkSize() {
            return chunkSize;
        }

        public void setChunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
        }

        public long getChunkDelayMillis() {
            return chunkDelayMillis;
        }

        public void setChunkDelayMillis(long chunkDelayMillis) {
            this.chunkDelayMillis = chunkDelayMillis;
        }
    }
}
