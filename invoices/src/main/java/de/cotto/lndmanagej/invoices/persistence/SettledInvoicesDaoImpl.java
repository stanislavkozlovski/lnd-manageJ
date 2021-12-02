package de.cotto.lndmanagej.invoices.persistence;

import de.cotto.lndmanagej.invoices.SettledInvoicesDao;
import de.cotto.lndmanagej.model.SettledInvoice;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
@Transactional
public class SettledInvoicesDaoImpl implements SettledInvoicesDao {
    private final SettledInvoicesRepository repository;

    public SettledInvoicesDaoImpl(SettledInvoicesRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(Collection<SettledInvoice> settledInvoices) {
        List<SettledInvoiceJpaDto> converted = settledInvoices.stream()
                .map(SettledInvoiceJpaDto::createFromInvoice)
                .collect(toList());
        repository.saveAll(converted);
    }

    @Override
    public long getOffset() {
        return repository.getMaxAddIndexWithoutGaps();
    }
}
