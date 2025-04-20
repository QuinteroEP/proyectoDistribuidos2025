package com.puj.cliente;

import com.puj.stubs.Facultad.DatosPeticion;
import com.puj.stubs.Facultad.Empty;
import com.puj.stubs.Facultad.RespuestaPeticion;
import com.puj.stubs.facultyServiceGrpc;
import com.puj.stubs.facultyServiceGrpc.facultyServiceBlockingStub;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class facultad {
    public static void main(String[] args) {
        if(args.length != 4) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <nombre del programa> <semestre> <numero de salones requeridos> <numero de laboratorios requeridos>\n");
            System.exit(1);
        }
        final String nombre = args[0];
        final String semestre = args[1];
        final int numeroSalones = Integer.parseInt(args[2]);
        final int numeroLaboratorios = Integer.parseInt(args[3]);

        String server = "192.168.10.8"; //Cambiar segun la IP del servidor
        int port = 1080;
    
        System.out.println("Conectando con el servidor " + server + " en el puerto " + port + "...");
        ManagedChannel channel = ManagedChannelBuilder.forAddress(server, port)
                .usePlaintext()
                .build();

        //Stubs
        facultyServiceBlockingStub facultyStub = facultyServiceGrpc.newBlockingStub(channel);

        //Inicializar listas de salones y laboratorios
        facultyStub.populateLists(Empty.newBuilder().build());

        //Enviar peticion de salones
        System.out.println("\nNueva peticion para el programa de " + nombre +" (semestre " + semestre + ")");
        System.out.println("Salones requeridos: " + numeroSalones);
        System.out.println("Laboratorios requeridos: " + numeroLaboratorios);
        System.out.println("\n");

        DatosPeticion request = DatosPeticion.newBuilder()
                .setSemestre(semestre)
                .setFacultad(nombre)
                .setPrograma("Programacion")
                .setSalones(numeroSalones)
                .setLaboratorios(numeroLaboratorios)
                .build();

        System.out.println("Peticion de salones enviada al servidor. Esperando respuesta...\n");
        RespuestaPeticion respuestaPeticion = facultyStub.solicitarSalon(request);
        if(respuestaPeticion.getStatus() == 200){
            System.out.println("Lo sentimos, no hay suficientes salones y laboratorios disponibles para el programa de " + nombre + "\n");
        }
        else{
            if(respuestaPeticion.getSalonCount() > 0){
                System.out.println("Salones asignados:");
                for (int i = 0; i < respuestaPeticion.getSalonCount(); i++) {
                    System.out.println(respuestaPeticion.getSalon(i));
                }
                System.out.println("\n");
            }
    
            if(respuestaPeticion.getLabCount() > 0){
                System.out.println("Laboratorios asignados:");
                for (int i = 0; i < respuestaPeticion.getLabCount(); i++) {
                    System.out.println( respuestaPeticion.getLab(i));
                }
                System.out.println("\n");
            }
    
            if(respuestaPeticion.getHybridCount() > 0){
                System.out.println("No hay suficientes laboratorios disponibles. Se han asignado los siguientes salones como laboratorios moviles:");
                for (int i = 0; i < respuestaPeticion.getHybridCount(); i++) {
                    System.out.println( respuestaPeticion.getHybrid(i));
                }
                System.out.println("\n");
            }
        }
        
        // Cerrar el canal
        channel.shutdown();
    }
}