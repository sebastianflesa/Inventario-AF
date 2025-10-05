package com.function;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
 * Listener para evento BodegaEliminada
 */
public class ActualizarProductosAlEliminarBodegaFunction {

    @FunctionName("actualizarProductosAlEliminarBodega")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, route = "events/bodegaEliminada", authLevel = AuthorizationLevel.FUNCTION)
        HttpRequestMessage<Optional<List<Map<String,Object>>>> request,
        final ExecutionContext context) {

        context.getLogger().info("actualizarProductosAlEliminarBodega invoked");
        List<Map<String,Object>> events = request.getBody().orElse(Collections.emptyList());
        var ctx = SpringContextSingleton.getContext();
        ProductoService productoService = ctx.getBean(ProductoService.class);

        for (Map<String,Object> ev : events) {
            Map<String,Object> data = (Map<String,Object>) ev.get("data");
            Object bidObj = data.get("bodegaId");
            Long bid = null;
            if (bidObj instanceof Integer) bid = ((Integer)bidObj).longValue();
            else if (bidObj instanceof Number) bid = ((Number)bidObj).longValue();
            if (bid != null) {
                final Long finalBid = bid;
                productoService.findAll().stream()
                    .filter(p -> p.getBodega() != null && p.getBodega().getId().equals(finalBid))
                    .forEach(p -> {
                        p.setBodega(null);
                        productoService.save(p);
                    });
            }
        }
        return request.createResponseBuilder(HttpStatus.OK).body(Map.of("status","processed")).build();
    }
}
