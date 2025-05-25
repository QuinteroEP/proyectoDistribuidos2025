package com.puj.servidores;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.puj.dbManager;

public class replica {
    public static List<Long> tiempos = new ArrayList<>();
    public static long runningTimeTotal = 0;
    public static dbManager manager;

    static int salonesSize = 0;
    static int labsSize = 0;
    
    private static boolean isPrimary = false;

    public static void main(String[] args) throws UnknownHostException {
        if(args.length != 4) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <cantidad de salones disponibles> <cantidad de laboratorios disponibles> <direccion de servidor central> <tiempo maximo de espera en Ms>\n");
            System.exit(1);
        }

        final int can_Salones = Integer.parseInt(args[0]);
        final int can_Labs = Integer.parseInt(args[1]);
        final String centralIP = args[2];
        final long TIMEOUT = Long.parseLong(args[3]);

        // Laboratorios y salones
        List<String> salones = new ArrayList<>();
        List<String> laboratorios = new ArrayList<>();

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
        
        salonesSize = can_Salones;
        labsSize = can_Labs;

        new Thread(() -> monitorPrimary(centralIP, TIMEOUT)).start();

        while (!isPrimary) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        startAsPrimary(salones, laboratorios);
    }

    private static void monitorPrimary(String IP, Long TIMEOUT) {
        try (ZContext context = new ZContext()){
            String addressCentralHeartbeats = "tcp://" + IP + ":1092";
            ZMQ.Socket backupSocket = context.createSocket(SocketType.SUB);
            backupSocket.connect(addressCentralHeartbeats);
            backupSocket.subscribe("".getBytes());
            System.out.println("\nConexion creada con el servidor central, direccion: " + addressCentralHeartbeats + "\n");

            String addressCentralUpdates = "tcp://" + IP + ":1093";
            ZMQ.Socket backupUpdateSocket = context.createSocket(SocketType.SUB);
            backupUpdateSocket.connect(addressCentralUpdates);
            backupUpdateSocket.subscribe("".getBytes());
            System.out.println("\nPuerto para actualizaciones: " + addressCentralUpdates + "\n");

            long lastHeartbeat = System.currentTimeMillis();

            while (!isPrimary) {
                ZMQ.Poller poller = context.createPoller(2);
                poller.register(backupSocket, ZMQ.Poller.POLLIN);
                poller.register(backupUpdateSocket, ZMQ.Poller.POLLIN);

                if (poller.poll(10000) > 0) {
                    if (poller.pollin(0)) {
                        String msg_beats = backupSocket.recvStr();
                        lastHeartbeat = System.currentTimeMillis();
                        System.out.println("\nHearbeat de central recibido...\n");
                    }
                    if (poller.pollin(1)) {
                        String msg_updates = backupUpdateSocket.recvStr();
                        lastHeartbeat = System.currentTimeMillis();
                        byte[] salonesSizeBytes = backupUpdateSocket.recv(0);
                        byte[] labsSizeBytes = backupUpdateSocket.recv(0);

                        salonesSize = ByteBuffer.wrap(salonesSizeBytes).getInt();
                        labsSize = ByteBuffer.wrap(labsSizeBytes).getInt();

                        System.out.println("\nNuevo tamaño de salones: " + salonesSize);
                        System.out.println("Nuevo tamaño de laboratorios: " + labsSize);
                    }
                } 
                if (System.currentTimeMillis() - lastHeartbeat > TIMEOUT) {
                    isPrimary = true;
                    backupSocket.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startAsPrimary(List<String> salones, List<String> laboratorios) throws UnknownHostException {
        System.out.println("\n||Alerta: Conexion con servidor primario perdida, cambiando estado a activo||\n");

        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.ROUTER);
            socket.bind("tcp://*:1092");
            System.out.println("\nServidor de respaldo abierto como primario en el puerto 1092. Direccion " + InetAddress.getLocalHost() + "\n");
            
            ZMQ.Socket workerSocket = context.createSocket(SocketType.DEALER);
            workerSocket.bind("inproc://backend");

            // Actualizar lista de salones
            salones = new ArrayList<>(salones.subList(salones.size() - salonesSize, salones.size()));
            laboratorios = new ArrayList<>(laboratorios.subList(laboratorios.size() - labsSize, laboratorios.size()));

            System.out.println("Salones disponibles: " + salones);
            System.out.println("Laboratorios disponibles: " + laboratorios);
            System.out.println("\n");

            System.out.println("Esperando peticiones...\n");

            // Crear trabajadores
            for (int threadNbr = 0; threadNbr < 5; threadNbr++){
                new Thread(new handleRequest(context, salones, laboratorios)).start();
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

        long startTime;
        long endTime;
        long responseTime;

        public handleRequest(ZContext ctx, List<String> salonesDisponibles, List<String> laboratoriosDisponibles)
        {
            this.salonesDisponibles = salonesDisponibles;
            this.laboratoriosDisponibles = laboratoriosDisponibles;
            this.ctx = ctx;
        }
        @Override
        public void run(){
            ZMQ.Socket worker = ctx.createSocket(SocketType.DEALER);
            worker.connect("inproc://backend");

            while (!Thread.currentThread().isInterrupted()) {
                //Timer interno
                startTime = System.currentTimeMillis();
                
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

                System.out.println("\nNueva solicitud del programa " + nombre + ": " + numeroSalones + " salones; " + numeroLaboratorios + " Laboratorios. (Semestre: " + semestrePrograma + ", facultad: " + nombreFacultad + ")\n");

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

                endTime = System.currentTimeMillis();
                responseTime = endTime - startTime;

                System.out.println("\nSalones disponibles: " + salonesDisponibles);
                System.out.println("Laboratorios disponibles: " + laboratoriosDisponibles);
                System.out.println("\n");

                ZMsg reply = new ZMsg();
                reply.add(adress);
                reply.addString("");
                reply.addString(salonesAsignados + "|" + laboratoriosAsignados + "|" + status);
                reply.send(worker);

                System.out.println("\nTiempo de respuesta: " + responseTime + " ms\n");
                getTimes(responseTime);

                content.destroy();
            }
            ctx.destroy();
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
        }
    }
}
