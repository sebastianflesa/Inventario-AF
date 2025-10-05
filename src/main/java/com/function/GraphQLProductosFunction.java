package com.function;

import java.util.Collections;
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
 * GraphQL endpoint para Productos
 */
public class GraphQLProductosFunction {
    private static GraphQL graphQL;

    static {
        GraphQLObjectType productoType = GraphQLObjectType.newObject()
            .name("Producto")
            .field(f -> f.name("id").type(Scalars.GraphQLInt))
            .field(f -> f.name("nombre").type(Scalars.GraphQLString))
            .field(f -> f.name("stock").type(Scalars.GraphQLInt))
            .field(f -> f.name("precio").type(Scalars.GraphQLInt))
            .build();

        GraphQLObjectType queryType = GraphQLObjectType.newObject()
            .name("Query")
            .field(field -> field.name("productos")
                .type(GraphQLList.list(productoType))
                .dataFetcher(env -> {
                    var ctx = SpringContextSingleton.getContext();
                    ProductoService svc = ctx.getBean(ProductoService.class);
                    return svc.findAll();
                })
            )
            .field(field -> field.name("productoById")
                .type(productoType)
                .argument(arg -> arg.name("id").type(Scalars.GraphQLInt))
                .dataFetcher(env -> {
                    Integer id = env.getArgument("id");
                    var ctx = SpringContextSingleton.getContext();
                    ProductoService svc = ctx.getBean(ProductoService.class);
                    return svc.findAll().stream().filter(p -> p.getId().equals(id)).findFirst().orElse(null);
                })
            )
            .build();

        GraphQLSchema schema = GraphQLSchema.newSchema().query(queryType).build();
        graphQL = GraphQL.newGraphQL(schema).build();
    }

    @FunctionName("graphqlProductos")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, route = "graphql/productos", authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<Map<String,Object>>> request,
        final ExecutionContext context) {

        context.getLogger().info("graphqlProductos invoked");
        Map<String,Object> body = request.getBody().orElse(Collections.emptyMap());
        String query = (String) body.get("query");
        if (query == null || query.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(Map.of("error","Debe incluir campo 'query'")).build();
        }
        ExecutionResult result = graphQL.execute(query);
        return request.createResponseBuilder(HttpStatus.OK).body(result.toSpecification()).build();
    }
}
