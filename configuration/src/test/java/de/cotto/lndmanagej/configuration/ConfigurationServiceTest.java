package de.cotto.lndmanagej.configuration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Set;

import static de.cotto.lndmanagej.configuration.WarningsConfigurationSettings.ONLINE_CHANGES_THRESHOLD;
import static de.cotto.lndmanagej.model.ChannelIdFixtures.CHANNEL_ID;
import static de.cotto.lndmanagej.model.PubkeyFixtures.PUBKEY;
import static de.cotto.lndmanagej.model.ResolutionFixtures.ANCHOR_CLAIMED;
import static de.cotto.lndmanagej.model.ResolutionFixtures.COMMIT_CLAIMED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfigurationServiceTest {
    private static final String RESOLUTIONS_SECTION = "resolutions";
    private static final String ALIASES_SECTION = "aliases";
    private static final String WARNINGS_SECTION = "warnings";
    private static final String COMMIT_CLAIMED_STRING
            = "COMMIT:CLAIMED:abc222abc000abc000abc000abc000abc000abc000abc000abc000abc000abc0";
    private static final String ANCHOR_CLAIMED_STRING
            = "ANCHOR:CLAIMED:abc222abc000abc000abc000abc000abc000abc000abc000abc000abc000abc0";

    @InjectMocks
    private ConfigurationService configurationService;

    @Mock
    private IniFileReader iniFileReader;

    @Test
    void getHardcodedResolutions_empty() {
        when(iniFileReader.getValues(RESOLUTIONS_SECTION)).thenReturn(Map.of());
        assertThat(configurationService.getHardcodedResolutions(CHANNEL_ID)).isEmpty();
    }

    @Test
    void getHardcodedResolutions_one_resolution_short_channel_id() {
        when(iniFileReader.getValues(RESOLUTIONS_SECTION))
                .thenReturn(Map.of(String.valueOf(CHANNEL_ID.getShortChannelId()), Set.of(COMMIT_CLAIMED_STRING)));
        assertThat(configurationService.getHardcodedResolutions(CHANNEL_ID)).containsExactly(COMMIT_CLAIMED);
    }

    @Test
    void getHardcodedResolutions_one_resolution_compact_form() {
        when(iniFileReader.getValues(RESOLUTIONS_SECTION))
                .thenReturn(Map.of(CHANNEL_ID.getCompactForm(), Set.of(COMMIT_CLAIMED_STRING)));
        assertThat(configurationService.getHardcodedResolutions(CHANNEL_ID)).containsExactly(COMMIT_CLAIMED);
    }

    @Test
    void getHardcodedResolutions_one_resolution_compact_form_lnd() {
        when(iniFileReader.getValues(RESOLUTIONS_SECTION))
                .thenReturn(Map.of(CHANNEL_ID.getCompactFormLnd(), Set.of(COMMIT_CLAIMED_STRING)));
        assertThat(configurationService.getHardcodedResolutions(CHANNEL_ID)).containsExactly(COMMIT_CLAIMED);
    }

    @Test
    void getHardcodedResolutions_two_resolutions() {
        when(iniFileReader.getValues(RESOLUTIONS_SECTION)).thenReturn(Map.of(
                CHANNEL_ID.getCompactFormLnd(),
                Set.of(COMMIT_CLAIMED_STRING, ANCHOR_CLAIMED_STRING)
        ));
        assertThat(configurationService.getHardcodedResolutions(CHANNEL_ID))
                .containsExactlyInAnyOrder(COMMIT_CLAIMED, ANCHOR_CLAIMED);
    }

    @Test
    void getHardcodedResolutions_bogus_string() {
        when(iniFileReader.getValues(RESOLUTIONS_SECTION)).thenReturn(Map.of(
                CHANNEL_ID.getCompactFormLnd(),
                Set.of("hello", "hello:peter", "a:b:c")
        ));
        assertThat(configurationService.getHardcodedResolutions(CHANNEL_ID)).isEmpty();
    }

    @Test
    void getHardcodedAlias_not_known() {
        assertThat(configurationService.getHardcodedAlias(PUBKEY)).isEmpty();
    }

    @Test
    void getHardcodedAlias() {
        String expected = "configured alias";
        when(iniFileReader.getValues(ALIASES_SECTION)).thenReturn(Map.of(PUBKEY.toString(), Set.of(expected)));
        assertThat(configurationService.getHardcodedAlias(PUBKEY)).contains(expected);
    }

    @Test
    void getHardcodedAlias_two_in_config() {
        String first = "a";
        String second = "b";
        when(iniFileReader.getValues(ALIASES_SECTION)).thenReturn(Map.of(PUBKEY.toString(), Set.of(first, second)));
        String actual = configurationService.getHardcodedAlias(PUBKEY).orElseThrow();
        assertThat(Set.of(first, second).contains(actual)).isTrue();
    }

    @Test
    void getOnlineChangesThreshold_defaults_to_empty() {
        assertThat(configurationService.getIntegerValue(ONLINE_CHANGES_THRESHOLD)).isEmpty();
    }

    @Test
    void getIntegerValue() {
        int expectedValue = 42;
        WarningsConfigurationSettings setting = ONLINE_CHANGES_THRESHOLD;
        String name = setting.getName();
        when(iniFileReader.getValues(WARNINGS_SECTION)).thenReturn(Map.of(name, Set.of(String.valueOf(expectedValue))));
        assertThat(configurationService.getIntegerValue(setting)).contains(expectedValue);
    }

    @Test
    void getIntegerValue_not_integer() {
        WarningsConfigurationSettings setting = ONLINE_CHANGES_THRESHOLD;
        String name = setting.getName();
        when(iniFileReader.getValues(WARNINGS_SECTION)).thenReturn(Map.of(name, Set.of("x")));
        assertThat(configurationService.getIntegerValue(setting)).isEmpty();
    }
}
