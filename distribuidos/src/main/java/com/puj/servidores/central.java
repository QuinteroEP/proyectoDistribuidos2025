package com.puj.servidores;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class central {
    //Tiempos de respuets
    public static List<Long> tiempos = new ArrayList<>();
    public static long runningTimeTotal = 0;
    
    public static void main(String[] args){
        //Laboratorios y salones
        List<String> salones = new ArrayList<>();
        List<String> laboratorios = new ArrayList<>();

        //Crear Socket
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.ROUTER);
            socket.bind("tcp://*:1090");
            System.out.println("\nServidor central abierto en el puerto 1090...");

            //Popular listas de salones
            for(int i = 1; i <= 20; i++){
                String n = String.valueOf(i);
                String s = n + "S";

                salones.add(s);
            }

            for(int i = 1; i <= 10; i++){
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
                byte[] empty = socket.recv(0);  
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

        System.out.println("\nNueva solicitud del programa " + nombre + ": " + numeroSalones + " salones; " + numeroLaboratorios + " Laboratorios.\n");

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

        //Estado de la peticion
        if(LabSuccess && ClassSuccess){
            System.out.println("\nPeticion completada sin problemas\n");
            status = "completado";
        }
        else if(incomplete){
            System.out.println("\nLa peticion no pudo ser completada.\n");
            status = "pendiente";
        }
        else if(!LabSuccess || !ClassSuccess){
            System.out.println("\nLa peticion no pudo ser completada en su totalidad\n");
            status = "completado parcialmente";
        }
        
        System.out.println("\nSalones disponibles: " + salonesDisponibles);
        System.out.println("Laboratorios disponibles: " + laboratoriosDisponibles);
        System.out.println("\n");
        
        //Escribir al archivo
        writeToFile(nombre, numeroSalones, numeroLaboratorios, status);

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

    private static void writeToFile(String nombre, int Salones, int Laboratorios, String status){
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode jsonNode = objectMapper.createObjectNode();
        jsonNode.put("Programa academico", nombre);
        jsonNode.put("Salones pedidos", Salones);
        jsonNode.put("Laboratorios pedidos", Laboratorios);
        jsonNode.put("Estado", status);

        File file = new File("asignacionSalones.json");
        ArrayNode rootArray;

        try {
            // If file exists, read it as an array
            if (file.exists() && file.length() > 0) {
                rootArray = (ArrayNode) objectMapper.readTree(file);
            } else {
                // If file doesn't exist or is empty, create a new array
                rootArray = objectMapper.createArrayNode();
            }
    
            rootArray.add(jsonNode);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, rootArray);
    
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}