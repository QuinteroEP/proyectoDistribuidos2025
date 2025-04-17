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
    List<String> salones = new ArrayList<>();
    List<String> laboratorios = new ArrayList<>();
    List<String> laboratoriosMobiles = new ArrayList<>();

    String mensaje = null;
    Integer status = null;

    boolean classSuccess = false;
    boolean labSuccess = false;
    boolean mobiles = false; 

    boolean initiated = false;

    //Tiempos de respuets
    long startTime;
    long endTime;
    long responseTime;

    @Override
    public void populateLists(Empty request, StreamObserver<Empty> responseObserver){
        if(initiated == false){
            for(int i = 1; i <= 10; i++){
                String n = String.valueOf(i);
                String s = n + "S";

                salones.add(s);
            }
            for(int i = 1; i <= 5; i++){
                String n = String.valueOf(i);
                String l = n + "L";

                laboratorios.add(l);
            }
            System.out.println("Listas de salones generadas.\n");
            initiated = true;
        }
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
        List<String> salonesAsignados = new ArrayList<>();

        if(salones.size() >= request.getSalones()){
            for(int i = 0; i < request.getSalones(); i++){
                salonesAsignados.add(salones.get(0));
                salones.remove(0);
            }
            System.out.println("Salones asignados: " + salonesAsignados);
            classSuccess = true;
        
        }else{
            System.out.println("Error: Salones insuficientes.\n");
            classSuccess = false;
        }

        //Asignar laboratorios
        List<String> laboratiosAsignados = new ArrayList<>();

        if(laboratorios.size() >= request.getLaboratorios()){
            for(int i = 0; i < request.getLaboratorios(); i++){
                laboratiosAsignados.add(laboratorios.get(0));
                laboratorios.remove(0);
            }
            System.out.println("Laboratorios asignados: " + laboratiosAsignados);
            labSuccess = true;
        
        }

        //Hibridos
        else if (laboratorios.size() <= request.getLaboratorios() && salones.size() >= request.getLaboratorios()){
            int r = 0;
            System.out.println("Error: Laboratorios insuficientes, asignando laboratorios mobiles.\n");
            for(int i = 0; i <= laboratorios.size(); i++){
                laboratiosAsignados.add(laboratorios.get(0));
                laboratorios.remove(0);
                r++;
            }
            
            for(int i = 0; i < request.getLaboratorios() - r; i++){
                laboratoriosMobiles.add(salones.get(0));
                salones.remove(0);
            }
            System.out.println("Laboratorios moviles asignados: " + laboratoriosMobiles + "\n");
            mobiles = true;
            labSuccess = true;
        }
        
        else{
            System.out.println("Error: Laboratorios insuficientes.\n");
            labSuccess = false;
        }

        //Codigos de estado
        if(classSuccess == false && labSuccess == false){
            mensaje = "La peticion no pude ser completada.";
            status = 300;
        }

        else if(classSuccess && labSuccess == false){
            mensaje = "La peticion no pude ser completada en su totalidad. Laboratorios insuficientes";
            status = 201;
        }

        else if(classSuccess == false && labSuccess){
            mensaje = "La peticion no pude ser completada en su totalidad. Salones insuficientes";
            status = 202;
        }

        else if (classSuccess && labSuccess && mobiles == false){
            mensaje = "La peticion fue completada. Laboratorios asignados";
            status = 101;
        }

        else if (classSuccess && mobiles){
            mensaje = "La peticion fue completada. Laboratorios mobiles asignados";
            status = 102;
        }

        else if (classSuccess && labSuccess){
            mensaje = "La peticion fue completada sin problemas";
            status = 100;
        }

        //Enviar respuesta
        RespuestaPeticion response = RespuestaPeticion.newBuilder()
            .addAllSalon(salonesAsignados) // Add the list of assigned salones
            .addAllLab(laboratiosAsignados) // Add the list of assigned labs
            .addAllHybrid(laboratoriosMobiles) // Add the list of assigned mobile labs
            .setMensaje(mensaje) // Set the status message
            .setStatus(status) // Set the status code
            .build();

        endTime = System.currentTimeMillis();
        responseTime = endTime - startTime;

        System.out.println("Tiempo de respuesta: " + responseTime + " ms\n");
        System.out.println("Estado de la peticion: " + status + " (" + mensaje + ")\n");

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
