package com.puj;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Facultad {
    public static void main(String[] args) {
        if(args.length != 5) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <nombre de la facultad> <semestre> <direccion IP del servidor Central> <puerto de la facultad> <direccion IP del servidor de respaldo>\n");
            System.exit(1);
        }

        final String nombre = args[0];
        final String semestre = args[1];
        final String serverIP = args[2];
        final String port = args[3];
        final String backupIP = args[4];

        System.out.println("\nFacultad de " + nombre + " para el semestre " + semestre + " creada.\n");

        try (ZContext context = new ZContext()) {
            //Servidor de programas - sincrono
            String addressProgram = "tcp://*:"+port;
            ZMQ.Socket receiveSocket = context.createSocket(SocketType.REP);
            receiveSocket.bind(addressProgram);
            System.out.println("inicializado el servidor de programas. Direccion: " + addressProgram + "\n");

            //Servidor Central - asincrono
            String addressCentral = "tcp://" + serverIP + ":1090";
            ZMQ.Socket sendSocket = context.createSocket(SocketType.DEALER);
            sendSocket.connect(addressCentral);
            System.out.println("Conectado al servidor central. Direccion: " + addressCentral + "\n");

            //Servidor de respaldo
            String addressBackup = "tcp://" + backupIP + ":1090";
            ZMQ.Socket backupSocket = context.createSocket(SocketType.DEALER);
            backupSocket.connect(addressBackup);
            System.out.println("Conectado al servidor de respaldo. Direccion: " + addressBackup + "\n");

            System.out.println("Esperando peticiones de los programas...\n");

            //Recibir peticion
            while (!Thread.currentThread().isInterrupted()) {
                //Recibir mensaje
                String recievedMessage = receiveSocket.recvStr();
                String[] parts = recievedMessage.split("\\,");

                System.out.println("\nNueva petición recibida: " + recievedMessage);

                //Costruir mensaje
                System.out.println("\nSalones: " + parts[2] + "\nLaboratorios: " + parts[3]);
                String numeroSalones = parts[2];
                String numeroLaboratorios = parts[3];
                String nombrePrograma = parts[0];
                String nombreFacultad = parts[4];
                String semestrePrograma = parts[1]; 

                //Enviar mensaje
                String request = nombrePrograma + "|" + numeroSalones + "|" + numeroLaboratorios + "|" + nombreFacultad + "|" + semestrePrograma;
                sendSocket.sendMore(""); //Mensaje vacio 
                sendSocket.send(request.getBytes(ZMQ.CHARSET), 0);
                System.out.println("\nPeticion enviada, esperando respuesta...\n");

                //Recibir y procesar respuesta
                sendSocket.recv(0); 
                byte[] reply = sendSocket.recv(0);

                String replyString = new String(reply, ZMQ.CHARSET);
                String[] partsResponse = replyString.split("\\|");

                System.out.println("Peticion completada. \nSalones: " + partsResponse[0] + "\nLaboratorios: " + partsResponse[1]);
                System.out.println("Status: " + partsResponse[2]);

                receiveSocket.send(replyString);
            }
        }
    }
}