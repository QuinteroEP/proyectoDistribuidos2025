package com.puj.cliente;

import java.util.Scanner;

public class facultad {
    public static void main(String[] args) {
        if(args.length != 4) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <nombre del programa> <semestre> <numero de salones requeridos> <numero de laboratorios requeridos>\n");
            System.exit(1);
        }
        final String nombre = args[0];
        final String semestre = args[1];
        final String numeroSalones = args[2];
        final String numeroLaboratorios = args[3];

        //enviar datos de peticion
        System.out.println("\nNueva peticion para el programa de " + nombre +" (semestre " + semestre + ")");
        System.out.println("Salones requeridos: " + numeroSalones);
        System.out.println("Laboratorios requeridos: " + numeroLaboratorios);
        System.out.println("\n");

        String server = "192.168.10.6"; //Cambiar segun la IP del servidor
        int port = 1080;
        @SuppressWarnings("resource")
        Scanner input = new Scanner(System.in);
    
        System.out.println("Conectando con el servidor");

        System.out.println("Enviando peticion al servidor y esperado respuesta... ");
    }
}