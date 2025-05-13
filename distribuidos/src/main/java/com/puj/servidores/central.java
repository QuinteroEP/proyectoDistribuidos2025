package com.puj.servidores;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDate;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.puj.dbManager;

public class central {
    //Tiempos de respuets
    public static List<Long> tiempos = new ArrayList<>();
    public static long runningTimeTotal = 0;
    public static dbManager manager;
    
    public static void main(String[] args) throws UnknownHostException{
        if(args.length != 2) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <cantidad de salones disponibles> <cantidad de laboratorios disponibles>\n");
            System.exit(1);
        }

        final int can_Salones = Integer.parseInt(args[0]);
        final int can_Labs = Integer.parseInt(args[1]);

        //Laboratorios y salones
        List<String> salones = new ArrayList<>();
        List<String> laboratorios = new ArrayList<>();

        //Crear Socket
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.ROUTER);
            socket.bind("tcp://*:1090");
            System.out.println("\nServidor central abierto en el puerto 1090. Servidor " + InetAddress.getLocalHost() + "\n");

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

            System.out.println("Salones disponibles: " + salones);
            System.out.println("Laboratorios disponibles: " + laboratorios);
            System.out.println("\n");

            System.out.println("Esperando peticiones...\n");

            //Crear trabajadores
            ExecutorService executor = Executors.newCachedThreadPool();

            //Recibir mensajes
            while (!Thread.currentThread().isInterrupted()) {
                byte[] id = socket.recv(0); 
                socket.recv(0);  
                byte[] request = socket.recv(0);

                String message = new String(request, ZMQ.CHARSET);

                //Enviar peticion a trabajador
                executor.submit(() -> {
                    String reply = handleRequest(message, salones, laboratorios);
                    socket.sendMore(id);
                    socket.sendMore(""); // empty delimiter
                    socket.send(reply);
                });
            }
        }
    }

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
    }

    public static void getTimes(Long t){
        long maxTime = 0;
        long minTime = 1000;
        long promedio = 0;

        runningTimeTotal = runningTimeTotal + t;
        tiempos.add(t);

        if(tiempos.size() == 5){
            //Tiempo maximo
            for(int i = 0; i < tiempos.size(); i++){
                if(tiempos.get(i) > maxTime){
                    maxTime = tiempos.get(i);
                }
            }

            //Tiempo minimo
            for(int i = 0; i < tiempos.size(); i++){
                if(tiempos.get(i) < minTime){
                    minTime = tiempos.get(i);
                }
            }

            promedio = runningTimeTotal/5;

            System.out.println("\nTiempo minimo de respueta: " + minTime + "ms");
            System.out.println("Tiempo maximo de respueta: " + maxTime + "ms");
            System.out.println("Tiempo promedio de respueta: " + promedio + "ms\n");
        }
    }
}