package com.ct.fastreport.aws.lambda;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.ct.fastreport.Application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ApiGatewayLambdaHandler implements RequestStreamHandler {

    private static final SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> HANDLER =
            initHandler();

    @Override
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        HANDLER.proxyStream(input, output, context);
    }

    private static SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> initHandler() {
        try {
            SpringBootLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> handler =
                    SpringBootLambdaContainerHandler.getHttpApiV2ProxyHandler(Application.class, "aws");
            handler.stripBasePath("/prod");
            return handler;
        } catch (ContainerInitializationException ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }
}
