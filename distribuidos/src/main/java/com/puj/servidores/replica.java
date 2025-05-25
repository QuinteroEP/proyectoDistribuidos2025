package com.puj.servidores;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.puj.dbManager;

public class replica { // ✅ corregido el nombre de clase con mayúscula
    public static List<Long> tiempos = new ArrayList<>();
    public static long runningTimeTotal = 0;
    public static dbManager manager;

    static int salonesSize = 0;
    static int labsSize = 0;

    private static boolean isPrimary = false;

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <direccion de servidor central> <tiempo maximo de espera en Ms>\n");
            System.exit(1);
        }

        final String centralIP = args[0];
        final long TIMEOUT = Long.parseLong(args[1]);

        new Thread(() -> monitorPrimary(centralIP, TIMEOUT)).start();

        while (!isPrimary) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        startAsPrimary();
    }

    private static void monitorPrimary(String IP, Long TIMEOUT) {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket centralSocket = context.createSocket(SocketType.SUB);

            String addressCentral = "tcp://" + IP + ":1092";
            centralSocket.connect(addressCentral);
            centralSocket.subscribe(ZMQ.SUBSCRIPTION_ALL);

            System.out.println("\nConexion creada con el servidor central, direccion: " + addressCentral + "\n");

            long lastHeartbeat = System.currentTimeMillis();

            while (!isPrimary) {
                ZMQ.Poller poller = context.createPoller(1);
                poller.register(centralSocket, ZMQ.Poller.POLLIN);

                if (poller.poll(10000) > 0) {
                    String msg = centralSocket.recvStr();
                    if ("HEARTBEAT".equals(msg)) {
                        lastHeartbeat = System.currentTimeMillis();
                        System.out.println("\nHeartbeat de central recibido...\n");
                    } else if ("Actualizacion".equals(msg)) {
                        byte[] salonesSizeBytes = centralSocket.recv(0);
                        byte[] labsSizeBytes = centralSocket.recv(0);

                        salonesSize = ByteBuffer.wrap(salonesSizeBytes).getInt();
                        labsSize = ByteBuffer.wrap(labsSizeBytes).getInt();

                        System.out.println("Nuevo tamaño de salones: " + salonesSize);
                        System.out.println("Nuevo tamaño de laboratorios: " + labsSize);
                        lastHeartbeat = System.currentTimeMillis();

                        while (poller.poll(0) > 0) {
                            centralSocket.recv(ZMQ.DONTWAIT); // descartar
                        }
                    }
                }

                if (System.currentTimeMillis() - lastHeartbeat > TIMEOUT) {
                    isPrimary = true;
                }

                Thread.sleep(3000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startAsPrimary() {
        System.out.println("\n||Alerta: Conexion con servidor primario perdida, cambiando estado a activo||\n");

        List<String> salones = new ArrayList<>();
        List<String> laboratorios = new ArrayList<>();

        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.ROUTER);
            socket.bind("tcp://*:1092");

            try {
                System.out.println("\nServidor de respaldo abierto en el puerto 1092. Direccion " + InetAddress.getLocalHost() + "\n");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            for (int i = 1; i <= salonesSize; i++) {
                salones.add(i + "S");
            }

            for (int i = 1; i <= labsSize; i++) {
                laboratorios.add(i + "L");
            }

            System.out.println("Salones disponibles: " + salones);
            System.out.println("Laboratorios disponibles: " + laboratorios);
            System.out.println("\nEsperando peticiones...\n");

            ExecutorService executor = Executors.newCachedThreadPool();

            while (!Thread.currentThread().isInterrupted()) {
                byte[] id = socket.recv(0);
                socket.recv(0);
                byte[] request = socket.recv(0);

                String message = new String(request, ZMQ.CHARSET);

                executor.submit(() -> {
                    String reply = handleRequest(message, salones, laboratorios);
                    socket.sendMore(id);
                    socket.sendMore("");
                    socket.send(reply);
                });
            }
        }
    }

    private static String handleRequest(String message, List<String> salonesDisponibles, List<String> laboratoriosDisponibles) {
        List<String> salonesAsignados = new ArrayList<>();
        List<String> laboratoriosAsignados = new ArrayList<>();
        String status = "";

        boolean classSuccess = true;
        boolean labSuccess = true;
        boolean incomplete = false;

        long startTime = System.currentTimeMillis();

        String[] parts = message.split("\\|");
        String nombre = parts[0];
        int numeroSalones = Integer.parseInt(parts[1]);
        int numeroLaboratorios = Integer.parseInt(parts[2]);
        String nombreFacultad = parts[3];
        String semestrePrograma = parts[4];

        // Asignación de salones
        if (salonesDisponibles.size() >= numeroSalones) {
            for (int i = 0; i < numeroSalones; i++) {
                salonesAsignados.add(salonesDisponibles.remove(0));
            }
        } else if (!salonesDisponibles.isEmpty()) {
            for (int i = 0; i < salonesDisponibles.size(); i++) {
                salonesAsignados.add(salonesDisponibles.remove(0));
            }
            classSuccess = false;
        } else {
            incomplete = true;
        }

        // Asignación de laboratorios
        if (laboratoriosDisponibles.size() >= numeroLaboratorios) {
            for (int i = 0; i < numeroLaboratorios; i++) {
                laboratoriosAsignados.add(laboratoriosDisponibles.remove(0));
            }
        } else if (salonesDisponibles.size() >= numeroLaboratorios) {
            for (int i = 0; i < laboratoriosDisponibles.size(); i++) {
                laboratoriosAsignados.add(laboratoriosDisponibles.remove(0));
            }
            for (int i = 0; i < numeroLaboratorios - laboratoriosAsignados.size(); i++) {
                laboratoriosAsignados.add(salonesDisponibles.remove(0));
            }
        } else if (!laboratoriosDisponibles.isEmpty()) {
            for (int i = 0; i < laboratoriosDisponibles.size(); i++) {
                laboratoriosAsignados.add(laboratoriosDisponibles.remove(0));
            }
            labSuccess = false;
        } else {
            labSuccess = false;
        }

        // Registrar estado en BD
        if (labSuccess && classSuccess) {
            status = "completado";
            dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestrePrograma, nombreFacultad, LocalDate.now().toString());
        } else if (incomplete) {
            status = "pendiente";
            dbManager.writePending(nombre, numeroSalones, numeroLaboratorios, nombreFacultad, semestrePrograma, LocalDate.now().toString());
        } else {
            status = "completado parcialmente";
            dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestrePrograma, nombreFacultad, LocalDate.now().toString());
        }

        long responseTime = System.currentTimeMillis() - startTime;
        System.out.println("Tiempo de respuesta: " + responseTime + " ms");
        getTimes(responseTime);

        return salonesAsignados + "|" + laboratoriosAsignados + "|" + status;
    }

    public static void getTimes(Long t) {
        long maxTime = 0;
        long minTime = 1000;
        long promedio;

        runningTimeTotal += t;
        tiempos.add(t);

        if (tiempos.size() == 5) {
            for (Long time : tiempos) {
                if (time > maxTime) maxTime = time;
                if (time < minTime) minTime = time;
            }

            promedio = runningTimeTotal / 5;
            System.out.println("\nTiempo mínimo: " + minTime + " ms");
            System.out.println("Tiempo máximo: " + maxTime + " ms");
            System.out.println("Promedio: " + promedio + " ms\n");
        }
    }
}
