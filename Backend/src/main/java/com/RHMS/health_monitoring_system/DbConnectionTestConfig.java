// package com.RHMS.health_monitoring_system;

// import io.r2dbc.spi.ConnectionFactory;
// import io.r2dbc.spi.Result;
// import org.springframework.boot.ApplicationRunner;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import reactor.core.publisher.Flux;
// import reactor.core.publisher.Mono;

// @Configuration
// public class DbConnectionTestConfig {

//     @Bean
//     public ApplicationRunner testDbConnection(ConnectionFactory connectionFactory) {
//         return args -> {
//             Mono.from(connectionFactory.create())
//                 .flatMapMany(conn ->
//                     Flux.from(conn.createStatement("SELECT 1").execute())
//                         .flatMap((Result result) ->
//                             result.map((row, metadata) -> row.get(0))
//                         )
//                         .doFinally(signal -> conn.close())
//                 )
//                 .doOnNext(val -> System.out.println("✅ DB connection test successful, result = " + val))
//                 .doOnError(err -> System.err.println("❌ DB connection failed: " + err.getMessage()))
//                 .subscribe();
//         };
//     }
// }
