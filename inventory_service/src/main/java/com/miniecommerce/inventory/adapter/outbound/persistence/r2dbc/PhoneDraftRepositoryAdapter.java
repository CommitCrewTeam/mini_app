package com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc;

import com.miniecommerce.inventory.adapter.outbound.persistence.r2dbc.mapper.PhoneImportPersistenceMapper;
import com.miniecommerce.inventory.app.port.outbound.phone.PhoneDraftRepositoryPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.reactive.TransactionalOperator;

@Repository
public class PhoneDraftRepositoryAdapter implements PhoneDraftRepositoryPort {

    private final PhoneDraftReactiveRepository repository;
    private final PhoneImportPersistenceMapper mapper;
    private final TransactionalOperator transactions;

    public PhoneDraftRepositoryAdapter(
            PhoneDraftReactiveRepository repository,
            PhoneImportPersistenceMapper mapper,
            @Qualifier("inventoryTransactions") TransactionalOperator transactions
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.transactions = transactions;
    }


}
