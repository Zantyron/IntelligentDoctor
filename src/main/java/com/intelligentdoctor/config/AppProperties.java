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
    private Agent agent = new Agent();
    private Admin admin = new Admin();
    private Executor executor = new Executor();
    private RateLimit rateLimit = new RateLimit();

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

    public Agent getAgent() {
        return agent;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(RateLimit rateLimit) {
        this.rateLimit = rateLimit;
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
        @Min(1000)
        private long timeoutMillis = 300000;

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

        public long getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(long timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }
    }

    public static class Agent {
        private String paradigm = "react-rag-tool";

        public String getParadigm() {
            return paradigm;
        }

        public void setParadigm(String paradigm) {
            this.paradigm = paradigm;
        }
    }

    public static class Admin {
        private String username = "admin";
        private String password = "admin";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Executor {
        @Min(1)
        private int corePoolSize = 6;
        @Min(1)
        private int maxPoolSize = 12;
        @Min(0)
        private int queueCapacity = 200;

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class RateLimit {
        private boolean enabled = true;
        @Min(1)
        private int maxRequests = 20;
        @Min(1)
        private long windowSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxRequests() {
            return maxRequests;
        }

        public void setMaxRequests(int maxRequests) {
            this.maxRequests = maxRequests;
        }

        public long getWindowSeconds() {
            return windowSeconds;
        }

        public void setWindowSeconds(long windowSeconds) {
            this.windowSeconds = windowSeconds;
        }
    }
}
