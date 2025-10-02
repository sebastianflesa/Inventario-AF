    package com.function;

    import com.microsoft.azure.functions.*;
    import com.microsoft.azure.functions.annotation.*;
    import java.util.Optional;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    import org.springframework.context.ApplicationContext;

    import com.azure.core.credential.AzureKeyCredential;
    import com.azure.core.util.BinaryData;
    import com.azure.messaging.eventgrid.EventGridEvent;
    import com.azure.messaging.eventgrid.EventGridPublisherClient;
    import com.azure.messaging.eventgrid.EventGridPublisherClientBuilder;
    import com.fasterxml.jackson.databind.ObjectMapper;
    import com.fasterxml.jackson.core.type.TypeReference;
    import com.function.models.BodegaEntity;
    import com.function.models.BodegaRepository;
    import com.function.service.ProductoService;
    import com.function.config.EventGridConfig;

    import graphql.ExecutionResult;
    import graphql.GraphQL;
    import graphql.Scalars;
    import graphql.schema.GraphQLList;
    import graphql.schema.GraphQLObjectType;
    import graphql.schema.GraphQLSchema;

    /**
     * Azure Functions with HTTP Trigger.
     */
    public class Function {
        private static GraphQL graphQL;
        private static ProductoService productoService;
        private static EventGridConfig eventGridConfig;
        static {
            org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
            productoService = ctx.getBean(ProductoService.class);
            eventGridConfig = ctx.getBean(EventGridConfig.class);
                GraphQLObjectType bodegaType = GraphQLObjectType.newObject()
                    .name("Bodega")
                    .field(field -> field.name("id").type(Scalars.GraphQLInt))
                    .field(field -> field.name("nombre").type(Scalars.GraphQLString))
                    .field(field -> field.name("direccion").type(Scalars.GraphQLString))
                    .build();

                com.function.service.BodegaService bodegaService = ctx.getBean(com.function.service.BodegaService.class);

            GraphQLObjectType productoType = GraphQLObjectType.newObject()
                .name("Producto")
                .field(field -> field.name("id").type(Scalars.GraphQLInt))
                .field(field -> field.name("nombre").type(Scalars.GraphQLString))
                .field(field -> field.name("stock").type(Scalars.GraphQLInt))
                .field(field -> field.name("precio").type(Scalars.GraphQLInt))
                .field(field -> field.name("bodega").type(bodegaType)
                    .dataFetcher(env -> {
                        com.function.models.ProductoEntity producto = env.getSource();
                        return producto.getBodega();
                    })
                )
                .build();

            
            GraphQLObjectType queryType = GraphQLObjectType.newObject()
                .name("Query")
                .field(field -> field
                    .name("productos")
                    .type(GraphQLList.list(productoType))
                    .dataFetcher(env -> {
                        return productoService.findAll();
                    })
                )
                    .field(field -> field
                        .name("bodegas")
                        .type(GraphQLList.list(bodegaType))
                        .dataFetcher(env -> {
                            return bodegaService.findAll();
                        })
                    )
                .build();

            GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();

            graphQL = GraphQL.newGraphQL(schema).build();
        }

        @FunctionName("getAllProductosGQ")
        public HttpResponseMessage graphqlProductosEndpoint(
                @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "graphql/productos") 
                HttpRequestMessage<Optional<Map<String, Object>>> request,
                final ExecutionContext context) {

            context.getLogger().info("Procesando query GraphQL de productos...");

            Map<String, Object> body = request.getBody().orElse(new HashMap<>());
            String query = (String) body.get("query");

            if (query == null || query.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Falta la query en el body"))
                    .build();
            }

            if (!query.contains("productos")) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Solo se permiten consultas de productos en este endpoint"))
                    .build();
            }

            ExecutionResult executionResult = graphQL.execute(query);

            return request.createResponseBuilder(HttpStatus.OK)
                .body(executionResult.toSpecification())
                .build();
        }

        @FunctionName("getAllBodegasGQ")
        public HttpResponseMessage graphqlBodegasEndpoint(
                @HttpTrigger(
                    name = "req",
                    methods = {HttpMethod.POST},
                    authLevel = AuthorizationLevel.ANONYMOUS,
                    route = "graphql/bodegas") 
                HttpRequestMessage<Optional<Map<String, Object>>> request,
                final ExecutionContext context) {

            context.getLogger().info("Procesando query GraphQL de bodegas...");

            Map<String, Object> body = request.getBody().orElse(new HashMap<>());
            String query = (String) body.get("query");

            if (query == null || query.isEmpty()) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Falta la query en el body"))
                    .build();
            }

            if (!query.contains("bodegas")) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Solo se permiten consultas de bodegas en este endpoint"))
                    .build();
            }

            ExecutionResult executionResult = graphQL.execute(query);

            return request.createResponseBuilder(HttpStatus.OK)
                .body(executionResult.toSpecification())
                .build();
        }

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
            if (producto == null || producto.getNombre() == null || producto.getBodega() == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body(new java.util.HashMap<String, Object>() {{
                        put("status", 400);
                        put("mensaje", "error datos invalidos");
                    }})
                    .build();
            }
            org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
            com.function.service.ProductoService productoService = ctx.getBean(com.function.service.ProductoService.class);
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
            org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
            com.function.service.BodegaService bodegaService = ctx.getBean(com.function.service.BodegaService.class);
            boolean deleted = bodegaService.delete(id);
            if (!deleted) {
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
                    put("mensaje", "bodega eliminada correctamente");
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

        @FunctionName("GenerateCreateBodega")
        public HttpResponseMessage GenerateCreateBodega(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, route = "generatebodega", authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Optional<com.function.models.BodegaEntity>> request,
            final ExecutionContext context) {

            String eventGridTopicEndpoString = eventGridConfig.getTopicEndpoint();
            String eventGridTopicKey = eventGridConfig.getTopicKey();

            BodegaEntity bodegaData = request.getBody()
                .orElse(new BodegaEntity(null, "Bodega en durazno", "Calle Falsa 123"));

            EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
                .endpoint(eventGridTopicEndpoString)
                .credential(new AzureKeyCredential(eventGridTopicKey))
                .buildEventGridEventPublisherClient();

            EventGridEvent event = new EventGridEvent(
                "/EventGridEvents/bodega",
                "Bodega.Created",
                BinaryData.fromObject(bodegaData),
                "1.0"
            );

            client.sendEvent(event);

            return request.createResponseBuilder(HttpStatus.CREATED)
                .body(Map.of("status", 201, "mensaje", "evento enviado a Event Grid"))
                .build();
        }
        

    @FunctionName("GenerateUpdateBodega")
    public HttpResponseMessage GenerateUpdateBodega(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, route = "generatebodega/update/{id}", authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<com.function.models.BodegaEntity>> request,
        @BindingName("id") Long id,
        final ExecutionContext context) {

        String eventGridTopicEndpoString = eventGridConfig.getTopicEndpoint();
        String eventGridTopicKey = eventGridConfig.getTopicKey();
        BodegaEntity bodegaData = request.getBody().orElse(
            new BodegaEntity(id, "Bodega Actualizada", "Direccion actualizada 321")
        );
        bodegaData.setId(id);
        EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
            .endpoint(eventGridTopicEndpoString)
            .credential(new AzureKeyCredential(eventGridTopicKey))
            .buildEventGridEventPublisherClient();

        EventGridEvent event = new EventGridEvent(
            "/EventGridEvents/bodega",
            "Bodega.Updated",
            BinaryData.fromObject(bodegaData),
            "1.0"
        );

        client.sendEvent(event);

        return request.createResponseBuilder(HttpStatus.OK)
            .body(Map.of("status", 200, "mensaje", "evento de update enviado a Event Grid"))
            .build();
    }

    @FunctionName("consumerUpdateBodega")
    public void consumerUpdateBodega(
        @EventGridTrigger(name = "event") String content,
        final ExecutionContext ctx
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> event = mapper.readValue(content, new TypeReference<Map<String,Object>>(){});
            Object data = event.get("data");
            BodegaEntity bodega = mapper.convertValue(data, BodegaEntity.class);

            var svc = SpringContextSingleton.getContext().getBean(com.function.service.BodegaService.class);
            var updated = svc.update(bodega.getId(), bodega);
            if (updated == null) {
                ctx.getLogger().warning("Bodega no encontrada para actualizar, id=" + bodega.getId());
            } else {
                ctx.getLogger().info("Bodega actualizada: " + updated);
            }
        } catch (Exception e) {
            ctx.getLogger().severe("Error en consumerUpdateBodega: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @FunctionName("GenerateCreateProducto")
    public HttpResponseMessage GenerateCreateProducto(
        @HttpTrigger(
            name = "req",
            methods = {HttpMethod.POST},
            route = "generateproducto",
            authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<com.function.models.ProductoEntity>> request,
        final ExecutionContext context) {
        String eventGridTopicEndpoString = eventGridConfig.getTopicEndpoint();
        String eventGridTopicKey = eventGridConfig.getTopicKey();

        // {
        //   "nombre":"PRODUCTO DE TEST",
        //   "stock":100,
        //   "precio":10000,
        //   "bodega":{"id":33}
        // }

        com.function.models.ProductoEntity body = request.getBody().orElse(null);
        if (body == null
            || body.getNombre() == null
            || body.getBodega() == null
            || body.getBodega().getId() == null) {

            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", 400,
                    "mensaje", "Body invalido"
                ))
                .build();
        }

        if (body.getPrecio() == null || body.getPrecio() < 0
            || body.getStock() == null || body.getStock() < 0) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                    "status", 400,
                    "mensaje", "Precio stock invalido"
                ))
                .build();
        }

        EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
            .endpoint(eventGridTopicEndpoString)
            .credential(new AzureKeyCredential(eventGridTopicKey))
            .buildEventGridEventPublisherClient();

        String subject = "/EventGridEvents/bodega";

        EventGridEvent event = new EventGridEvent(
            subject,
            "Producto.Created",
            BinaryData.fromObject(body),
            "1.0"
        );

        client.sendEvent(event);

        return request.createResponseBuilder(HttpStatus.CREATED)
            .body(Map.of(
                "status", 201,
                "mensaje", "evento de create enviado a Event Grid",
                "subject", subject,
                "eventType", "Producto.Created"
            ))
            .build();
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

    @FunctionName("consumerCreateBodega")
    public void consumerCreateBodega(
            @EventGridTrigger(name = "event") String content,
            final ExecutionContext ctx
        ) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> event = mapper.readValue(content, new TypeReference<Map<String,Object>>(){});
                Object data = event.get("data");
                BodegaEntity bodega = mapper.convertValue(data, BodegaEntity.class);

                var svc = SpringContextSingleton.getContext().getBean(com.function.service.BodegaService.class);
                var saved = svc.save(bodega);
                ctx.getLogger().info("Bodega guardada: " + saved);
            } catch (Exception e) {
                ctx.getLogger().severe("Error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }


    @FunctionName("GenerateUpdateProducto")
    public HttpResponseMessage GenerateUpdateProducto(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, route = "generateproducto/update/{id}", authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<com.function.models.ProductoEntity>> request,
        @BindingName("id") Integer id,
        final ExecutionContext context) {

        String eventGridTopicEndpoString = eventGridConfig.getTopicEndpoint();
        String eventGridTopicKey = eventGridConfig.getTopicKey();

        com.function.models.ProductoEntity productoData = request.getBody().orElseGet(() -> {
            com.function.models.ProductoEntity p = new com.function.models.ProductoEntity();
            p.setId(id);
            p.setNombre("Producto Actualizado");
            p.setPrecio(12990);
            p.setStock(50);
            return p;
        });
        productoData.setId(id);

        EventGridPublisherClient<EventGridEvent> client = new EventGridPublisherClientBuilder()
            .endpoint(eventGridTopicEndpoString)
            .credential(new AzureKeyCredential(eventGridTopicKey))
            .buildEventGridEventPublisherClient();

        EventGridEvent event = new EventGridEvent(
            "/EventGridEvents/bodega",
            "Producto.Updated",
            BinaryData.fromObject(productoData),
            "1.0"
        );

        client.sendEvent(event);

        return request.createResponseBuilder(HttpStatus.OK)
            .body(Map.of("status", 200, "mensaje", "evento de update enviado a Event Grid"))
            .build();
    }

    @FunctionName("consumerUpdateProducto")
    public void consumerUpdateProducto(
        @EventGridTrigger(name = "event") String content,
        final ExecutionContext ctx
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> event = mapper.readValue(content, new TypeReference<Map<String,Object>>(){});
            Object data = event.get("data");
            com.function.models.ProductoEntity producto = mapper.convertValue(data, com.function.models.ProductoEntity.class);

            var svc = SpringContextSingleton.getContext().getBean(com.function.service.ProductoService.class);
            var updated = svc.update(producto.getId(), producto);
            if (updated == null) {
                ctx.getLogger().warning("Producto no encontrado para actualizar, id=" + producto.getId());
            } else {
                ctx.getLogger().info("Producto actualizado: " + updated);
            }
        } catch (Exception e) {
            ctx.getLogger().severe("Error en consumerUpdateProducto: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }


}
