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

    boolean success = false;
    boolean mobiles = false;

    boolean initiated = false;

    //Tiempos de respuets
    long startTime;
    long endTime;
    long responseTime;

    List<Long> tiempos = new ArrayList<>();
    long runningTimeTotal = 0;

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
            success = true;
        
        }else{
            System.out.println("Error: Salones insuficientes.\n");
        }

        //Asignar laboratorios
        List<String> laboratiosAsignados = new ArrayList<>();

        if(laboratorios.size() >= request.getLaboratorios()){
            for(int i = 0; i < request.getLaboratorios(); i++){
                laboratiosAsignados.add(laboratorios.get(0));
                laboratorios.remove(0);
            }
            System.out.println("Laboratorios asignados: " + laboratiosAsignados);
            success = true;
        }

        //Hibridos
        else if (laboratorios.size() <= request.getLaboratorios() && salones.size() >= request.getLaboratorios()){
            int r = 0;
            int remaining = laboratorios.size();

            System.out.println("Alerta: Laboratorios insuficientes, asignando laboratorios normales y mobiles.\n");
            for(int i = 0; i < remaining; i++){
                laboratiosAsignados.add(laboratorios.get(0));
                laboratorios.remove(0);
                r++;
            }
            if(laboratiosAsignados.size() > 0){
                System.out.println("Laboratorios asignados: " + laboratiosAsignados);
            }

            for(int i = 0; i < request.getLaboratorios() - r; i++){
                laboratoriosMobiles.add(salones.get(0));
                salones.remove(0);
            }
            System.out.println("Laboratorios moviles asignados: " + laboratoriosMobiles + "\n");
            mobiles = true;
            success = true;
        }
        else{
            System.out.println("Error: Laboratorios insuficientes.\n");
            success = false;
        }

        //Codigos de estado
        if(success == false){
            mensaje = "La peticion no pude ser completada.";
            status = 200;
        }

        else if (success && mobiles){
            mensaje = "La peticion fue completada. Laboratorios mobiles asignados";
            status = 101;
        }

        else if (success){
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

        //Metricas de tiempo
        runningTimeTotal += responseTime;
        tiempos.add(responseTime);

        if(tiempos.size() == 5){
            System.out.println("Tiempo de respuesta promedio: " + (runningTimeTotal / tiempos.size()) + " ms\n");
            getMaxTime(tiempos);
            getMinTime(tiempos);

            tiempos.clear();
            runningTimeTotal = 0;
        }

        responseObserver.onNext(response);
        responseObserver.onCompleted();
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
