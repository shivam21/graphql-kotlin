---
id: spring-beans
title: Automatically Created Beans
---

`graphql-kotlin-spring-server` automatically creates all the necessary beans to start GraphQL web server. Many of the beans are conditionally created and default behavior
can be customized by providing custom beans in your application context. See sections below for the information about all automatically created beans.

## Execution
| Bean                             | Description |
|:---------------------------------|:------------|
| ContextWebFilter                 | Default web filter that populates GraphQL context in the reactor subscriber context. |
| DataFetcherExceptionHandler      | GraphQL exception handler used from the various execution strategies, defaults to [KotlinDataFetcherExceptionHandler](https://github.com/ExpediaGroup/graphql-kotlin/blob/master/graphql-kotlin-spring-server/src/main/kotlin/com/expediagroup/graphql/spring/exception/KotlinDataFetcherExceptionHandler.kt). |
| DataLoaderRegistryFactory        | Factory used to create DataLoaderRegistry instance per query execution. See [graphql-java documentation](https://www.graphql-java.com/documentation/v14/batching/) for more details. |
| GraphQLContextFactory            | Factory used by context WebFilter to generate GraphQL context based on the incoming request. GraphQL context can be any object. Defaults to empty [GraphQLContext](./spring-graphql-context.md). |
| KotlinDataFetcherFactoryProvider | Factory used during schema construction to obtain `DataFetcherFactory` that should be used for target function (using Spring aware `SpringDataFetcher`) and property resolution. |


## Non-Federated Schema
_Created only if federation is disabled_

| Bean                             | Description |
|:---------------------------------|:------------|
| SchemaGeneratorConfig          | Schema generator configuration information, see [Schema Generator Configuration](../schema-generator/customizing-schemas/generator-config.md) for details. Can be customized by providing `TopLevelNames`, [SchemaGeneratorHooks](../schema-generator/customizing-schemas/generator-config.md) and `KotlinDataFetcherFactoryProvider` beans. |
| GraphQLSchema                  | GraphQL schema generated based on the schema generator configuration and  `Query`, `Mutation` and `Subscription` objects available in the application context. |


## Federated Schema
_Created only if federation is enabled_

| Bean                             | Description |
|:---------------------------------|:------------|
| FederatedTypeResolvers         | List of `FederatedTypeResolvers` marked as beans that should be added to hooks. See [Federated Type Resolution](../federated/type-resolution.md) for more details |
| FederatedSchemaGeneratorHooks  | Schema generator hooks used to build federated schema |
| FederatedSchemaGeneratorConfig | Federated schema generator configuration information. You can customize the configuration by providing `TopLevelNames`, `FederatedSchemaGeneratorHooks` and `KotlinDataFetcherFactoryProvider` beans |
| GraphQLSchema                  | GraphQL schema generated based on the federated schema generator configuration and  `Query`, `Mutation` and `Subscription` objects available in the application context. |


## GraphQL Configuration
| Bean                             | Description |
|:---------------------------------|:------------|
| GraphQL                          | GraphQL execution engine generated using `GraphQLSchema` with default async execution strategies. GraphQL engine can be customized by optionally providing a list of [Instrumentation](https://www.graphql-java.com/documentation/v14/instrumentation/) beans (which can be ordered by implementing Spring Ordered interface), [ExecutionIdProvider](https://github.com/graphql-java/graphql-java/blob/master/src/main/java/graphql/execution/ExecutionIdProvider.java) and [PreparsedDocumentProvider](https://github.com/graphql-java/graphql-java/blob/master/src/main/java/graphql/execution/preparsed/PreparsedDocumentProvider.java) in the application context. |
| QueryHandler                     | Handler invoked from GraphQL query route that executes the incoming request, defaults to [SimpleQueryHandler](https://github.com/ExpediaGroup/graphql-kotlin/blob/master/graphql-kotlin-spring-server/src/main/kotlin/com/expediagroup/graphql/spring/execution/QueryHandler.kt). |


## Subscriptions
_Created only if the `Subscription` marker interface is used _

| Bean                             | Description |
|:---------------------------------|:------------|
| SubscriptionHandler              | Web socket handler for executing GraphQL subscriptions, defaults to [SimpleSubscriptionHandler](https://github.com/ExpediaGroup/graphql-kotlin/blob/master/graphql-kotlin-spring-server/src/main/kotlin/com/expediagroup/graphql/spring/execution/SubscriptionHandler.kt#L49).<br><br>_Created only if `Subscription` bean is available in the context._ |
| WebSocketHandlerAdapter          | See [Spring documentation](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/reactive/socket/server/support/WebSocketHandlerAdapter.html).<br><br>_Created only if `Subscription` bean is available in the context._ |
| ApolloSubscriptionHooks          | Provides hooks into the subscription request lifecycle. See [the subscription docs](./subscriptions.md) |


## Fixed Beans
The following beans are currently automatically created and cannot be disabled:

* Default routes for GraphQL queries/mutations and SDL endpoint
* Default route for [Prisma Labs Playground](https://github.com/prisma-labs/graphql-playground), created only if playground is enabled
* Default `ApolloSubscriptionProtocolHandler` for handling GraphQL subscriptions, created only if `Subscription` bean is available in the context
* Default `SubscriptionWebSocketHandler` that utilizes above subscription protocol handler, created only if `Subscription` bean is available in the context
* Default subscription handler mapping, created only if `Subscription` bean is available in the context
