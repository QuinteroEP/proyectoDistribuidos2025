package com.puj.servidorCentral;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class servidor {
    public static void main(String[] args){
    //Abrir el puerto 1080 para la aplicacion
        System.out.println("Inicializando servidor de reserva de salones");

        Server server = ServerBuilder
            .forPort(1080)
            .addService(new servidorgRPC()) 
            .build();
         try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getLocalHost();
            System.out.println("Servidor corriendo en la IP " + inetAddress.getHostAddress() + " en el puerto " + server.getPort());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        System.out.println("Esperando peticiones del clientes...\n");

        try {
            server.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            server.shutdown();
            System.out.println("Conexion terminada");
        }));
    }
}
