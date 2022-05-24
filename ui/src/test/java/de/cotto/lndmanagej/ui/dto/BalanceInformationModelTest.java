package de.cotto.lndmanagej.ui.dto;

import org.junit.jupiter.api.Test;

import static de.cotto.lndmanagej.ui.dto.BalanceInformationModelFixture.BALANCE_INFORMATION_MODEL;
import static org.assertj.core.api.Assertions.assertThat;

class BalanceInformationModelTest {

    @Test
    void routableCapacity() {
        assertThat(BALANCE_INFORMATION_MODEL.getRoutableCapacity()).isEqualTo(1123L);
    }

    @Test
    void inboundPercentage() {
        assertThat(BALANCE_INFORMATION_MODEL.getInboundPercentage()).isEqualTo(10.952_804_986_642_917);
    }

    @Test
    void outboundPercentage() {
        assertThat(BALANCE_INFORMATION_MODEL.getOutboundPercentage()).isEqualTo(100 - 10.952_804_986_642_917);
    }
}