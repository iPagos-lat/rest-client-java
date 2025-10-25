package com.ipagos.morganainvoices.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultas")
@Data
@NoArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ✅ Campos de la cita
    @Column(nullable = true) 
    private String nombreMedico;
    
    @Column(nullable = true)
    private String nombrePaciente;
    
    @Column(nullable = true)
    private String email;
    
    @Column(name = "telefono_paciente", nullable = true) 
    private String telefonoPaciente;
    
    @Column(name = "fecha_consulta", nullable = true)
    private LocalDate fechaConsulta;
    
    @Column(name = "hora_consulta", nullable = true)
    private LocalTime horaConsulta;

    // ✅ NUEVO: MONTO TOTAL y MONEDA (El monto anterior es reemplazado)
    @Column(name = "monto_total", nullable = false) // Monto es obligatorio, usamos NOT NULL en JPA
    private BigDecimal montoTotal;
    
    @Column(name = "moneda", length = 3, nullable = false) // Usaremos 'moneda' con longitud 3 (MXN)
    private String moneda;
    
    // ... (El resto de los campos existentes) ...
    
    private boolean aceptaTerminos;

    @Column(name = "fecha_registro")
    private LocalDateTime fechaRegistro = LocalDateTime.now();

    private String invoiceId;
    private String paymentLink;
    private String status;
}