package api.assignment.backend.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class ElasticsearchConfig {

    @Value("${elasticsearch.url:http://localhost:9200}")
    private String esUrl;

    @Value("${elasticsearch.apikey:}")
    private String apiKey;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            var httpHost = HttpHost.create(esUrl);

            var restClientBuilder = RestClient.builder(httpHost);

            if (apiKey != null && !apiKey.isBlank()) {
                restClientBuilder.setDefaultHeaders(new Header[]{
                        new BasicHeader("Authorization", "ApiKey " + apiKey)
                });
            }

            var restClient = restClientBuilder.build();
            var transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

            log.info("Elasticsearch client configured for: {}", esUrl);
            return new ElasticsearchClient(transport);
        } catch (Exception e) {
            log.warn("Failed to configure Elasticsearch client: {}. Search will use PostgreSQL fallback.", e.getMessage());
            return null;
        }
    }
}
