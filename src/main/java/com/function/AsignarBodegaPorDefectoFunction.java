package com.function;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.function.models.BodegaEntity;
import com.function.models.ProductoEntity;
import com.function.service.BodegaService;
import com.function.service.ProductoService;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

/**
 * Listener de eventos (Event Grid) implementado como HttpTrigger:
 * El Event Grid reenvía POST con 'events' array; aquí lo procesamos.
 */
public class AsignarBodegaPorDefectoFunction {

    @FunctionName("asignarBodegaPorDefecto")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, route = "events/productoCreado", authLevel = AuthorizationLevel.FUNCTION)
        HttpRequestMessage<Optional<List<Map<String,Object>>>> request,
        final ExecutionContext context) {

        context.getLogger().info("AsignarBodegaPorDefecto invoked");
        List<Map<String,Object>> events = request.getBody().orElse(Collections.emptyList());
        var ctx = SpringContextSingleton.getContext();
        ProductoService productoService = ctx.getBean(ProductoService.class);
        BodegaService bodegaService = ctx.getBean(BodegaService.class);

        // find or create default bodega
        List<BodegaEntity> bodegas = bodegaService.findAll();
        BodegaEntity defaultB = bodegas.stream().filter(b -> "DEFAULT".equalsIgnoreCase(b.getNombre())).findFirst().orElse(null);
        if (defaultB == null) {
            defaultB = new BodegaEntity(null, "DEFAULT", "Automatica");
            defaultB = bodegaService.save(defaultB);
        }

        for (Map<String,Object> ev : events) {
            Map<String,Object> data = (Map<String,Object>) ev.get("data");
            Object pidObj = data.get("productoId");
            final Integer pid;
            if (pidObj instanceof Integer) pid = (Integer) pidObj;
            else if (pidObj instanceof Number) pid = ((Number)pidObj).intValue();
            else pid = null;
            if (pid != null) {
                ProductoEntity p = new ProductoEntity();
                p.setId(pid);
                p.setBodega(defaultB);
                productoService.save(p);
                // better: load existing and update bodega; method below demonstrates approach
                ProductoEntity existing = productoService.findAll().stream().filter(x -> x.getId().equals(pid)).findFirst().orElse(null);
                if (existing != null) {
                    existing.setBodega(defaultB);
                    productoService.save(existing);
                }
            }
        }

        return request.createResponseBuilder(HttpStatus.OK).body(Map.of("status","processed")).build();
    }
}
