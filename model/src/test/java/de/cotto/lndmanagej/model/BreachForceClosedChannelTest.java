package de.cotto.lndmanagej.model;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import static de.cotto.lndmanagej.model.ChannelFixtures.CAPACITY;
import static de.cotto.lndmanagej.model.ChannelIdFixtures.CHANNEL_ID;
import static de.cotto.lndmanagej.model.ChannelPointFixtures.CHANNEL_POINT;
import static de.cotto.lndmanagej.model.ChannelPointFixtures.TRANSACTION_HASH_2;
import static de.cotto.lndmanagej.model.ForceClosedChannelFixtures.FORCE_CLOSED_CHANNEL_BREACH;
import static de.cotto.lndmanagej.model.PubkeyFixtures.PUBKEY;
import static de.cotto.lndmanagej.model.PubkeyFixtures.PUBKEY_2;
import static org.assertj.core.api.Assertions.assertThat;

class BreachForceClosedChannelTest {
    @Test
    void create() {
        assertThat(new BreachForceClosedChannelBuilder()
                .withChannelId(CHANNEL_ID)
                .withChannelPoint(CHANNEL_POINT)
                .withCapacity(CAPACITY)
                .withOwnPubkey(PUBKEY)
                .withRemotePubkey(PUBKEY_2)
                .withCloseTransactionHash(TRANSACTION_HASH_2)
                .withOpenInitiator(OpenInitiator.LOCAL)
                .build()
        ).isEqualTo(FORCE_CLOSED_CHANNEL_BREACH);
    }

    @Test
    void getId() {
        assertThat(FORCE_CLOSED_CHANNEL_BREACH.getId()).isEqualTo(CHANNEL_ID);
    }

    @Test
    void getRemotePubkey() {
        assertThat(FORCE_CLOSED_CHANNEL_BREACH.getRemotePubkey()).isEqualTo(PUBKEY_2);
    }

    @Test
    void getCapacity() {
        assertThat(FORCE_CLOSED_CHANNEL_BREACH.getCapacity()).isEqualTo(CAPACITY);
    }

    @Test
    void getChannelPoint() {
        assertThat(FORCE_CLOSED_CHANNEL_BREACH.getChannelPoint()).isEqualTo(CHANNEL_POINT);
    }

    @Test
    void getPubkeys() {
        assertThat(FORCE_CLOSED_CHANNEL_BREACH.getPubkeys()).containsExactlyInAnyOrder(PUBKEY, PUBKEY_2);
    }

    @Test
    void getCloseTransactionHash() {
        assertThat(FORCE_CLOSED_CHANNEL_BREACH.getCloseTransactionHash()).isEqualTo(TRANSACTION_HASH_2);
    }

    @Test
    void getOpenInitiator() {
        assertThat(FORCE_CLOSED_CHANNEL_BREACH.getOpenInitiator()).isEqualTo(OpenInitiator.LOCAL);
    }

    @Test
    void getCloseInitiator() {
        assertThat(FORCE_CLOSED_CHANNEL_BREACH.getCloseInitiator()).isEqualTo(CloseInitiator.REMOTE);
    }

    @Test
    void testEquals() {
        EqualsVerifier.forClass(BreachForceClosedChannel.class).usingGetClass().verify();
    }
}