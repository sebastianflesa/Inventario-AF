package com.function;

import java.util.HashMap;
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

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

/**
 * Azure Function para exponer un endpoint GraphQL
 */
public class GraphQLFunction {

    private static GraphQL graphQL;

    static {
        // Definir esquema GraphQL
        GraphQLObjectType productoType = GraphQLObjectType.newObject()
            .name("Producto")
            .field(field -> field.name("id").type(Scalars.GraphQLInt))
            .field(field -> field.name("nombre").type(Scalars.GraphQLString))
            .field(field -> field.name("stock").type(Scalars.GraphQLInt))
            .field(field -> field.name("precio").type(Scalars.GraphQLInt))
            .build();

        GraphQLObjectType queryType = GraphQLObjectType.newObject()
            .name("Query")
            .field(field -> field
                .name("productos")
                .type(GraphQLList.list(productoType))
                .dataFetcher(env -> {
                    org.springframework.context.ApplicationContext ctx = SpringContextSingleton.getContext();
                    ProductoService productoService = ctx.getBean(ProductoService.class);
                    return productoService.findAll();
                })
            )
            .build();

        GraphQLSchema schema = GraphQLSchema.newSchema()
            .query(queryType)
            .build();

        graphQL = GraphQL.newGraphQL(schema).build();
    }

    @FunctionName("graphqlEndpoint")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "graphql") 
            HttpRequestMessage<Optional<Map<String, Object>>> request,
            final ExecutionContext context) {

        context.getLogger().info("Procesando query GraphQL...");

        Map<String, Object> body = request.getBody().orElse(new HashMap<>());
        String query = (String) body.get("query");

        if (query == null || query.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "Falta la query en el body"))
                .build();
        }

        ExecutionResult executionResult = graphQL.execute(query);

        return request.createResponseBuilder(HttpStatus.OK)
            .body(executionResult.toSpecification())
            .build();
    }
}
