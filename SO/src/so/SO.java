/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package so;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

class Proceso {
    int pid;
    int tiempoEjecucion;
    int tamañoMemoria;
    int prioridad;
    int tiempoRestante;
    int tipoInstruccion;
    int tiempoBloqueo;

    public Proceso(int pid, int tiempoEjecucion, int tamañoMemoria, int prioridad) {
        this.pid = pid;
        this.tiempoEjecucion = tiempoEjecucion;
        this.tamañoMemoria = tamañoMemoria;
        this.prioridad = prioridad;
        this.tiempoRestante = tiempoEjecucion;
    }
}

class PlanificadorProcesos {
    private ArrayList<Proceso> colaListos = new ArrayList<>();
    private ArrayList<Proceso> colaTerminados = new ArrayList<>();
    private final int TAMAÑO_MEMORIA = 16 * 1024;  // 16 Mb
    private int usoMemoriaActual = 0;
    private int contadorProcesos = 0;
    private Proceso procesoEjecutando = null;
    private final int QUANTUM = 5;
    private boolean enEjecucion = false;
    private Random aleatorio = new Random();
    private ExecutorService servicioEjecucion = Executors.newSingleThreadExecutor();

    public Proceso generarProceso() {
        if (usoMemoriaActual + 500 <= TAMAÑO_MEMORIA) {
            contadorProcesos++;
            int tiempoEjecucion = aleatorio.nextInt(21) + 10;  // 10-30
            int tamañoMemoria = aleatorio.nextInt(301) + 200;  // 200-500
            int prioridad = aleatorio.nextInt(4) + 1;  // 1-4

            Proceso nuevoProceso = new Proceso(contadorProcesos, tiempoEjecucion, tamañoMemoria, prioridad);

            if (prioridad == 1) {
                colaListos.add(0, nuevoProceso);
            } else {
                colaListos.add(nuevoProceso);
            }

            usoMemoriaActual += tamañoMemoria;
            return nuevoProceso;
        }
        return null;
    }

    public void generarInstruccion(Proceso proceso) {
        int tipoInstruccion = aleatorio.nextInt(3) + 1;
        
        switch (tipoInstruccion) {
            case 1:  // Ejecución normal
                proceso.tiempoBloqueo = QUANTUM;
                break;
            case 2:  // Operación de entrada-salida
                proceso.tiempoBloqueo = aleatorio.nextInt(11) + 5;  // 5-15
                break;
            case 3:  // Acceso a memoria
                proceso.tiempoBloqueo = aleatorio.nextInt(9) + 4;  // 4-12
                break;
        }

        proceso.tiempoBloqueo = Math.min(proceso.tiempoBloqueo, proceso.tiempoRestante);
        proceso.tipoInstruccion = tipoInstruccion;
    }

    public void planificarProceso() {
        if (!enEjecucion) return;

        // Planificación por prioridad para procesos de prioridad 1
        Proceso procesoPrioridad = colaListos.stream()
            .filter(p -> p.prioridad == 1)
            .findFirst()
            .orElse(null);

        if (procesoPrioridad != null) {
            procesoEjecutando = procesoPrioridad;
            colaListos.remove(procesoPrioridad);
        } else if (!colaListos.isEmpty()) {
            procesoEjecutando = colaListos.remove(0);
        } else {
            return;
        }

        generarInstruccion(procesoEjecutando);

        servicioEjecucion.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(procesoEjecutando.tiempoBloqueo);
                
                procesoEjecutando.tiempoRestante -= procesoEjecutando.tiempoBloqueo;
                
                if (procesoEjecutando.tiempoRestante <= 0) {
                    usoMemoriaActual -= procesoEjecutando.tamañoMemoria;
                    colaTerminados.add(procesoEjecutando);
                } else {
                    if (procesoEjecutando.prioridad == 1) {
                        colaListos.add(0, procesoEjecutando);
                    } else {
                        colaListos.add(procesoEjecutando);
                    }
                }
                
                planificarProceso();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    // Métodos para la interfaz gráfica
    public ArrayList<Proceso> getColaListos() { return colaListos; }
    public ArrayList<Proceso> getColaTerminados() { return colaTerminados; }
    public void setEjecucion(boolean ejecucion) { enEjecucion = ejecucion; }
    public boolean getEjecucion() { return enEjecucion; }
    public int getContadorProcesos() { return contadorProcesos; }
    public int getUsoMemoriaActual() { return usoMemoriaActual; }
}

public class SO extends JFrame {
    private PlanificadorProcesos planificador = new PlanificadorProcesos();
    private JTextArea areaListaProcesos;
    private Thread hiloGeneracion;

    public SO() {
        setTitle("Simulador de Gestión de Procesos y Memoria");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        JButton botonIniciar = new JButton("Iniciar Simulación");
        botonIniciar.addActionListener(e -> iniciarSimulacion());
        add(botonIniciar);

        JButton botonProcesoManual = new JButton("Crear Proceso Manual");
        botonProcesoManual.addActionListener(e -> crearProcesoManual());
        add(botonProcesoManual);

        JButton botonTerminar = new JButton("Terminar Simulación");
        botonTerminar.addActionListener(e -> terminarSimulacion());
        add(botonTerminar);

        areaListaProcesos = new JTextArea(10, 50);
        areaListaProcesos.setEditable(false);
        add(new JScrollPane(areaListaProcesos));
    }

    private void iniciarSimulacion() {
        planificador.setEjecucion(true);
        
        hiloGeneracion = new Thread(() -> {
            while (planificador.getEjecucion()) {
                if (Math.random() < 0.5) {
                    planificador.generarProceso();
                    actualizarListaProcesos();
                }
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        hiloGeneracion.start();

        planificador.planificarProceso();
    }

    private void crearProcesoManual() {
        if (!planificador.getEjecucion()) {
            JOptionPane.showMessageDialog(this, "Inicie la simulación primero");
            return;
        }

        try {
            int tiempoEjecucion = Integer.parseInt(JOptionPane.showInputDialog("Tiempo de Ejecución (10-30):"));
            int tamañoMemoria = Integer.parseInt(JOptionPane.showInputDialog("Tamaño de Memoria (200-500):"));
            int prioridad = Integer.parseInt(JOptionPane.showInputDialog("Prioridad (1-4):"));

            if (tiempoEjecucion >= 10 && tiempoEjecucion <= 30 &&
                tamañoMemoria >= 200 && tamañoMemoria <= 500 &&
                prioridad >= 1 && prioridad <= 4) {

                Proceso nuevoProceso = new Proceso(
                    planificador.getContadorProcesos() + 1, 
                    tiempoEjecucion, 
                    tamañoMemoria, 
                    prioridad
                );

                if (prioridad == 1) {
                    planificador.getColaListos().add(0, nuevoProceso);
                } else {
                    planificador.getColaListos().add(nuevoProceso);
                }

                actualizarListaProcesos();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Entrada inválida");
        }
    }

    private void actualizarListaProcesos() {
        SwingUtilities.invokeLater(() -> {
            areaListaProcesos.setText("Cola de Listos:\n");
            for (Proceso p : planificador.getColaListos()) {
                areaListaProcesos.append(String.format(
                    "PID: %d, Tiempo: %d, Memoria: %d, Prioridad: %d\n", 
                    p.pid, p.tiempoRestante, p.tamañoMemoria, p.prioridad
                ));
            }
            
            areaListaProcesos.append("\nCola de Terminados:\n");
            for (Proceso p : planificador.getColaTerminados()) {
                areaListaProcesos.append(String.format(
                    "PID: %d, Tiempo: %d, Memoria: %d, Prioridad: %d\n", 
                    p.pid, p.tiempoEjecucion, p.tamañoMemoria, p.prioridad
                ));
            }
        });
    }

    private void terminarSimulacion() {
        planificador.setEjecucion(false);
        JOptionPane.showMessageDialog(this, 
            String.format(
                "Procesos Totales: %d\n" +
                "Procesos Terminados: %d\n" +
                "Uso de Memoria: %d Kb", 
                planificador.getContadorProcesos(),
                planificador.getColaTerminados().size(),
                planificador.getUsoMemoriaActual()
            )
        );
        System.exit(0);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new SO().setVisible(true);
        });
    }
}
