package com.puj.servidores;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class replica {
    private static boolean isPrimary = false;
    private static final long TIMEOUT = 10000;

    public static void main(String[] args) {
        if(args.length != 3) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <cantidad de salones disponibles> <cantidad de laboratorios disponibles> <direccion de servidor central>\n");
            System.exit(1);
        }

        final int can_Salones = Integer.parseInt(args[0]);
        final int can_Labs = Integer.parseInt(args[1]);
        final String centralIP = (args[2]);

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
        
        new Thread(() -> monitorPrimary(centralIP)).start();

        while (!isPrimary) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        startAsPrimary();
    }

    private static void monitorPrimary(String IP) {
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

                } 
                if (System.currentTimeMillis() - lastHeartbeat > TIMEOUT) {
                    System.out.println("\n||Alerta: Conexion con servidor primario perdida, cambiando estado a activo||\n");
                    isPrimary = true;
                }

                Thread.sleep(3000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void startAsPrimary() {
        System.out.println("\nTomando cargo de servidor primario...\n");

         /* 
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
            }*/
    }

    /* 
    private static String handleRequest(String message, List<String> salonesDisponibles, List<String> laboratoriosDisponibles){
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
       
        //Procesar el mensaje
        String[] parts = message.split("\\|");
        String nombre = parts[0];
        int numeroSalones = Integer.parseInt(parts[1]);
        int numeroLaboratorios = Integer.parseInt(parts[2]);
        String nombreFacultad = parts[3];
        String semestrePrograma = parts[4];

        System.out.println("\nNueva solicitud del programa " + nombre + ": " + numeroSalones + " salones; " + numeroLaboratorios + " Laboratorios.");
        System.out.println("(Semestre: " + semestrePrograma + ", facultad: " + nombreFacultad + ")\n");

        //Realizar asignacion de salones

        //Salones
        if(salonesDisponibles.size() >= numeroSalones){
            for(int i = 0; i < numeroSalones; i++){
                salonesAsignados.add(salonesDisponibles.get(0));
                salonesDisponibles.remove(0);
            }
            ClassSuccess = true;
        
        }else{
            System.out.println("Atencion: Salones insuficientes.\n");

            if(salonesDisponibles.size() != 0){
                for(int i = 0; i <= salonesDisponibles.size(); i++){
                salonesAsignados.add(salonesDisponibles.get(0));
                salonesDisponibles.remove(0);
                }
                ClassSuccess = false;
            }
            else{
                incomplete = true;
            }
            
        }
        System.out.println("Salones asignados a " + nombre +  ": " + salonesAsignados);

        //Laboratorios
        if(laboratoriosDisponibles.size() >= numeroLaboratorios){
            for(int i = 0; i < numeroLaboratorios; i++){
                laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                laboratoriosDisponibles.remove(0);
            }
            incomplete = false;
            LabSuccess = true;
        
        }else if(salonesDisponibles.size() >= numeroLaboratorios){
            System.out.println("\nAtencion: Laboratorios insuficientes, asignado laboratorios hibridos.\n");

            for(int i = 0; i < laboratoriosDisponibles.size(); i++){
                laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                laboratoriosDisponibles.remove(0);
            }

            for(int i = 0; i < numeroLaboratorios - laboratoriosAsignados.size(); i++){
                laboratoriosAsignados.add(salonesDisponibles.get(0));
                salonesDisponibles.remove(0);
            }
            incomplete = false;
            LabSuccess = true;
        }else{
            System.out.println("\nAtencion: Laboratorios insuficientes.\n");

            if(laboratoriosDisponibles.size() != 0){
                for(int i = 0; i <= laboratoriosDisponibles.size(); i++){
                    laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                    laboratoriosDisponibles.remove(0);
                }
            }
            LabSuccess = false;
        }
        System.out.println("\nLaboratorios asignados a " + nombre +  ": " + laboratoriosAsignados);

        //Estado de la peticion y registrar peticion
        if(LabSuccess && ClassSuccess){
            System.out.println("\nPeticion completada sin problemas\n");
            status = "completado";
            dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestrePrograma, nombreFacultad);
        }
        else if(incomplete){
            System.out.println("\nLa peticion no pudo ser completada.\n");
            status = "pendiente";
            dbManager.writePending(nombre, numeroSalones, numeroLaboratorios, nombreFacultad, semestrePrograma, LocalDate.now().toString());
        }
        else if(!LabSuccess || !ClassSuccess){
            System.out.println("\nLa peticion no pudo ser completada en su totalidad\n");
            status = "completado parcialmente";
            dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestrePrograma, nombreFacultad);
        }
        
        System.out.println("\nSalones disponibles: " + salonesDisponibles);
        System.out.println("Laboratorios disponibles: " + laboratoriosDisponibles);
        System.out.println("\n");
        

        //Enviar respuesta
        endTime = System.currentTimeMillis();
        responseTime = endTime - startTime;

        System.out.println("\nTiempo de respuesta: " + responseTime + " ms\n");
        getTimes(responseTime);
        
        String response = salonesAsignados + "|" + laboratoriosAsignados + "|" + status;
        return response;
    } */
}
