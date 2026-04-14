package com.ct.fastreport.aws.lambda;

import com.ct.fastreport.Application;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

final class SpringLambdaContext {

    private static volatile ConfigurableApplicationContext context;

    private SpringLambdaContext() {
    }

    static ConfigurableApplicationContext get() {
        ConfigurableApplicationContext current = context;
        if (current == null) {
            synchronized (SpringLambdaContext.class) {
                current = context;
                if (current == null) {
                    current = new SpringApplicationBuilder(Application.class)
                            .profiles("aws")
                            .properties("spring.autoconfigure.exclude=org.springframework.cloud.function.serverless.web.ServerlessAutoConfiguration")
                            .web(WebApplicationType.NONE)
                            .run();
                    context = current;
                }
            }
        }
        return current;
    }
}
