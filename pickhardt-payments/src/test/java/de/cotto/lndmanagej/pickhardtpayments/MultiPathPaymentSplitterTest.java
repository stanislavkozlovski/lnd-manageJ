package de.cotto.lndmanagej.pickhardtpayments;

import de.cotto.lndmanagej.grpc.GrpcGetInfo;
import de.cotto.lndmanagej.model.Coins;
import de.cotto.lndmanagej.pickhardtpayments.model.Edge;
import de.cotto.lndmanagej.pickhardtpayments.model.EdgeWithLiquidityInformation;
import de.cotto.lndmanagej.pickhardtpayments.model.Flow;
import de.cotto.lndmanagej.pickhardtpayments.model.Flows;
import de.cotto.lndmanagej.pickhardtpayments.model.MultiPathPayment;
import de.cotto.lndmanagej.pickhardtpayments.model.Route;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static de.cotto.lndmanagej.model.ChannelFixtures.CAPACITY;
import static de.cotto.lndmanagej.model.ChannelFixtures.CAPACITY_2;
import static de.cotto.lndmanagej.model.ChannelIdFixtures.CHANNEL_ID;
import static de.cotto.lndmanagej.model.ChannelIdFixtures.CHANNEL_ID_2;
import static de.cotto.lndmanagej.model.PolicyFixtures.POLICY_1;
import static de.cotto.lndmanagej.model.PubkeyFixtures.PUBKEY;
import static de.cotto.lndmanagej.model.PubkeyFixtures.PUBKEY_2;
import static de.cotto.lndmanagej.model.PubkeyFixtures.PUBKEY_4;
import static de.cotto.lndmanagej.pickhardtpayments.model.EdgeFixtures.EDGE;
import static de.cotto.lndmanagej.pickhardtpayments.model.FlowFixtures.FLOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MultiPathPaymentSplitterTest {
    private static final Coins AMOUNT = Coins.ofSatoshis(1_234);
    @InjectMocks
    private MultiPathPaymentSplitter multiPathPaymentSplitter;

    @Mock
    private FlowComputation flowComputation;

    @Mock
    private GrpcGetInfo grpcGetInfo;

    @Mock
    private EdgeComputation edgeComputation;

    @BeforeEach
    void setUp() {
        when(flowComputation.getOptimalFlows(any(), any(), any())).thenReturn(new Flows());
        lenient().when(edgeComputation.getEdgeWithLiquidityInformation(EDGE)).thenReturn(noInformationFor(EDGE));
    }

    @Test
    void getMultiPathPaymentTo_uses_own_pubkey_as_source() {
        when(grpcGetInfo.getPubkey()).thenReturn(PUBKEY_4);
        multiPathPaymentSplitter.getMultiPathPaymentTo(PUBKEY_2, AMOUNT);
        verify(flowComputation).getOptimalFlows(PUBKEY_4, PUBKEY_2, AMOUNT);
    }

    @Test
    void getMultiPathPayment_failure() {
        MultiPathPayment multiPathPayment = multiPathPaymentSplitter.getMultiPathPayment(PUBKEY, PUBKEY_2, AMOUNT);
        assertThat(multiPathPayment.probability()).isZero();
    }

    @Test
    void getMultiPathPaymentTo() {
        when(flowComputation.getOptimalFlows(PUBKEY, PUBKEY_2, AMOUNT)).thenReturn(new Flows(FLOW));
        when(grpcGetInfo.getPubkey()).thenReturn(PUBKEY);

        MultiPathPayment multiPathPayment = multiPathPaymentSplitter.getMultiPathPaymentTo(PUBKEY_2, AMOUNT);

        Route expectedRoute = new Route(List.of(EDGE), AMOUNT).withLiquidityInformation(Set.of(noInformationFor(EDGE)));
        MultiPathPayment expected = new MultiPathPayment(Set.of(expectedRoute));
        assertThat(multiPathPayment).isEqualTo(expected);
    }

    @Test
    void getMultiPathPayment_one_flow_probability() {
        long capacitySat = EDGE.capacity().satoshis();
        Coins halfOfCapacity = Coins.ofSatoshis(capacitySat / 2);
        Flow flow = new Flow(EDGE, halfOfCapacity);
        when(flowComputation.getOptimalFlows(PUBKEY, PUBKEY_2, halfOfCapacity)).thenReturn(new Flows(flow));

        MultiPathPayment multiPathPayment =
                multiPathPaymentSplitter.getMultiPathPayment(PUBKEY, PUBKEY_2, halfOfCapacity);

        assertThat(multiPathPayment.probability())
                .isEqualTo((1.0 * halfOfCapacity.satoshis() + 1) / (capacitySat + 1));
    }

    @Test
    void getMultiPathPayment_two_flows_probability() {
        long capacitySat = CAPACITY.satoshis();
        Coins halfOfCapacity = Coins.ofSatoshis(capacitySat / 2);
        Edge edge1 = new Edge(CHANNEL_ID, PUBKEY, PUBKEY_2, CAPACITY, POLICY_1);
        Edge edge2 = new Edge(CHANNEL_ID_2, PUBKEY, PUBKEY_2, CAPACITY_2, POLICY_1);
        Flow flow1 = new Flow(edge1, halfOfCapacity);
        Flow flow2 = new Flow(edge2, CAPACITY_2);
        when(edgeComputation.getEdgeWithLiquidityInformation(edge1)).thenReturn(noInformationFor(edge1));
        when(edgeComputation.getEdgeWithLiquidityInformation(edge2)).thenReturn(noInformationFor(edge2));
        Coins totalAmount = halfOfCapacity.add(CAPACITY_2);
        when(flowComputation.getOptimalFlows(PUBKEY, PUBKEY_2, totalAmount)).thenReturn(new Flows(flow1, flow2));

        MultiPathPayment multiPathPayment =
                multiPathPaymentSplitter.getMultiPathPayment(PUBKEY, PUBKEY_2, totalAmount);

        double probabilityFlow1 = (1.0 * halfOfCapacity.satoshis() + 1) / (capacitySat + 1);
        double probabilityFlow2 = 1.0 / (CAPACITY_2.satoshis() + 1);
        assertThat(multiPathPayment.probability()).isEqualTo(probabilityFlow1 * probabilityFlow2);
    }

    @Test
    void getMultiPathPayment_two_flows_through_same_channel_probability() {
        long capacitySat = EDGE.capacity().satoshis();
        Coins halfOfCapacity = Coins.ofSatoshis(capacitySat / 2);
        Flow flow1 = new Flow(EDGE, halfOfCapacity);
        Flow flow2 = new Flow(EDGE, halfOfCapacity);
        when(flowComputation.getOptimalFlows(PUBKEY, PUBKEY_2, EDGE.capacity())).thenReturn(new Flows(flow1, flow2));

        MultiPathPayment multiPathPayment =
                multiPathPaymentSplitter.getMultiPathPayment(PUBKEY, PUBKEY_2, EDGE.capacity());

        assertThat(multiPathPayment.probability()).isEqualTo(1.0 / (capacitySat + 1));
    }

    @Test
    void getMultiPathPayment_adds_remainder_to_most_probable_route() {
        when(flowComputation.getOptimalFlows(PUBKEY, PUBKEY_2, AMOUNT)).thenReturn(new Flows(FLOW));
        assumeThat(FLOW.amount()).isLessThan(AMOUNT);
        MultiPathPayment multiPathPayment = multiPathPaymentSplitter.getMultiPathPayment(PUBKEY, PUBKEY_2, AMOUNT);
        assertThat(multiPathPayment.routes().iterator().next().amount()).isEqualTo(AMOUNT);
    }

    @Test
    void getMultiPathPayment_adds_liquidity_information_to_route() {
        Coins amount = FLOW.amount();
        Edge edge = FLOW.edge();
        EdgeWithLiquidityInformation withLiquidityInformation =
                EdgeWithLiquidityInformation.forKnownLiquidity(edge, Coins.ofSatoshis(1));
        when(edgeComputation.getEdgeWithLiquidityInformation(edge)).thenReturn(withLiquidityInformation);
        when(flowComputation.getOptimalFlows(PUBKEY, PUBKEY_2, amount)).thenReturn(new Flows(FLOW));
        Route expected = new Route(List.of(edge), amount).withLiquidityInformation(Set.of(withLiquidityInformation));

        MultiPathPayment multiPathPayment = multiPathPaymentSplitter.getMultiPathPayment(PUBKEY, PUBKEY_2, amount);
        assertThat(multiPathPayment.routes()).containsExactly(expected);
    }

    @Test
    void getMultiPathPayment_adds_remainder_to_most_probable_route_due_to_liquidity_information() {
        Coins oneSat = Coins.ofSatoshis(1);
        Coins largeCapacity = Coins.ofSatoshis(10);
        Coins smallCapacity = Coins.ofSatoshis(5);

        Edge edgeLargeCapacity = new Edge(CHANNEL_ID, PUBKEY, PUBKEY_2, largeCapacity, POLICY_1);
        Edge edgeSmallCapacity = new Edge(CHANNEL_ID_2, PUBKEY, PUBKEY_2, smallCapacity, POLICY_1);

        EdgeWithLiquidityInformation liquidityInformationLarge =
                EdgeWithLiquidityInformation.forUpperBound(edgeLargeCapacity, Coins.ofSatoshis(2));
        EdgeWithLiquidityInformation liquidityInformationSmall = noInformationFor(edgeSmallCapacity);
        when(edgeComputation.getEdgeWithLiquidityInformation(edgeLargeCapacity)).thenReturn(liquidityInformationLarge);
        when(edgeComputation.getEdgeWithLiquidityInformation(edgeSmallCapacity)).thenReturn(liquidityInformationSmall);

        when(flowComputation.getOptimalFlows(PUBKEY, PUBKEY_2, AMOUNT)).thenReturn(new Flows(
                new Flow(edgeLargeCapacity, oneSat),
                new Flow(edgeSmallCapacity, oneSat)
        ));
        assumeThat(FLOW.amount()).isLessThan(AMOUNT);
        MultiPathPayment multiPathPayment = multiPathPaymentSplitter.getMultiPathPayment(PUBKEY, PUBKEY_2, AMOUNT);
        Route route1 = new Route(List.of(edgeLargeCapacity), oneSat)
                .withLiquidityInformation(Set.of(liquidityInformationLarge));
        Route route2 = new Route(List.of(edgeSmallCapacity), AMOUNT.subtract(oneSat))
                .withLiquidityInformation(Set.of(liquidityInformationSmall));
        assertThat(multiPathPayment.routes()).containsExactlyInAnyOrder(route1, route2);
    }

    private EdgeWithLiquidityInformation noInformationFor(Edge edgeSmallCapacity) {
        return EdgeWithLiquidityInformation.forUpperBound(edgeSmallCapacity, edgeSmallCapacity.capacity());
    }
}
