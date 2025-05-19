package com.puj.servidores;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.zeromq.ZMQ;

public class replica {
    private static final String DB_URL = "jdbc:postgresql://postgres:5432/aulas";
    private static final String DB_USER = "postgres";
    private static final String DB_PASSWORD = "admin";
    private static final int PORT = 5555;
    private static boolean isPrimary = false;

    public static void main(String[] args) {
        if(args.length != 2) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <cantidad de salones disponibles> <cantidad de laboratorios disponibles>\n");
            System.exit(1);
        }

        final int can_Salones = Integer.parseInt(args[0]);
        final int can_Labs = Integer.parseInt(args[1]);

        //Laboratorios y salones
        List<String> salones = new ArrayList<>();
        List<String> laboratorios = new ArrayList<>();

        //Popular listas de salones
        for(int i = 1; i <= can_Salones; i++){
            String n = String.valueOf(i);
            String s = n + "S";

            salones.add(s);
        }

        for(int i = 1; i <= can_Labs; i++){
            String n = String.valueOf(i);
            String l = n + "L";

            laboratorios.add(l);
        }
        
        new Thread(replica::monitorPrimary).start();

        while (!isPrimary) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        startAsPrimary();
    }

    private static void monitorPrimary() {
        try (ZMQ.Context context = ZMQ.context(1);
             ZMQ.Socket subscriber = context.socket(ZMQ.SUB)) {
            
            subscriber.connect("tcp://central:5556");
            subscriber.subscribe("ALIVE".getBytes());

            long lastBeat = System.currentTimeMillis();
            final int TIMEOUT_MS = 15000;

            while (!isPrimary) {
                String message = subscriber.recvStr(ZMQ.DONTWAIT);
                if (message != null && message.equals("ALIVE")) {
                    lastBeat = System.currentTimeMillis();
                    System.out.println("[RÉPLICA] Heartbeat recibido");
                } else if (System.currentTimeMillis() - lastBeat > TIMEOUT_MS) {
                    System.out.println("[RÉPLICA] Alerta: conexion perdida con el servidor central. Promoviendo a primario...");
                    isPrimary = true;
                }
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startAsPrimary() {
        System.out.println("[PRIMARIO] Iniciando servicios...");
        
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

        try (ZMQ.Context context = ZMQ.context(1);
             ZMQ.Socket receiver = context.socket(ZMQ.ROUTER);
             Connection dbConn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                
                receiver.bind("tcp://*:" + PORT);
                System.out.println("[PRIMARIO] Escuchando en puerto " + PORT);

                while (true) {
                    byte[] identity = receiver.recv();
                    String request = receiver.recvStr();
                    
                    executor.submit(() -> {
                        try {
                            String response = processRequest(request, dbConn);
                            receiver.sendMore(identity);
                            receiver.send(response);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    private static String processRequest(String request, Connection dbConn) throws SQLException {
        // Ejemplo: Asignar aula
        String[] parts = request.split(":");
        String facultadId = parts[0];
        int aulasNeeded = Integer.parseInt(parts[1]);

        try (PreparedStatement stmt = dbConn.prepareStatement(
                "UPDATE aulas SET disponible = false WHERE tipo = 'AULA' AND disponible = true LIMIT ?")) {
            
            stmt.setInt(1, aulasNeeded);
            int updated = stmt.executeUpdate();

            return updated >= aulasNeeded ? "OK:" + updated : "ERROR:Recursos insuficientes";
        }
    }
}
