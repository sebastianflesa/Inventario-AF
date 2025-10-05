package com.function;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import com.function.service.BodegaService;
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

public class GraphQLBodegasFunction {
    private static GraphQL graphQL;

    static {
        GraphQLObjectType bodegaType = GraphQLObjectType.newObject()
            .name("Bodega")
            .field(f -> f.name("id").type(Scalars.GraphQLInt))
            .field(f -> f.name("nombre").type(Scalars.GraphQLString))
            .field(f -> f.name("direccion").type(Scalars.GraphQLString))
            .build();

        GraphQLObjectType queryType = GraphQLObjectType.newObject()
            .name("Query")
            .field(field -> field.name("bodegas")
                .type(GraphQLList.list(bodegaType))
                .dataFetcher(env -> {
                    var ctx = SpringContextSingleton.getContext();
                    BodegaService svc = ctx.getBean(BodegaService.class);
                    return svc.findAll();
                })
            )
            .build();

        GraphQLSchema schema = GraphQLSchema.newSchema().query(queryType).build();
        graphQL = GraphQL.newGraphQL(schema).build();
    }

    @FunctionName("graphqlBodegas")
    public HttpResponseMessage run(
        @HttpTrigger(name = "req", methods = {HttpMethod.POST}, route = "graphql/bodegas", authLevel = AuthorizationLevel.ANONYMOUS)
        HttpRequestMessage<Optional<Map<String,Object>>> request,
        final ExecutionContext context) {

        context.getLogger().info("graphqlBodegas invoked");
        Map<String,Object> body = request.getBody().orElse(Collections.emptyMap());
        String query = (String) body.get("query");
        if (query == null || query.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body(Map.of("error","Debe incluir campo 'query'")).build();
        }
        ExecutionResult result = graphQL.execute(query);
        return request.createResponseBuilder(HttpStatus.OK).body(result.toSpecification()).build();
    }
}
