package org.example.comp3820.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FhirConfig {

    @Value("${fhir.server.base-url}")
    private String fhirServerBaseUrl;

    @Bean
    public FhirContext fhirContext() {
        return FhirContext.forR4(); // 假定使用 FHIR R4 标准
    }

    @Bean
    public IGenericClient fhirClient(FhirContext fhirContext) {
        IGenericClient client = fhirContext.newRestfulGenericClient(fhirServerBaseUrl);
        // 在实际 SMART-on-FHIR 中，这里需要添加拦截器来注入 Bearer Token
        return client;
    }
}