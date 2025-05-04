package com.puj;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Facultad {
    public static void main(String[] args) {
        if(args.length != 4) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <nombre del programa> <semestre> <numero de salones requeridos> <numero de laboratorios requeridos>\n");
            System.exit(1);
        }

        final String nombre = args[0];
        final String semestre = args[1];
        final int numeroSalones = Integer.parseInt(args[2]);
        final int numeroLaboratorios = Integer.parseInt(args[3]);

        try (ZContext context = new ZContext()) {

            ZMQ.Socket socket = context.createSocket(SocketType.REQ);
            socket.connect("tcp://192.168.10.4:1080"); //Cambiar por la IP de la maquina con el servidor central
            System.out.println("Conectado al servidor de peticiones.\n");

            System.out.println("Peticion recibida para el programa " + nombre + " (" + semestre + "):\nSalones: " + numeroSalones + "\nLaboratorios: " + numeroLaboratorios + "\n");

            //Costruir mensaje
            String request = nombre + "|" + numeroSalones + "|" + numeroLaboratorios;
            socket.send(request.getBytes(ZMQ.CHARSET), 0);

            System.out.println("Peticion enviada, esperando respuesta...\n");

            //Recibir respuesta
            byte[] reply = socket.recv(0);
            System.out.println("Peticion completada: " + new String(reply, ZMQ.CHARSET));
        }
    }
}