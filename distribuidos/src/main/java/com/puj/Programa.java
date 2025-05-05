package com.puj;

public class Programa {
    public static void main(String[] args) {
        if(args.length != 5) {
            System.out.println("\nError: uso incorrecto. Se requieren los parametros <nombre del programa> <semestre> <facultad del programa> <numero de salones requeridos> <numero de laboratorios requeridos>\n");
            System.exit(1);
        }

        final String nombre = args[0];
        final String semestre = args[1];
        final int facultad = Integer.parseInt(args[2]);
        final int numeroSalones = Integer.parseInt(args[3]);
        final int numeroLaboratorios = Integer.parseInt(args[4]);
    }
    
}
