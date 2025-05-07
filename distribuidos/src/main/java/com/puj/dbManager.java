package com.puj;

import org.bson.Document;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;

import java.util.List;


public class dbManager {
    public static void main( String[] args ) {
        String uri = "mongodb+srv://pabloqc:Moira24@clusterproyecto.nr6bmkl.mongodb.net/?retryWrites=true&w=majority&appName=ClusterProyecto";
        Document ping = new Document("ping", 1);

        try (MongoClient mongoClient = MongoClients.create(uri)) {
            System.out.println("\nPing hacia la base de datos...");
            Document response = mongoClient.getDatabase("Asignaciones").runCommand(ping);
            System.out.println("Respuesta: " + response.toJson(JsonWriterSettings.builder().indent(true).build()) + "\n");
            while(true){}
        }
    }

    public static void writeAsign(String nombre, List<String> Salones, List<String> Laboratorios, String status, String semestre, String facultad){
        String uri = "mongodb+srv://pabloqc:Moira24@clusterproyecto.nr6bmkl.mongodb.net/?retryWrites=true&w=majority&appName=ClusterProyecto";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            System.out.println("\nConectado a la base de datos.\n");

            MongoDatabase database = mongoClient.getDatabase("Asignaciones");
            MongoCollection<Document> transactions = database.getCollection("Transacciones");

            Document registro = new Document("_id", new ObjectId());
            registro.append("Programa academico", nombre)
                    .append("Semestre: ", semestre)
                    .append("Facultad: ", facultad)
                    .append("Salones asginados", Salones)
                    .append("Laboratorios asginados", Laboratorios)
                    .append("Estado", status);

            transactions.insertOne(registro);
            System.out.println("\nBase de datos actualizada.\n");
        } 
    }

    public static void writePending(String nombre, int Salones, int Laboratorios, String facultad, String semestre, String fecha){
        String uri = "mongodb+srv://pabloqc:Moira24@clusterproyecto.nr6bmkl.mongodb.net/?retryWrites=true&w=majority&appName=ClusterProyecto";
        try (MongoClient mongoClient = MongoClients.create(uri)) {
            System.out.println("\nConectado a la base de datos.\n");

            MongoDatabase database = mongoClient.getDatabase("Asignaciones");
            MongoCollection<Document> transactions = database.getCollection("Pendiente");

            Document registro = new Document("_id", new ObjectId());
            registro.append("Programa academico", nombre)
                    .append("Semestre: ", semestre)
                    .append("Facultad: ", facultad)
                    .append("Salones pedidos", Salones)
                    .append("Laboratorios pedidos", Laboratorios)
                    .append("Fecha", fecha);

            transactions.insertOne(registro);
            System.out.println("\nBase de datos actualizada.\n");
        }
    }
}
