package com.puj.servidores;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.puj.dbManager;

public class CentralWorker {
    private static List<String> salonesDisponibles = new ArrayList<>();
    private static List<String> laboratoriosDisponibles = new ArrayList<>();

    // CORREGIDO: Este método ahora es público
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Uso: <cantidad_salones> <cantidad_laboratorios>");
            System.exit(1);
        }

        int cantidadSalones = Integer.parseInt(args[0]);
        int cantidadLaboratorios = Integer.parseInt(args[1]);

        // Inicializar recursos
        for (int i = 1; i <= cantidadSalones; i++) {
            salonesDisponibles.add("Salón " + i);
        }
        for (int i = 1; i <= cantidadLaboratorios; i++) {
            laboratoriosDisponibles.add("Laboratorio " + i);
        }

        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(ZMQ.REP);
            socket.connect("tcp://broker:5556"); // importante usar nombre de servicio Docker
            System.out.println("[WORKER] Conectado al broker (5556)");

            while (!Thread.currentThread().isInterrupted()) {
                String request = socket.recvStr();
                System.out.println("\n[WORKER] Solicitud recibida: " + request);

                String response = handleRequest(request);

                socket.send(response);
                System.out.println("[WORKER] Respuesta enviada.\n");
            }
        }
    }

    private static synchronized String handleRequest(String message) {
        List<String> salonesAsignados = new ArrayList<>();
        List<String> laboratoriosAsignados = new ArrayList<>();
        String status;

        boolean classSuccess = true;
        boolean labSuccess = true;
        boolean incomplete = false;

        String[] parts = message.split("\\|");
        String nombre = parts[0];
        int numSalones = Integer.parseInt(parts[1]);
        int numLabs = Integer.parseInt(parts[2]);
        String facultad = parts[3];
        String semestre = parts[4];

        System.out.println("Asignando a: " + nombre + " (" + facultad + ")");

        // Salones
        if (salonesDisponibles.size() >= numSalones) {
            for (int i = 0; i < numSalones; i++) {
                salonesAsignados.add(salonesDisponibles.remove(0));
            }
        } else if (!salonesDisponibles.isEmpty()) {
            for (int i = 0; i < salonesDisponibles.size(); i++) {
                salonesAsignados.add(salonesDisponibles.remove(0));
            }
            classSuccess = false;
        } else {
            incomplete = true;
        }

        // Laboratorios
        if (laboratoriosDisponibles.size() >= numLabs) {
            for (int i = 0; i < numLabs; i++) {
                laboratoriosAsignados.add(laboratoriosDisponibles.remove(0));
            }
        } else if (salonesDisponibles.size() >= numLabs) {
            for (int i = 0; i < laboratoriosDisponibles.size(); i++) {
                laboratoriosAsignados.add(laboratoriosDisponibles.remove(0));
            }
            for (int i = 0; i < numLabs - laboratoriosAsignados.size(); i++) {
                laboratoriosAsignados.add(salonesDisponibles.remove(0));
            }
        } else if (!laboratoriosDisponibles.isEmpty()) {
            for (int i = 0; i < laboratoriosDisponibles.size(); i++) {
                laboratoriosAsignados.add(laboratoriosDisponibles.remove(0));
            }
            labSuccess = false;
        } else {
            labSuccess = false;
        }

        // Estado y escritura en BD
        if (labSuccess && classSuccess) {
            status = "completado";
            dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestre, facultad,
                    LocalDate.now().toString());
        } else if (incomplete) {
            status = "pendiente";
            dbManager.writePending(nombre, numSalones, numLabs, facultad, semestre, LocalDate.now().toString());
        } else {
            status = "completado parcialmente";
            dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestre, facultad,
                    LocalDate.now().toString());
        }

        return salonesAsignados + "|" + laboratoriosAsignados + "|" + status;
    }
}
