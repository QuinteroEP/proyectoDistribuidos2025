package com.puj;

import java.util.ArrayList;
import java.util.List;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;

public class FacultadLB {
    private static final long REQUEST_TIMEOUT = 5000;
    private static final long DELAY = 3000;

    // Tiempos de respuets
    public static List<Long> tiempos = new ArrayList<>();
    public static long runningTimeTotal = 0;

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 5) {
            System.out.println(
                    "\nError: uso incorrecto. Se requieren los parametros <nombre de la facultad> <semestre> <direccion IP del servidor Central> <puerto de la facultad> <direccion IP del servidor de respaldo>\n");
            System.exit(1);
        }

        final String nombre = args[0];
        final String semestre = args[1];
        final String serverIP = args[2];
        final String port = args[3];
        final String backupIP = args[4];

        long startTime;
        long endTime;
        long responseTime;

        System.out.println("\nFacultad de " + nombre + " para el semestre " + semestre + " creada.\n");

        try (ZContext context = new ZContext()) {
            // Servidor de programas - sincrono
            String addressProgram = "tcp://*:" + port;
            ZMQ.Socket receiveSocket = context.createSocket(SocketType.REP);
            receiveSocket.bind(addressProgram);
            System.out.println("inicializado el servidor de programas. Direccion: " + addressProgram + "\n");

            // Servidor Central - asincrono
            String addressCentral = "tcp://" + serverIP + ":1090";
            ZMQ.Socket sendSocket = context.createSocket(SocketType.REQ);
            sendSocket.connect(addressCentral);
            System.out.println("Conectado al servidor central. Direccion: " + addressCentral + "\n");

            System.out.println("Esperando peticiones de los programas...\n");

            // Recibir peticion
            while (!Thread.currentThread().isInterrupted()) {
                // Recibir mensaje
                String recievedMessage = receiveSocket.recvStr();
                String[] parts = recievedMessage.split("\\,");

                System.out.println("\nNueva petición recibida: " + recievedMessage);

                // Costruir mensaje
                System.out.println("\nSalones: " + parts[2] + "\nLaboratorios: " + parts[3]);
                String numeroSalones = parts[2];
                String numeroLaboratorios = parts[3];
                String nombrePrograma = parts[0];
                String nombreFacultad = parts[4];
                String semestrePrograma = parts[1];

                // Enviar mensaje
                String request = nombrePrograma + "|" + numeroSalones + "|" + numeroLaboratorios + "|" + nombreFacultad
                        + "|" + semestrePrograma;
                startTime = System.currentTimeMillis();
                sendSocket.send(request.getBytes(ZMQ.CHARSET), 0);

                // Verficar conexiones
                boolean received = false;
                Poller poller = context.createPoller(1);
                poller.register(sendSocket, Poller.POLLIN);
                int rc = poller.poll(REQUEST_TIMEOUT);
                if (poller.pollin(0)) {
                    // System.out.println("\nServidor central funcionando\n");
                    received = true;
                } else if (rc == 0) {
                    // System.out.println("\nProblema sospechado con el servidor central\n");
                    sendSocket.close();

                    // System.out.println("\nIntentando conectar con el servidor replica\n");

                    // Cambiar a servidor de respaldo
                    String addressBackup = "tcp://" + backupIP + ":1092";
                    sendSocket = context.createSocket(SocketType.REQ);
                    sendSocket.connect(addressBackup);
                    poller.register(sendSocket, ZMQ.Poller.POLLIN);

                    // Volver a enviar mensaje
                    sendSocket.send(request.getBytes(ZMQ.CHARSET), 0);
                    received = true;
                }

                // Recibir y procesar respuesta
                System.out.println("\nPeticion enviada, esperando respuesta...\n");
                byte[] reply = sendSocket.recv(0);

                String replyString = new String(reply, ZMQ.CHARSET);
                String[] partsResponse = replyString.split("\\|");

                System.out.println(
                        "Peticion completada. \nSalones: " + partsResponse[0] + "\nLaboratorios: " + partsResponse[1]);
                System.out.println("Status: " + partsResponse[2]);

                receiveSocket.send(replyString);

                endTime = System.currentTimeMillis();
                responseTime = endTime - startTime;
                System.out.println("\nTiempo de respuesta: " + responseTime + " ms\n");
                getTimes(responseTime);
                responseTime = 0;
            }
            context.destroy();
        }
    }

    public static void getTimes(Long t) {
        long maxTime = 0;
        long minTime = 1000;
        long promedio = 0;

        runningTimeTotal = runningTimeTotal + t;
        tiempos.add(t);

        if (tiempos.size() == 5) {
            // Tiempo maximo
            for (int i = 0; i < tiempos.size(); i++) {
                if (tiempos.get(i) > maxTime) {
                    maxTime = tiempos.get(i);
                }
            }

            // Tiempo minimo
            for (int i = 0; i < tiempos.size(); i++) {
                if (tiempos.get(i) < minTime) {
                    minTime = tiempos.get(i);
                }
            }

            promedio = runningTimeTotal / 5;

            System.out.println("\nTiempo minimo de respueta: " + minTime + "ms");
            System.out.println("Tiempo maximo de respueta: " + maxTime + "ms");
            System.out.println("Tiempo promedio de respueta: " + promedio + "ms\n");

            runningTimeTotal = 0;
            tiempos.clear();
        }
    }
}