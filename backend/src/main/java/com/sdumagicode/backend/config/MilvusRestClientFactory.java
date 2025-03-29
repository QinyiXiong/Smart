package com.sdumagicode.backend.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
 
/**
 * @author qingf
 * @title MilvusRestClientFactory
 * @description
 * @create 2024/4/23
 */
public class MilvusRestClientFactory {
 
    private static String  IP_ADDR;
 
    private static Integer PORT ;
 
    private MilvusServiceClient milvusServiceClient;
 
    private ConnectParam.Builder  connectParamBuilder;
 
 
    private static MilvusRestClientFactory milvusRestClientFactory = new MilvusRestClientFactory();
 
    private MilvusRestClientFactory(){
 
    }
 
    public static MilvusRestClientFactory build(String ipAddr, Integer  port) {
        IP_ADDR = ipAddr;
        PORT = port;
        return milvusRestClientFactory;
    }
 
    private ConnectParam.Builder connectParamBuilder(String host, int port) {
        return  ConnectParam.newBuilder().withHost(host).withPort(port);
    }
 
 
 
    public void init() {
        connectParamBuilder =  connectParamBuilder(IP_ADDR,PORT);
        ConnectParam connectParam = connectParamBuilder.build();
        milvusServiceClient =new MilvusServiceClient(connectParam);
    }
 
 
    public MilvusServiceClient getMilvusClient() {
        return milvusServiceClient;
    }
 
 
    public void close() {
        if (milvusServiceClient != null) {
            try {
                milvusServiceClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}