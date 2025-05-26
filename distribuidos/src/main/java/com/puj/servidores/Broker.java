package com.puj.servidores;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import com.puj.dbManager;

public class Broker {
   public static dbManager manager;
   public static List<Long> tiempos = new ArrayList<>();
   public static long runningTimeTotal = 0;

   public static void main(String[] args) {
      if (args.length != 3) {
         System.out.println("Uso: <cantidad_salones> <cantidad_laboratorios> <cantidad de trabajadores>");
         System.exit(1);
      }

      int cantidadSalones = Integer.parseInt(args[0]);
      int cantidadLaboratorios = Integer.parseInt(args[1]);
      int workers = Integer.parseInt(args[2]);

      List<String> salonesDisponibles = Collections.synchronizedList(new ArrayList<>());
      List<String> laboratoriosDisponibles = Collections.synchronizedList(new ArrayList<>());

      // Requiere: puerto para recibir (desde facultades) y enviar (a workers)
      final String frontendPort = "1090"; // Facultad -> Broker
      final String backendPort = "2091"; // Broker -> Worker

      ZContext context = new ZContext();
      try {
         // ROUTER: recibe desde las facultades
         ZMQ.Socket frontend = context.createSocket(SocketType.ROUTER);
         frontend.bind("tcp://*:" + frontendPort);
         System.out.println("\n[Broker] Escuchando las solicitudes de facultades en puerto " + frontendPort);

         // DEALER: envia a los workers
         ZMQ.Socket backend = context.createSocket(SocketType.ROUTER);
         backend.bind("tcp://*:" + backendPort);
         System.out.println("\n[Broker] Enrutando solicitudes a los workers en puerto " + backendPort);

         String addressBackup = "tcp://*:1092";
         ZMQ.Socket BackupSocket = context.createSocket(SocketType.PUB);
         BackupSocket.bind(addressBackup);
         System.out.println("\n[Broker] Puerto 1092 para heartbeats abierto");

         String addressBackupUpdate = "tcp://*:1093";
         ZMQ.Socket BackupUpdateSocket = context.createSocket(SocketType.PUB);
         BackupUpdateSocket.bind(addressBackupUpdate);
         System.out.println("\n[Broker] Puerto 1093 para actualizaciones de replica abierto\n");

         // Inicializar recursos
         for (int i = 1; i <= cantidadSalones; i++) {
            String n = String.valueOf(i);
            String s = n + "S";

            salonesDisponibles.add(s);
         }

         for (int i = 1; i <= cantidadLaboratorios; i++) {
            String n = String.valueOf(i);
            String l = n + "L";

            laboratoriosDisponibles.add(l);
         }

         // Proceso para heartbeats
         new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
               BackupSocket.send("HEARTBEAT");
               try {
                  Thread.sleep(3000);
               } catch (InterruptedException e) {
                  e.printStackTrace();
               }
            }
         }).start();

         for (int workerNbr = 0; workerNbr < workers; workerNbr++) {
            Thread worker = new Worker(salonesDisponibles, laboratoriosDisponibles, backendPort,
                  BackupUpdateSocket);
            worker.start();
         }

         Queue<ZFrame> availableWorkers = new LinkedList<>();
         Map<String, ZFrame> workerToClient = new HashMap<>();

         ZMQ.Poller poller = context.createPoller(2);
         poller.register(frontend, ZMQ.Poller.POLLIN);
         poller.register(backend, ZMQ.Poller.POLLIN);

         while (!Thread.currentThread().isInterrupted()) {
            poller.poll();

            if (poller.pollin(0)) {
               ZMsg clientMsg = ZMsg.recvMsg(frontend);
               System.out.println("\n[Broker] Nueva peticion, enviando a trabajadores...\n");

               if (!availableWorkers.isEmpty()) {
                  ZFrame workerId = availableWorkers.poll(); // Get the next available worker

                  ZMsg msgToWorker = new ZMsg();
                  msgToWorker.add(workerId);
                  msgToWorker.add("");

                  ZFrame clientId = clientMsg.pop();
                  workerToClient.put(workerId.toString(), clientId.duplicate());
;
                  clientMsg.pop(); 

                  msgToWorker.append(clientMsg);
                  msgToWorker.send(backend);
               }

            }
            if (poller.pollin(1)) {
               ZMsg backendMsg = ZMsg.recvMsg(backend);
               //System.out.println("\nMensaje: " + backendMsg.toString());

               ZFrame workerId = backendMsg.pop();
               backendMsg.pop();
               ZFrame lastPart = backendMsg.pop();

               availableWorkers.add(workerId.duplicate());

               if ("READY".equals(lastPart.toString())) {
                  availableWorkers.add(workerId.duplicate());
                  System.out.println("\nWorker: " + lastPart.toString());
               } else {
                  ZFrame clientId = workerToClient.remove(workerId.toString());

                  //System.out.println("\nRespuesta: " + lastPart.toString());
                  ZMsg responseToClient = new ZMsg();
                  responseToClient.add(clientId);
                  responseToClient.add("");
                  responseToClient.add(lastPart);

                  responseToClient.send(frontend);
                  System.out.println("\n[Broker] Respuesta enviada al cliente");
               }
            }

         }

      } catch (Exception e) {
         e.printStackTrace();
      }
   }

   private static class Worker extends Thread {
      ZMQ.Socket updateSocket;

      private final List<String> salonesDisponibles;
      private final List<String> laboratoriosDisponibles;

      List<String> salonesAsignados = new ArrayList<>();
      List<String> laboratoriosAsignados = new ArrayList<>();
      String status = "";

      Boolean ClassSuccess = true;
      Boolean LabSuccess = true;
      Boolean incomplete = false;

      long startTime;
      long endTime;
      long responseTime;

      String backendPort;

      public Worker(List<String> salones, List<String> labs, String backendPort, ZMQ.Socket updateSocket) {

         this.salonesDisponibles = salones;
         this.laboratoriosDisponibles = labs;
         this.backendPort = backendPort;
         this.updateSocket = updateSocket;
      }

      @Override
      public void run() {
         try (ZContext context = new ZContext()) {
            ZMQ.Socket worker = context.createSocket(SocketType.REQ);
            worker.connect("tcp://localhost:" + backendPort);

            worker.send("READY");

            while (!Thread.currentThread().isInterrupted()) {
               ZMsg msg = ZMsg.recvMsg(worker);

               startTime = System.currentTimeMillis();
               System.out.println("\n[Worker] Tarea recibida\n");
               ZFrame payload = msg.pop();
               String request = payload.getString(ZMQ.CHARSET);

               String[] parts = request.split("\\|");
               String nombre = parts[0];
               int numeroSalones = Integer.parseInt(parts[1]);
               int numeroLaboratorios = Integer.parseInt(parts[2]);
               String nombreFacultad = parts[3];
               String semestrePrograma = parts[4];

               System.out.println("\n[Worker] Nueva solicitud del programa " + nombre + ": " + numeroSalones
                     + " salones; " + numeroLaboratorios + " Laboratorios. (Semestre: " + semestrePrograma
                     + ", facultad: " + nombreFacultad + ")\n");

               // Realizar asignacion de salones

               // Salones
               if (salonesDisponibles.size() >= numeroSalones) {
                  for (int i = 0; i < numeroSalones; i++) {
                     salonesAsignados.add(salonesDisponibles.get(0));
                     salonesDisponibles.remove(0);
                  }
                  ClassSuccess = true;
               } else {
                  System.out.println("[Worker] Atencion: Salones insuficientes.\n");

                  if (salonesDisponibles.size() != 0) {
                     for (int i = 0; i <= salonesDisponibles.size(); i++) {
                        salonesAsignados.add(salonesDisponibles.get(0));
                        salonesDisponibles.remove(0);
                     }
                     ClassSuccess = false;
                  } else {
                     incomplete = true;
                  }

               }
               System.out.println("[Worker] Salones asignados a " + nombre + ": " + salonesAsignados);

               // Laboratorios
               if (laboratoriosDisponibles.size() >= numeroLaboratorios) {
                  for (int i = 0; i < numeroLaboratorios; i++) {
                     laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                     laboratoriosDisponibles.remove(0);
                  }
                  incomplete = false;
                  LabSuccess = true;

               } else if (salonesDisponibles.size() >= numeroLaboratorios) {
                  System.out.println("\n[Worker] Atencion: Laboratorios insuficientes, asignado laboratorios hibridos.\n");

                  for (int i = 0; i < laboratoriosDisponibles.size(); i++) {
                     laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                     laboratoriosDisponibles.remove(0);
                  }

                  for (int i = 0; i < numeroLaboratorios - laboratoriosAsignados.size(); i++) {
                     laboratoriosAsignados.add(salonesDisponibles.get(0));
                     salonesDisponibles.remove(0);
                  }
                  incomplete = false;
                  LabSuccess = true;
               } else {
                  System.out.println("\n[Worker] Atencion: Laboratorios insuficientes.\n");

                  if (laboratoriosDisponibles.size() != 0) {
                     for (int i = 0; i <= laboratoriosDisponibles.size(); i++) {
                        laboratoriosAsignados.add(laboratoriosDisponibles.get(0));
                        laboratoriosDisponibles.remove(0);
                     }
                  }
                  LabSuccess = false;
               }
               System.out.println("\n[Worker] Laboratorios asignados a " + nombre + ": " + laboratoriosAsignados);

               if (LabSuccess && ClassSuccess) {
                  System.out.println("\n[Worker] Peticion completada sin problemas\n");
                  status = "completado";
                  dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestrePrograma,
                        nombreFacultad, LocalDate.now().toString());
               } else if (incomplete) {
                  System.out.println("\n[Worker] La peticion no pudo ser completada.\n");
                  status = "pendiente";
                  dbManager.writePending(nombre, numeroSalones, numeroLaboratorios, nombreFacultad, semestrePrograma,
                        LocalDate.now().toString());
               } else if (!LabSuccess || !ClassSuccess) {
                  System.out.println("\n[Worker] La peticion no pudo ser completada en su totalidad\n");
                  status = "completado parcialmente";
                  dbManager.writeAsign(nombre, salonesAsignados, laboratoriosAsignados, status, semestrePrograma,
                        nombreFacultad, LocalDate.now().toString());
               }

               endTime = System.currentTimeMillis();
               responseTime = endTime - startTime;

               System.out.println("\nSalones disponibles: " + salonesDisponibles);
               System.out.println("Laboratorios disponibles: " + laboratoriosDisponibles);
               System.out.println("\n");

               System.out.println("\n[Worker] Tiempo de respuesta: " + responseTime + " ms\n");
               getTimes(responseTime);

               new Thread(() -> {
                  System.out.println("Actualizando backup");
                  byte[] bytes_salones = ByteBuffer.allocate(4).putInt(salonesDisponibles.size()).array();
                  byte[] bytes_labs = ByteBuffer.allocate(4).putInt(laboratoriosDisponibles.size()).array();

                  updateSocket.sendMore("UPDATE");
                  updateSocket.sendMore(bytes_salones);
                  updateSocket.send(bytes_labs);
               }).start();

               ZMsg reply = new ZMsg();
               reply.addString(salonesAsignados + "|" + laboratoriosAsignados + "|" + status);
               reply.send(worker);
            }
         }
      }
   }

   public static void getTimes(Long t) {
      long maxTime = 0;
      long minTime = 1000;
      long promedio = 0;

      runningTimeTotal = runningTimeTotal + t;
      tiempos.add(t);

      if (tiempos.size() == 5) {
         // Tiempo maximo
         for (int i = 0; i < tiempos.size(); i++) {
            if (tiempos.get(i) > maxTime) {
               maxTime = tiempos.get(i);
            }
         }

         // Tiempo minimo
         for (int i = 0; i < tiempos.size(); i++) {
            if (tiempos.get(i) < minTime) {
               minTime = tiempos.get(i);
            }
         }

         promedio = runningTimeTotal / 5;

         System.out.println("\nTiempo minimo de respueta: " + minTime + "ms");
         System.out.println("Tiempo maximo de respueta: " + maxTime + "ms");
         System.out.println("Tiempo promedio de respueta: " + promedio + "ms\n");
      }
   }
}
