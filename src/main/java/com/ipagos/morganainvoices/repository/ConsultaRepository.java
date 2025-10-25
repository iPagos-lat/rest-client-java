package com.ipagos.morganainvoices.repository;

import com.ipagos.morganainvoices.model.Consulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.math.BigDecimal; 

@Repository
public interface ConsultaRepository extends JpaRepository<Consulta, Long> {
    
    // ❌ ANTES: findByInvoiceIdIsNullAndEmailIsNotNullAndMontoGreaterThan
    // ✅ AHORA: Usar 'MontoTotal'
    List<Consulta> findByInvoiceIdIsNullAndEmailIsNotNullAndMontoTotalGreaterThan(BigDecimal monto); 
}