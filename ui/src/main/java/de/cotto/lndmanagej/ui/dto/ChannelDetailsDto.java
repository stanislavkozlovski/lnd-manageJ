package de.cotto.lndmanagej.ui.dto;

import de.cotto.lndmanagej.controller.dto.ChannelStatusDto;
import de.cotto.lndmanagej.controller.dto.FeeReportDto;
import de.cotto.lndmanagej.controller.dto.FlowReportDto;
import de.cotto.lndmanagej.controller.dto.OnChainCostsDto;
import de.cotto.lndmanagej.controller.dto.PoliciesDto;
import de.cotto.lndmanagej.controller.dto.RatingDto;
import de.cotto.lndmanagej.controller.dto.RebalanceReportDto;
import de.cotto.lndmanagej.model.ChannelId;
import de.cotto.lndmanagej.model.OpenInitiator;
import de.cotto.lndmanagej.model.Pubkey;

import java.util.Set;

public record ChannelDetailsDto(
        ChannelId channelId,
        Pubkey remotePubkey,
        String remoteAlias,
        int channelAge,
        ChannelStatusDto channelStatus,
        OpenInitiator openInitiator,
        BalanceInformationModel balanceInformation,
        long capacitySat,
        OnChainCostsDto onChainCosts,
        PoliciesDto policies,
        FeeReportDto feeReport,
        FlowReportDto flowReport,
        RebalanceReportDto rebalanceReport,
        Set<String> warnings,
        RatingDto rating
) {

}
