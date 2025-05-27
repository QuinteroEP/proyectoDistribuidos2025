package com.puj.servidores;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.time.LocalDate;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.puj.dbManager;

public class central {
    // Tiempos de respuets
    public static List<Long> tiempos = new ArrayList<>();
    public static long runningTimeTotal = 0;
    public static dbManager manager;

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println(
                    "\nError: uso incorrecto. Se requieren los parametros <cantidad de salones disponibles> <cantidad de laboratorios disponibles>\n");
            System.exit(1);
        }

        final int can_Salones = Integer.parseInt(args[0]);
        final int can_Labs = Integer.parseInt(args[1]);

        // Laboratorios y salones
        List<String> salones = new ArrayList<>();
        List<String> laboratorios = new ArrayList<>();

        // Crear Socket
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.ROUTER);
            socket.bind("tcp://*:1090");
            System.out.println(
                    "\nServidor central abierto en el puerto 1090. Direccion " + InetAddress.getLocalHost() + "\n");

            ZMQ.Socket workerSocket = context.createSocket(SocketType.DEALER);
            workerSocket.bind("inproc://backend");

            String addressBackup = "tcp://*:1092";
            ZMQ.Socket BackupSocket = context.createSocket(SocketType.PUB);
            BackupSocket.bind(addressBackup);
            System.out.println("Puerto 1092 para heartbeats abierto\n");

            String addressUpdates = "tcp://*:1093";
            ZMQ.Socket updateSocket = context.createSocket(SocketType.PUB);
            updateSocket.bind(addressUpdates);
            System.out.println("Puerto 1093 para actualizaciones del servidor replica abierto\n");

            // Proceso para heartbeats
            new Thread(() -> {
                try (ZContext heartbeatContext = new ZContext()) {
                    while (!Thread.currentThread().isInterrupted()) {
                        BackupSocket.send("HEARTBEAT");
                        Thread.sleep(3000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

            // Popular listas de salones
            for (int i = 1; i <= can_Salones; i++) {
                String n = String.valueOf(i);
                String s = n + "S";

                salones.add(s);
            }

            for (int i = 1; i <= can_Labs; i++) {
                String n = String.valueOf(i);
                String l = n + "L";

                laboratorios.add(l);
            }

            System.out.println("Salones disponibles: " + salones);
            System.out.println("Laboratorios disponibles: " + laboratorios);
            System.out.println("\n");

            System.out.println("Esperando peticiones...\n");

            // Crear trabajadores
            for (int threadNbr = 0; threadNbr < 10; threadNbr++) {
                new Thread(new handleRequest(context, salones, laboratorios, updateSocket)).start();
            }

            ZMQ.proxy(socket, workerSocket, null);
        }
    }

    private static class handleRequest implements Runnable {
        private ZContext ctx;

        List<String> salonesDisponibles = new ArrayList<>();
        List<String> laboratoriosDisponibles = new ArrayList<>();

        List<String> salonesAsignados = new ArrayList<>();
        List<String> laboratoriosAsignados = new ArrayList<>();
        String status = "";

        Boolean ClassSuccess = true;
        Boolean LabSuccess = true;
        Boolean incomplete = false;

        ZMQ.Socket updateSocket;

        public handleRequest(ZContext ctx, List<String> salonesDisponibles, List<String> laboratoriosDisponibles,
                ZMQ.Socket updateSocket) {
            this.salonesDisponibles = salonesDisponibles;
            this.laboratoriosDisponibles = laboratoriosDisponibles;
            this.ctx = ctx;
            this.updateSocket = updateSocket;
        }

        @Override
        public void run() {
            ZMQ.Socket worker = ctx.createSocket(SocketType.DEALER);
            worker.connect("inproc://backend");

            while (!Thread.currentThread().isInterrupted()) {
                // Reibir peitciones
                ZMsg msg = ZMsg.recvMsg(worker);

                ZFrame adress = msg.pop();
                msg.pop();
                ZFrame content = msg.pop();

                String message = content.getString(ZMQ.CHARSET);

                String[] parts = message.split("\\|");
                String nombre = parts[0];
                int numeroSalones = Integer.parseInt(parts[1]);
                int numeroLaboratorios = Integer.parseInt(parts[2]);
                String nombreFacultad = parts[3];
                String semestrePrograma = parts[4];

                System.out.println("\nNueva solicitud del programa " + nombre + ": " + numeroSalones + " salones; "
                        + numeroLaboratorios + " Laboratorios. (Semestre: " + semestrePrograma + ", facultad: "
                        + nombreFacultad + ")\n");

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
                    ClassSuccess = false;
                    if (salonesDisponibles.size() != 0) {
                        while (!salonesDisponibles.isEmpty()) {
                            salonesAsignados.add(salonesDisponibles.get(0));
                            salonesDisponibles.remove(0);
                        }
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

                ZMsg reply = new ZMsg();
                reply.add(adress);
                reply.addString("");
                reply.addString(salonesAsignados + "|" + laboratoriosAsignados + "|" + status);
                reply.send(worker);

                new Thread(() -> {
                    System.out.println("Actualizando backup");
                    byte[] bytes_salones = ByteBuffer.allocate(4).putInt(salonesDisponibles.size()).array();
                    byte[] bytes_labs = ByteBuffer.allocate(4).putInt(laboratoriosDisponibles.size()).array();

                    updateSocket.sendMore("UPDATE");
                    updateSocket.sendMore(bytes_salones);
                    updateSocket.send(bytes_labs);
                }).start();

                content.destroy();
            }
            ctx.destroy();
        }
    }
}