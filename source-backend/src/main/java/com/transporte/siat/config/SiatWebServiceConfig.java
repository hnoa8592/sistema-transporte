package com.transporte.siat.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpComponentsMessageSender;

@Configuration
@RequiredArgsConstructor
public class SiatWebServiceConfig {

    private final SiatProperties siatProperties;

    /**
     * WebServiceTemplate para el servicio de códigos (CUIS, CUFD, catálogos, eventos).
     * Usamos StringSource/StringResult directamente, sin marshaller JAXB.
     */
    @Bean(name = "siatCodesTemplate")
    public WebServiceTemplate siatCodesTemplate() {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setDefaultUri(siatProperties.getEndpointCodigos());
        template.setMessageSender(httpSender());
        return template;
    }

    /**
     * WebServiceTemplate para el servicio de facturación (emisión, anulación, reversión).
     */
    @Bean(name = "siatEmisionTemplate")
    public WebServiceTemplate siatEmisionTemplate() {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setDefaultUri(siatProperties.getEndpointFacturacion());
        template.setMessageSender(httpSender());
        return template;
    }

    private HttpComponentsMessageSender httpSender() {
        HttpComponentsMessageSender sender = new HttpComponentsMessageSender();
        sender.setConnectionTimeout(siatProperties.getConnectionTimeout());
        sender.setReadTimeout(siatProperties.getReadTimeout());
        return sender;
    }
}
