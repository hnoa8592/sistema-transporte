package com.transporte.siat.service;

import com.transporte.siat.client.SiatCodesClient;
import com.transporte.siat.client.SiatSoapResponse;
import com.transporte.siat.dto.SiatCatalogoResponse;
import com.transporte.siat.entity.SiatCatalogo;
import com.transporte.siat.entity.SiatConfig;
import com.transporte.siat.entity.SiatCuis;
import com.transporte.siat.enums.SiatTipoCatalogo;
import com.transporte.siat.mapper.SiatCatalogoMapper;
import com.transporte.siat.repository.SiatCatalogoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sincroniza catálogos del SIN con la base de datos local.
 * Los catálogos son necesarios para validar códigos en la emisión de facturas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiatCatalogService {

    private final SiatCodesClient codesClient;
    private final SiatCatalogoRepository catalogoRepository;
    private final SiatCatalogoMapper catalogoMapper;
    private final SiatCodesService codesService;

    @Cacheable(value = "siat-catalogos", key = "#tipo")
    public List<SiatCatalogoResponse> findByTipo(SiatTipoCatalogo tipo) {
        return catalogoRepository.findByTipoCatalogoAndVigenteTrue(tipo)
                .stream().map(catalogoMapper::toResponse).toList();
    }

    /**
     * Sincroniza todos los catálogos principales desde el SIN.
     */
    @Transactional
    @CacheEvict(value = "siat-catalogos", allEntries = true)
    public Map<String, Integer> sincronizarTodos(SiatConfig config) {
        SiatCuis cuis = codesService.getCuisVigente(
                config.getId(), config.getCodigoSucursal(), config.getCodigoPuntoVenta());

        int totalActualizados = 0;

        totalActualizados += sincronizarCatalogo(config, cuis,
                SiatTipoCatalogo.ACTIVIDAD_ECONOMICA,
                codesClient.sincronizarActividades(config.getNit(), config.getCodigoSistema(),
                        cuis.getCuis(), config.getCodigoSucursal(), config.getCodigoPuntoVenta()),
                "listaCodigos");

        totalActualizados += sincronizarCatalogo(config, cuis,
                SiatTipoCatalogo.PRODUCTO_SERVICIO,
                codesClient.sincronizarProductosServicios(config.getNit(), config.getCodigoSistema(),
                        cuis.getCuis(), config.getCodigoSucursal(), config.getCodigoPuntoVenta()),
                "listaCodigos");

        totalActualizados += sincronizarCatalogo(config, cuis,
                SiatTipoCatalogo.LEYENDA_FACTURA,
                codesClient.sincronizarLeyendas(config.getNit(), config.getCodigoSistema(),
                        cuis.getCuis(), config.getCodigoSucursal(), config.getCodigoPuntoVenta()),
                "listaCodigos");

        totalActualizados += sincronizarCatalogo(config, cuis,
                SiatTipoCatalogo.TIPO_DOCUMENTO_IDENTIDAD,
                codesClient.sincronizarTiposDocumentoIdentidad(config.getNit(), config.getCodigoSistema(),
                        cuis.getCuis(), config.getCodigoSucursal(), config.getCodigoPuntoVenta()),
                "listaCodigos");

        totalActualizados += sincronizarCatalogo(config, cuis,
                SiatTipoCatalogo.METODO_PAGO,
                codesClient.sincronizarMetodosPago(config.getNit(), config.getCodigoSistema(),
                        cuis.getCuis(), config.getCodigoSucursal(), config.getCodigoPuntoVenta()),
                "listaCodigos");

        totalActualizados += sincronizarCatalogo(config, cuis,
                SiatTipoCatalogo.MONEDA,
                codesClient.sincronizarMonedas(config.getNit(), config.getCodigoSistema(),
                        cuis.getCuis(), config.getCodigoSucursal(), config.getCodigoPuntoVenta()),
                "listaCodigos");

        totalActualizados += sincronizarCatalogo(config, cuis,
                SiatTipoCatalogo.UNIDAD_MEDIDA,
                codesClient.sincronizarUnidadesMedida(config.getNit(), config.getCodigoSistema(),
                        cuis.getCuis(), config.getCodigoSucursal(), config.getCodigoPuntoVenta()),
                "listaCodigos");

        log.info("Sincronización completada: {} registros actualizados", totalActualizados);
        return Map.of("totalActualizados", totalActualizados);
    }

    private int sincronizarCatalogo(SiatConfig config, SiatCuis cuis,
                                     SiatTipoCatalogo tipo, SiatSoapResponse response,
                                     String listTag) {
        if (!response.isExitoso()) {
            log.warn("No se pudo sincronizar catálogo {}: {}", tipo, response.getMensaje());
            return 0;
        }

        List<SiatCatalogo> items = parseListaCodigos(response, tipo, config.getTenantId());
        if (items.isEmpty()) return 0;

        // Desactivar items anteriores de este tenant
        catalogoRepository.findByTipoCatalogoAndVigenteTrue(tipo)
                .forEach(c -> { c.setVigente(false); catalogoRepository.save(c); });

        catalogoRepository.saveAll(items);
        log.info("Catálogo {} sincronizado: {} items", tipo, items.size());
        return items.size();
    }

    private List<SiatCatalogo> parseListaCodigos(SiatSoapResponse response,
                                                   SiatTipoCatalogo tipo, String tenantId) {
        List<SiatCatalogo> result = new ArrayList<>();
        // La respuesta viene en datos como XML raw; intentamos extraerla
        String rawXml = response.getDatos().getOrDefault("rawXml", "");
        if (rawXml.isBlank()) return result;

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(rawXml.getBytes(StandardCharsets.UTF_8)));

            NodeList items = doc.getElementsByTagNameNS("*", "listaCodigos");
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String codigo = getChildText(item, "codigoCatalogo");
                String descripcion = getChildText(item, "descripcion");
                if (codigo != null && descripcion != null) {
                    SiatCatalogo catalogo = SiatCatalogo.builder()
                            .tipoCatalogo(tipo)
                            .codigo(codigo)
                            .descripcion(descripcion)
                            .vigente(true)
                            .build();
                    catalogo.setTenantId(tenantId);
                    result.add(catalogo);
                }
            }
        } catch (Exception e) {
            log.error("Error parseando catálogo {}: {}", tipo, e.getMessage());
        }
        return result;
    }

    private String getChildText(Element parent, String tagName) {
        NodeList list = parent.getElementsByTagNameNS("*", tagName);
        if (list.getLength() > 0) return list.item(0).getTextContent().trim();
        return null;
    }
}
