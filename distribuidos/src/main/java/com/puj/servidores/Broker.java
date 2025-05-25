package com.puj.servidores;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Broker {
 public static void main(String[] args) {
     //Requiere: puerto para recibir (desde facultades) y enviar (a workers)
     final String frontendPort = "5555"; //Facultad -> Broker
     final String backendPort = "5556"; //Broker -> Worker

     try(ZContext context = new ZContext()) {
        //ROUTER: recibe desde las facultades
         ZMQ.Socket frontend = context.createSocket(ZMQ.ROUTER);
            frontend.bind("tcp://*:" + frontendPort);
            System.out.println("[Broker] Escuchando las solicitudes de facultades en puerto " + frontendPort);

        //DEALER: envia a los workers
         ZMQ.Socket backend = context.createSocket(ZMQ.DEALER);
            backend.bind("tcp://*:" + backendPort);
            System.out.println("[Broker] Enrutando solicitudes a los workers en puerto " + backendPort);

        //Uso de un dispositivo ZeroMQ simple para poder redirigir los mensajes
        ZMQ.proxy(frontend, backend, null);
     }catch (Exception e) {
        e.printStackTrace();
     }
 }  
}
