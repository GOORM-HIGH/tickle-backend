package com.profect.tickle.domain.performance.elk;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EsConfig {
    @Bean
    public ElasticsearchClient esClient(@Value("${app.es.url}") String esUrl) {
        RestClient low = RestClient.builder(HttpHost.create(esUrl)).build();
        ElasticsearchTransport transport = new RestClientTransport(low, new JacksonJsonpMapper());
        return new ElasticsearchClient(transport);
    }
}
