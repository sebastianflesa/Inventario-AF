package com.function;

import java.util.HashMap;
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
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.EventGridTrigger;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.function.config.EventGridConfig;
import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.util.BinaryData;
import com.azure.messaging.eventgrid.EventGridEvent;
import com.azure.messaging.eventgrid.EventGridPublisherClient;
import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {
    private static ProductoService productoService;

    @FunctionName("createProducto")
    public HttpResponseMessage createProducto(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                route = "productos",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<com.function.models.ProductoEntity>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request to create producto.");
        com.function.models.ProductoEntity producto = request.getBody().orElse(null);

        org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
        com.function.service.ProductoService productoService = ctx.getBean(com.function.service.ProductoService.class);
        com.function.service.BodegaService bodegaService = ctx.getBean(com.function.service.BodegaService.class);

        context.getLogger().info("Producto recibido: " + producto);

        if (producto != null && producto.getBodega() == null) {
            List<com.function.models.BodegaEntity> bodegas = bodegaService.findAll();
            if (!bodegas.isEmpty()) {
                producto.setBodega(bodegas.get(0));
            }
        }

        if (producto == null || producto.getNombre() == null || producto.getBodega() == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", 400);
                    put("mensaje", "error datos invalidos");
                }})
                .build();
        }

        com.function.models.ProductoEntity saved = productoService.save(producto);
        return request.createResponseBuilder(HttpStatus.CREATED)
            .body(new java.util.HashMap<String, Object>() {{
                put("status", 201);
                put("mensaje", "producto creado correctamente");
                put("data", saved);
            }})
            .build();
       
    }

    @FunctionName("updateProducto")
    public HttpResponseMessage updateProducto(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                route = "productos/update/{id}",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<com.function.models.ProductoEntity>> request,
            @BindingName("id") Integer id,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request to update producto.");
        com.function.models.ProductoEntity producto = request.getBody().orElse(null);
        if (producto == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", 400);
                    put("mensaje", "datos de producto inválidos");
                }})
                .build();
        }
        org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
        com.function.service.ProductoService productoService = ctx.getBean(com.function.service.ProductoService.class);
        com.function.models.ProductoEntity updated = productoService.update(id, producto);
        if (updated == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", 404);
                    put("mensaje", "producto no encontrado");
                }})
                .build();
        }
        return request.createResponseBuilder(HttpStatus.OK)
            .body(new java.util.HashMap<String, Object>() {{
                put("status", 200);
                put("mensaje", "producto actualizado correctamente");
                put("data", updated);
            }})
            .build();
    }

    @FunctionName("deleteProducto")
    public HttpResponseMessage deleteProducto(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                route = "productos/delete/{id}",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            @BindingName("id") Integer id,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request to delete producto.");
        org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
        com.function.service.ProductoService productoService = ctx.getBean(com.function.service.ProductoService.class);
        boolean deleted = productoService.delete(id);
        if (!deleted) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", 404);
                    put("mensaje", "producto no encontrado");
                }})
                .build();
        }
        return request.createResponseBuilder(HttpStatus.OK)
            .body(new java.util.HashMap<String, Object>() {{
                put("status", 200);
                put("mensaje", "producto eliminado correctamente");
            }})
            .build();
    }
    @FunctionName("getAllProductos")
    public HttpResponseMessage getAllProductos(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                route = "productos",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request to get all productos.");
        org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
        com.function.service.ProductoService productoService = ctx.getBean(com.function.service.ProductoService.class);
        List<com.function.models.ProductoEntity> productos = productoService.findAll();
        return request.createResponseBuilder(HttpStatus.OK)
            .body(new java.util.HashMap<String, Object>() {{
                put("status", 200);
                put("mensaje", "lista de productos");
                put("data", productos);
            }})
            .build();
    }
    @FunctionName("getAllBodegas")
    public HttpResponseMessage getAllBodegas(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                route = "bodegas",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request to get all bodegas.");
        org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
        com.function.service.BodegaService bodegaService = ctx.getBean(com.function.service.BodegaService.class);
        List<com.function.models.BodegaEntity> bodegas = bodegaService.findAll();
        return request.createResponseBuilder(HttpStatus.OK)
            .body(new java.util.HashMap<String, Object>() {{
                put("status", 200);
                put("mensaje", "lista de bodegas");
                put("data", bodegas);
            }})
            .build();
    }

    @FunctionName("updateBodega")
    public HttpResponseMessage updateBodega(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                route = "bodegas/update/{id}",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<com.function.models.BodegaEntity>> request,
            @BindingName("id") Long id,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request to update bodega.");
        com.function.models.BodegaEntity bodega = request.getBody().orElse(null);
        if (bodega == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", 400);
                    put("mensaje", "datos de bodega invalidos");
                }})
                .build();
        }
        org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
        com.function.service.BodegaService bodegaService = ctx.getBean(com.function.service.BodegaService.class);
        com.function.models.BodegaEntity updated = bodegaService.update(id, bodega);
        if (updated == null) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", 404);
                    put("mensaje", "bodega no encontrada");
                }})
                .build();
        }
        return request.createResponseBuilder(HttpStatus.OK)
            .body(new java.util.HashMap<String, Object>() {{
                put("status", 200);
                put("mensaje", "bodega actualizada correctamente");
                put("data", updated);
            }})
            .build();
    }


    @FunctionName("deleteBodega")
    public HttpResponseMessage deleteBodega(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                route = "bodegas/delete/{id}",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            @BindingName("id") Long id,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request to delete bodega.");
        
        if (id == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", 400);
                    put("mensaje", "ID de bodega inválido");
                }})
                .build();
        }

        org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
        com.function.service.BodegaService bodegaService = ctx.getBean(com.function.service.BodegaService.class);

        // Verificar que la bodega existe
        List<com.function.models.BodegaEntity> bodegas = bodegaService.findAll();
        boolean bodegaExists = bodegas.stream().anyMatch(b -> b.getId().equals(id));
        
        if (!bodegaExists) {
            return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", 404);
                    put("mensaje", "bodega no encontrada");
                }})
                .build();
        }

        Map<String,Object> dataEv = new HashMap<>();
        dataEv.put("bodegaId", id);
        String topicEndpoint = System.getenv("EVENTGRID_TOPIC_ENDPOINT");
        String topicKey = System.getenv("EVENTGRID_KEY");
        
        if (topicEndpoint != null && topicKey != null) {
            EventGridPublisher.publish(topicEndpoint, topicKey, "BodegaEliminada", dataEv);
            context.getLogger().info("Evento BodegaEliminada publicado para bodega ID: " + id);
        } else {
            context.getLogger().warning("No se publicó evento: faltan variables de entorno EVENTGRID_TOPIC_ENDPOINT y/o EVENTGRID_KEY");
        }

        return request.createResponseBuilder(HttpStatus.OK)
            .body(new java.util.HashMap<String, Object>() {{
                put("status", 200);
                put("mensaje", "Solicitud de eliminación de bodega procesada. La eliminación se realizará de forma asíncrona.");
                put("bodegaId", id);
            }})
            .build();
    }

    @FunctionName("createBodega")
    public HttpResponseMessage createBodega(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                route = "bodegas",
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<com.function.models.BodegaEntity>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request to create bodega.");

        com.function.models.BodegaEntity bodega = request.getBody().orElse(null);
        if (bodega == null || bodega.getNombre() == null || bodega.getDireccion() == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(new java.util.HashMap<String, Object>() {{
                    put("status", 400);
                    put("mensaje", "error datos invalidos");
                }})
                .build();
        }

            org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
            com.function.service.BodegaService bodegaService = ctx.getBean(com.function.service.BodegaService.class);
            com.function.models.BodegaEntity saved = bodegaService.save(bodega);
        return request.createResponseBuilder(HttpStatus.CREATED)
            .body(new java.util.HashMap<String, Object>() {{
                put("status", 201);
                put("mensaje", "bodega creada correctamente");
                put("data", saved);
            }})
            .build();
    }

    @FunctionName("produceCreateProducto")
    public HttpResponseMessage produceCreateProducto(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.POST},
            route = "produceCreateProducto",
            authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<com.function.models.ProductoEntity>> request,
        final ExecutionContext context) {

        context.getLogger().info("Java HTTP trigger processed a request to produce create producto event.");
        String eventGridTopicEndpoint = null;
        String eventGridTopicKey = null;
        
        try {
            org.springframework.context.ApplicationContext springCtx = SpringContextSingleton.getContext();
            EventGridConfig eventGridConfig = springCtx.getBean(EventGridConfig.class);
            eventGridTopicEndpoint = eventGridConfig.getTopicEndpoint();
            eventGridTopicKey = eventGridConfig.getTopicKey();
            context.getLogger().info("Successfully retrieved EventGrid configuration from Spring context");
        } catch (Exception e) {
            context.getLogger().warning("Error retrieving EventGrid config from Spring context: " + e.getMessage());
            // Fallback to environment variables
            eventGridTopicEndpoint = System.getenv("EVENTGRID_TOPIC_ENDPOINT");
            eventGridTopicKey = System.getenv("EVENTGRID_KEY");
            context.getLogger().info("Using environment variables for EventGrid configuration");
        }

        // Validate EventGrid configuration
        if (eventGridTopicEndpoint == null || eventGridTopicEndpoint.isEmpty() || 
            eventGridTopicKey == null || eventGridTopicKey.isEmpty()) {
            context.getLogger().severe("EventGrid configuration is missing or invalid");
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", 500,
                    "mensaje", "Error de configuración de EventGrid"
                ))
                .build();
        }

        com.function.models.ProductoEntity body = request.getBody().orElse(null);

        if (body == null || body.getNombre() == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", 400,
                    "mensaje", "Cuerpo de la petición inválido o falta el nombre del producto"
                ))
                .build();
        }

        try {
            org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
            com.function.service.BodegaService bodegaService = ctx.getBean(com.function.service.BodegaService.class);

            if (body.getBodega() == null || body.getBodega().getId() == null) {
                List<com.function.models.BodegaEntity> bodegas = bodegaService.findAll();
                if (bodegas == null || bodegas.isEmpty()) {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body(Map.of(
                            "status", 400,
                            "mensaje", "No hay bodegas disponibles para asignar por defecto"
                        ))
                        .build();
                }
                body.setBodega(bodegas.get(0));
                context.getLogger().info("Asignada bodega por defecto: " + bodegas.get(0).getNombre());
            }

            EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
                .endpoint(eventGridTopicEndpoint)
                .credential(new AzureKeyCredential(eventGridTopicKey))
                .buildEventGridEventPublisherClient();

            String subject = "/EventGridEvents/producto";

            EventGridEvent event = new EventGridEvent(
                subject,
                "Producto.Created",
                BinaryData.fromObject(body),
                "1.0"
            );

            client.sendEvent(event);
            context.getLogger().info("Evento Producto.Created enviado exitosamente a EventGrid");

            return request.createResponseBuilder(HttpStatus.CREATED)
                .body(Map.of(
                    "status", 201,
                    "mensaje", "Evento de creación de producto enviado exitosamente",
                    "subject", subject,
                    "eventType", "Producto.Created",
                    "producto", body
                ))
                .build();

        } catch (Exception e) {
            context.getLogger().severe("Error enviando evento a EventGrid: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", 500,
                    "mensaje", "Error interno del servidor al procesar evento",
                    "error", e.getMessage()
                ))
                .build();
        }
    }

    @FunctionName("consumerCreateProducto")
    public void consumerCreateProducto(
        @EventGridTrigger(name = "event") String content,
        final ExecutionContext ctx
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> event = mapper.readValue(content, new TypeReference<Map<String,Object>>(){});
            Object data = event.get("data");
            com.function.models.ProductoEntity producto = mapper.convertValue(data, com.function.models.ProductoEntity.class);

            var svc = SpringContextSingleton.getContext().getBean(com.function.service.ProductoService.class);
            var saved = svc.save(producto);
            ctx.getLogger().info("Producto guardado: " + saved);
        } catch (Exception e) {
            ctx.getLogger().severe("Error en consumerCreateProducto: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @FunctionName("produceDeleteBodega")
    public HttpResponseMessage produceDeleteBodega(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.POST},
            route = "produceDeleteBodega/{id}",
            authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<String>> request,
        @BindingName("id") Long id,
        final ExecutionContext context) {
        try {
            context.getLogger().info("Java HTTP trigger processed a request to produce delete bodega event.");
            
            String eventGridTopicEndpoint = null;
            String eventGridTopicKey = null;
            
            try {
                org.springframework.context.ApplicationContext springCtx = SpringContextSingleton.getContext();
                EventGridConfig eventGridConfig = springCtx.getBean(EventGridConfig.class);
                eventGridTopicEndpoint = eventGridConfig.getTopicEndpoint();
                eventGridTopicKey = eventGridConfig.getTopicKey();
                context.getLogger().info("Successfully retrieved EventGrid configuration from Spring context");
            } catch (Exception e) {
                context.getLogger().warning("Error retrieving EventGrid config from Spring context: " + e.getMessage());
                // Fallback to environment variables
                eventGridTopicEndpoint = System.getenv("EVENTGRID_TOPIC_ENDPOINT");
                eventGridTopicKey = System.getenv("EVENTGRID_KEY");
                context.getLogger().info("Using environment variables for EventGrid configuration");
            }

            if (id == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                        "status", 400,
                        "mensaje", "ID de bodega invallido"
                    ))
                    .build();
            }

            org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
            com.function.service.BodegaService bodegaService = ctx.getBean(com.function.service.BodegaService.class);

            List<com.function.models.BodegaEntity> bodegas = bodegaService.findAll();
            boolean bodegaExists = bodegas.stream().anyMatch(b -> b.getId().equals(id));

            if (!bodegaExists) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                        "status", 404,
                        "mensaje", "Bodega no encontrada"
                    ))
                    .build();
            }

            EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
                .endpoint(eventGridTopicEndpoint)
                .credential(new AzureKeyCredential(eventGridTopicKey))
                .buildEventGridEventPublisherClient();

            String subject = "/EventGridEvents/bodega";

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("bodegaId", id);

            EventGridEvent event = new EventGridEvent(
                subject,
                "Bodega.Deleted",
                BinaryData.fromObject(eventData),
                "1.0"
            );

            client.sendEvent(event);

            return request.createResponseBuilder(HttpStatus.OK)
                .body(Map.of(
                    "status", 200,
                    "mensaje", "Evento de eliminación de bodega enviado a Event Grid",
                    "subject", subject,
                    "eventType", "Bodega.Deleted",
                    "bodegaId", id
                ))
                .build();
        } catch (Exception e) {
            context.getLogger().severe("Error en produceDeleteBodega: " + e.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "status", 500,
                    "mensaje", "Error al enviar evento de eliminación de bodega",
                    "error", e.getMessage()
                ))
                .build();
        }
    }

    @FunctionName("consumerDeleteBodega")
    public void consumerDeleteBodega(
        @EventGridTrigger(name = "event") String content,
        final ExecutionContext ctx
    ) {
        try {
            ctx.getLogger().info("Consumer delete bodega triggered with content: " + content);
            
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> event = mapper.readValue(content, new TypeReference<Map<String,Object>>(){});
            Object data = event.get("data");
            
            Map<String, Object> eventData = mapper.convertValue(data, new TypeReference<Map<String,Object>>(){});
            Object bodegaIdObj = eventData.get("bodegaId");
            
            final Long bodegaId;
            if (bodegaIdObj instanceof Integer) {
                bodegaId = ((Integer) bodegaIdObj).longValue();
            } else if (bodegaIdObj instanceof Long) {
                bodegaId = (Long) bodegaIdObj;
            } else if (bodegaIdObj instanceof Number) {
                bodegaId = ((Number) bodegaIdObj).longValue();
            } else {
                bodegaId = null;
            }

            if (bodegaId == null) {
                ctx.getLogger().warning("No se pudo obtener bodegaId del evento");
                return;
            }

            ctx.getLogger().info("Eliminando bodega con ID: " + bodegaId);

            var springCtx = SpringContextSingleton.getContext();
            var productoService = springCtx.getBean(com.function.service.ProductoService.class);
            var bodegaService = springCtx.getBean(com.function.service.BodegaService.class);

            List<com.function.models.ProductoEntity> productosAsociados = productoService.findAll()
                .stream()
                .filter(p -> p.getBodega() != null && p.getBodega().getId().equals(bodegaId))
                .toList();

            ctx.getLogger().info("Encontrados " + productosAsociados.size() + " productos asociados a la bodega " + bodegaId);

            for (com.function.models.ProductoEntity producto : productosAsociados) {
                productoService.delete(producto.getId());
                ctx.getLogger().info("Producto eliminado: " + producto.getId() + " - " + producto.getNombre());
            }

            boolean bodegaDeleted = bodegaService.delete(bodegaId);
            if (bodegaDeleted) {
                ctx.getLogger().info("Bodega eliminada correctamente: " + bodegaId);
            } else {
                ctx.getLogger().warning("No se pudo eliminar la bodega: " + bodegaId);
            }

        } catch (Exception e) {
            ctx.getLogger().severe("Error en consumerDeleteBodega: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


}
