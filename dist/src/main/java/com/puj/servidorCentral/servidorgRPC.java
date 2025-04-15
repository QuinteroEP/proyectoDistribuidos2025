package com.puj.servidorCentral;

import java.util.ArrayList;
import java.util.List;

import com.puj.stubs.Facultad.DatosPeticion;
import com.puj.stubs.Facultad.Empty;
import com.puj.stubs.Facultad.RespuestaPeticion;
import com.puj.stubs.facultyServiceGrpc;

import io.grpc.stub.StreamObserver;

public class servidorgRPC extends facultyServiceGrpc.facultyServiceImplBase {
    
    //Listas de salones y laboratios
    List<Integer> salones = new ArrayList<>();
    List<Integer> laboratorios = new ArrayList<>();

    //Tiempos de respuets
    long startTime;
    long endTime;
    long responseTime;

    @Override
    public void populateLists(Empty request, StreamObserver<Empty> responseObserver){
        for(int i = 1; i <= 10; i++){
            salones.add(i);
        }
        for(int i = 1; i <= 5; i++){
            laboratorios.add(i);
        }

        System.out.println("Listas de salones generadas.\n");
        System.out.println("Salones disponibles: " + salones);
        System.out.println("Laboratorios disponibles: " + laboratorios);
        System.out.println("\n");

        responseObserver.onNext(Empty.newBuilder().build());
        responseObserver.onCompleted();
    }

    @Override
    public void solicitarSalon(DatosPeticion request, StreamObserver<RespuestaPeticion> responseObserver){
        startTime = System.currentTimeMillis();

        System.out.println("Nueva peticion de la facultad de " + request.getFacultad() + " para el programa " + request.getPrograma() + " (semestre " + request.getSemestre() + ")");
        System.out.println("Salones requeridos: " + request.getSalones());
        System.out.println("Laboratorios requeridos: " + request.getLaboratorios() + "\n");

        //Asignar salones
        List<Integer> salonesAsignados = new ArrayList<>();

        if(salones.size() >= request.getSalones()){
            for(int i = 0; i < request.getSalones(); i++){
                salonesAsignados.add(salones.get(0));
                salones.remove(0);
            }
            System.out.println("Salones asignados: " + salonesAsignados);
        
        }else{
            System.out.println("Error: Salones insuficientes.\n");
        }

        //Asignar laboratorios
        List<Integer> laboratiosAsignados = new ArrayList<>();

        if(laboratorios.size() >= request.getLaboratorios()){
            for(int i = 0; i < request.getLaboratorios(); i++){
                laboratiosAsignados.add(laboratorios.get(0));
                laboratorios.remove(0);
            }
            System.out.println("Laboratorios asignados: " + laboratiosAsignados);
        
        }
        else if (laboratorios.size() <= request.getLaboratorios() && salones.size() >= request.getLaboratorios()){
            System.out.println("Error: Laboratorios insuficientes, asignando laboratorios mobiles.\n");
        }
        
        else{
            System.out.println("Error: Laboratorios insuficientes.\n");
        }
        RespuestaPeticion response = RespuestaPeticion.newBuilder()
            .addAllSalon(salonesAsignados) // Add the list of assigned salones
            .addAllLab(laboratiosAsignados) // Add the list of assigned labs
            .build();

        endTime = System.currentTimeMillis();
        responseTime = endTime - startTime;

        System.out.println("Tiempo de respuesta: " + responseTime + " ms\n");
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
