package com.puj;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Programa {
    public static void main(String[] args) {
        if(args.length != 6) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <nombre del programa> <semestre> <facultad del programa> <numero de salones requeridos> <numero de laboratorios requeridos> <puerto de la facultad>\n");
            System.exit(1);
        }

        final String nombre = args[0];
        final String semestre = args[1];
        final String facultad = args[2];
        final int numeroSalones = Integer.parseInt(args[3]);
        final int numeroLaboratorios = Integer.parseInt(args[4]);
        final String port = args[5];

        System.out.println("\nNueva peticion creada para el programa " + nombre + " (" + semestre + "):");
        System.out.println("Salones: " + numeroSalones);
        System.out.println("Laboratorios: " + numeroLaboratorios);
        System.out.println("\nEnviando peticion a la facultad de " + facultad + "...\n");

         try (ZContext context = new ZContext()) {
            String address = "tcp://127.0.0.1:"+port;
            ZMQ.Socket socket = context.createSocket(SocketType.REQ);
            socket.connect(address);

            String mensaje = nombre + "," + semestre + "," + numeroSalones + "," + numeroLaboratorios + "," + facultad;
            socket.send(mensaje);

            String reply = socket.recvStr();
            System.out.println("Respuesta de la facultad: " + reply);
        }
    }
}
