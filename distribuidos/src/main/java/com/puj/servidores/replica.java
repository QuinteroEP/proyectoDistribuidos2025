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

public class replica {
    public static List<Long> tiempos = new ArrayList<>();
    public static long runningTimeTotal = 0;
    public static dbManager manager;

    static int salonesSize = 0;
    static int labsSize = 0;
    
    private static boolean isPrimary = false;

    public static void main(String[] args) {
        if(args.length != 2) {
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
        try (ZContext context = new ZContext()){
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
                    System.out.println("\nHearbeat de central recibido...\n");
                    }
                    else if ("Actualizacion".equals(msg)){
                        byte[] salonesSizeBytes = centralSocket.recv(0);
                        byte[] labsSizeBytes = centralSocket.recv(0);

                        salonesSize = ByteBuffer.wrap(salonesSizeBytes).getInt();
                        labsSize = ByteBuffer.wrap(labsSizeBytes).getInt();

                        System.out.println("Nuevo tamaño de salones: " + salonesSize);
                        System.out.println("Nuevo tamaño de laboratorios: " + labsSize);
                        lastHeartbeat = System.currentTimeMillis();
                        while (poller.poll(0) > 0) {
                            centralSocket.recv(ZMQ.DONTWAIT); // Read and discard
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

        // Laboratorios y salones
        List<String> salones = new ArrayList<>();
        List<String> laboratorios = new ArrayList<>();

        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.ROUTER);
            socket.bind("tcp://*:1092");
            try {
                System.out.println(
                        "\nServidor de respaldo abierto en el puerto 1092. Direccion " + InetAddress.getLocalHost() + "\n");
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }

            // Popular listas de salones
            for (int i = 1; i <= salonesSize; i++) {
                String n = String.valueOf(i);
                String s = n + "S";

                salones.add(s);
            }

            for (int i = 1; i <= labsSize; i++) {
                String n = String.valueOf(i);
                String l = n + "L";

                laboratorios.add(l);
            }

            System.out.println("Salones disponibles: " + salones);
            System.out.println("Laboratorios disponibles: " + laboratorios);
            System.out.println("\n");

            System.out.println("Esperando peticiones...\n");

            // Crear trabajadores
            ExecutorService executor = Executors.newCachedThreadPool();
            
            // Procesar mensajes
            while (!Thread.currentThread().isInterrupted()) {
                // Reibir peitciones
                byte[] id = socket.recv(0);
                socket.recv(0);
                byte[] request = socket.recv(0);

                String message = new String(request, ZMQ.CHARSET);

                // Enviar peticion a trabajador
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

        Boolean ClassSuccess = true;
        Boolean LabSuccess = true;
        Boolean incomplete = false;

        long startTime;
        long endTime;
        long responseTime;

        startTime = System.currentTimeMillis();

        // Procesar el mensaje
        String[] parts = message.split("\\|");
        String nombre = parts[0];
        int numeroSalones = Integer.parseInt(parts[1]);
        int numeroLaboratorios = Integer.parseInt(parts[2]);
        String nombreFacultad = parts[3];
        String semestrePrograma = parts[4];

        System.out.println("\nNueva solicitud del programa " + nombre + ": " + numeroSalones + " salones; "
                + numeroLaboratorios + " Laboratorios.");
        System.out.println("(Semestre: " + semestrePrograma + ", facultad: " + nombreFacultad + ")\n");

        // Realizar asignacion de salones

        // Salones
        if (salonesDisponibles.size() >= numeroSalones) {
            for (int i = 0; i < numeroSalones; i++) {
                salonesAsignados.add(salonesDisponibles.get(0));
                salonesDisponibles.remove(0);
            }
            ClassSuccess = true;

        } else {
            System.out.println("Atencion: Salones insuficientes.\n");

            if (salonesDisponibles.size() != 0) {
                for (int i = 0; i <= salonesDisponibles.size(); i++) {
                    salonesAsignados.add(salonesDisponibles.get(0));
                    salonesDisponibles.remove(0);
                }
                ClassSuccess = false;
            } else {
                incomplete = true;
            }

        }
        System.out.println("Salones asignados a " + nombre + ": " + salonesAsignados);

        // Laboratorios
        if (laboratoriosDisponibles.size() >= numeroLaboratorios) {
            for (int i = 0; i < numeroLaboratorios; i++) {
                laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                laboratoriosDisponibles.remove(0);
            }
            incomplete = false;
            LabSuccess = true;

        } else if (salonesDisponibles.size() >= numeroLaboratorios) {
            System.out.println("\nAtencion: Laboratorios insuficientes, asignado laboratorios hibridos.\n");

            for (int i = 0; i < laboratoriosDisponibles.size(); i++) {
                laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                laboratoriosDisponibles.remove(0);
            }

            for (int i = 0; i < numeroLaboratorios - laboratoriosAsignados.size(); i++) {
                laboratoriosAsignados.add(salonesDisponibles.get(0));
                salonesDisponibles.remove(0);
            }
            incomplete = false;
            LabSuccess = true;
        } else {
            System.out.println("\nAtencion: Laboratorios insuficientes.\n");

            if (laboratoriosDisponibles.size() != 0) {
                for (int i = 0; i <= laboratoriosDisponibles.size(); i++) {
                    laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                    laboratoriosDisponibles.remove(0);
                }
            }
            LabSuccess = false;
        }
        System.out.println("\nLaboratorios asignados a " + nombre + ": " + laboratoriosAsignados);

        // Estado de la peticion y registrar peticion
        if (LabSuccess && ClassSuccess) {
            System.out.println("\nPeticion completada sin problemas\n");
            status = "completado";
            dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestrePrograma,
                    nombreFacultad, LocalDate.now().toString());
        } else if (incomplete) {
            System.out.println("\nLa peticion no pudo ser completada.\n");
            status = "pendiente";
            dbManager.writePending(nombre, numeroSalones, numeroLaboratorios, nombreFacultad, semestrePrograma,
                    LocalDate.now().toString());
        } else if (!LabSuccess || !ClassSuccess) {
            System.out.println("\nLa peticion no pudo ser completada en su totalidad\n");
            status = "completado parcialmente";
            dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestrePrograma,
                    nombreFacultad, LocalDate.now().toString());
        }

        System.out.println("\nSalones disponibles: " + salonesDisponibles);
        System.out.println("Laboratorios disponibles: " + laboratoriosDisponibles);
        System.out.println("\n");

        // Enviar respuesta
        endTime = System.currentTimeMillis();
        responseTime = endTime - startTime;

        System.out.println("\nTiempo de respuesta: " + responseTime + " ms\n");
        getTimes(responseTime);

        String response = salonesAsignados + "|" + laboratoriosAsignados + "|" + status;
        return response;
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
        }
    }
}
