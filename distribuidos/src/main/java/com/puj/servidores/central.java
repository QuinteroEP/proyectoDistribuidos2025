package com.puj.servidores;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class central {
    public static void main(String[] args){
        //Laboratorios y salones
        List<String> salones = new ArrayList<>();
        List<String> laboratorios = new ArrayList<>();
        List<String> laboratoriosMobiles = new ArrayList<>();

        //Tiempos de respuets
        List<Long> tiempos = new ArrayList<>();
        long runningTimeTotal = 0;

        //Crear Socket
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://*:1080");
            System.out.println("\nServidor central abierto en el puerto 1080...");

            //Generar listas de salones
            for(int i = 1; i <= 20; i++){
                String n = String.valueOf(i);
                String s = n + "S";

                salones.add(s);
            }

            for(int i = 1; i <= 20; i++){
                String n = String.valueOf(i);
                String l = n + "L";

                laboratorios.add(l);
            }

            System.out.println("Listas de salones generadas.\n");

            System.out.println("Salones disponibles: " + salones);
            System.out.println("Laboratorios disponibles: " + laboratorios);
            System.out.println("\n");

            System.out.println("Esperando peticiones...\n");

            //Crear trabajadores
            ExecutorService executor = Executors.newCachedThreadPool();

            while (!Thread.currentThread().isInterrupted()) {
                byte[] request = socket.recv(0);
                String message = new String(request, ZMQ.CHARSET);

                //Enviar peticion a trabajador
                executor.submit(() -> {
                    handleRequest(message, salones, laboratorios, socket);
                });
            }
        }
    }

    private static void handleRequest(String message, List<String> salonesDisponibles, List<String> laboratoriosDisponibles, ZMQ.Socket socket){
        List<String> salonesAsignados = new ArrayList<>();
        List<String> laboratoriosAsignados = new ArrayList<>();
        
        long startTime;
        long endTime;
        long responseTime;

        startTime = System.currentTimeMillis();
       
        //Procesar el mensaje
        String[] parts = message.split("\\|");
        String nombre = parts[0];
        int numeroSalones = Integer.parseInt(parts[1]);
        int numeroLaboratorios = Integer.parseInt(parts[2]);

        System.out.println("Nueva solicitud del programa " + nombre + ": " + numeroSalones + " salones; " + numeroLaboratorios + " Laboratorios.\n");

        //Realizar asignacion de salones

        //Salones
        if(salonesDisponibles.size() >= numeroSalones){
            for(int i = 0; i < numeroSalones; i++){
                salonesAsignados.add(salonesDisponibles.get(0));
                salonesDisponibles.remove(0);
            }
            System.out.println("\nSalones asignados a " + nombre +  ": " + salonesAsignados);
        
        }else{
            System.out.println("Error: Salones insuficientes.\n");
        }

        //Laboratorios
        if(laboratoriosDisponibles.size() >= numeroLaboratorios){
            for(int i = 0; i < numeroLaboratorios; i++){
                laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                laboratoriosDisponibles.remove(0);
            }
            System.out.println("\nLaboratorios asignados a " + nombre +  ": " + laboratoriosAsignados);
        
        }else{
            System.out.println("Error: Salones insuficientes.\n");
        }
        
        //Enviar respuesta
        endTime = System.currentTimeMillis();
        responseTime = endTime - startTime;

        System.out.println("\nTiempo de respuesta: " + responseTime + " ms\n");
        String response = "ack";
        socket.send(response.getBytes(ZMQ.CHARSET), 0);
    }

    public void getMaxTime(List<Long> tiempos){
        long maxTime = 0;

        for(int i = 0; i < tiempos.size(); i++){
            if(tiempos.get(i) > maxTime){
                maxTime = tiempos.get(i);
            }
        }

        System.out.println("Tiempo de respuesta maximo: " + maxTime + " ms\n");
    }

    public void getMinTime(List<Long> tiempos){
        long minTime = 1000;

        for(int i = 0; i < tiempos.size(); i++){
            if(tiempos.get(i) < minTime){
                minTime = tiempos.get(i);
            }
        }

        System.out.println("Tiempo de respuesta minimo: " + minTime + " ms\n");
    }
}