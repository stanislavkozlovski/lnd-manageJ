package de.cotto.lndmanagej.pickhardtpayments;

import com.google.ortools.Loader;
import com.google.ortools.graph.MinCostFlow;
import de.cotto.lndmanagej.model.Coins;
import de.cotto.lndmanagej.model.Pubkey;
import de.cotto.lndmanagej.pickhardtpayments.model.Edge;
import de.cotto.lndmanagej.pickhardtpayments.model.EdgeWithLiquidityInformation;
import de.cotto.lndmanagej.pickhardtpayments.model.IntegerMapping;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static de.cotto.lndmanagej.pickhardtpayments.model.EdgeFixtures.EDGE;
import static de.cotto.lndmanagej.pickhardtpayments.model.EdgeFixtures.EDGE_1_3;
import static de.cotto.lndmanagej.pickhardtpayments.model.EdgeFixtures.EDGE_2_3;
import static de.cotto.lndmanagej.pickhardtpayments.model.EdgeFixtures.EDGE_3_4;
import static org.assertj.core.api.Assertions.assertThat;

class ArcInitializerTest {
    private static final int QUANTIZATION = 1;
    private static final int PIECEWISE_LINEAR_APPROXIMATIONS = 1;
    private static final int FEE_RATE_FACTOR = 0;

    static {
        Loader.loadNativeLibraries();
    }

    private final MinCostFlow minCostFlow = new MinCostFlow();
    private final IntegerMapping<Pubkey> integerMapping = new IntegerMapping<>();
    private final Map<Integer, Edge> edgeMapping = new LinkedHashMap<>();
    private final ArcInitializer arcInitializer = new ArcInitializer(
            minCostFlow,
            integerMapping,
            edgeMapping,
            QUANTIZATION,
            PIECEWISE_LINEAR_APPROXIMATIONS,
            FEE_RATE_FACTOR
    );

    @Test
    void no_edge() {
        arcInitializer.addArcs(Set.of());
        assertThat(minCostFlow.getNumArcs()).isZero();
    }

    @Test
    void ignores_edge_with_zero_capacity() {
        arcInitializer.addArcs(Set.of(edge(EDGE, Coins.NONE)));
        assertThat(minCostFlow.getNumArcs()).isZero();
    }

    @Test
    void edge_with_capacity_equal_to_quantization_amount() {
        int quantization = 10_000;
        ArcInitializer arcInitializer = new ArcInitializer(
                minCostFlow,
                integerMapping,
                edgeMapping,
                quantization,
                PIECEWISE_LINEAR_APPROXIMATIONS,
                FEE_RATE_FACTOR
        );
        EdgeWithLiquidityInformation edgeWithLiquidityInformation =
                edge(EDGE, Coins.ofSatoshis(quantization));
        arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));
        assertThat(minCostFlow.getNumArcs()).isOne();
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    void edge_with_known_liquidity_is_added_as_arc_without_cost() {
        Coins capacity = Coins.ofSatoshis(100);
        Coins knownLiquidity = Coins.ofSatoshis(25);
        EdgeWithLiquidityInformation edgeWithLiquidityInformation =
                EdgeWithLiquidityInformation.forKnownLiquidity(EDGE.withCapacity(capacity), knownLiquidity);

        arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));

        assertThat(minCostFlow.getNumArcs()).isEqualTo(1);
        assertThat(minCostFlow.getUnitCost(0)).isEqualTo(0);
        assertThat(minCostFlow.getCapacity(0)).isEqualTo(25);
    }

    @Nested
    class EdgeWithLowerAndUpperBound {
        private EdgeWithLiquidityInformation edgeWithLiquidityInformation;

        @BeforeEach
        void setUp() {
            Coins capacity = Coins.ofSatoshis(100);
            Coins knownLiquidity = Coins.ofSatoshis(25);
            edgeWithLiquidityInformation = EdgeWithLiquidityInformation.forLowerAndUpperBound(
                    EDGE.withCapacity(capacity),
                    knownLiquidity,
                    capacity
            );
        }

        @Test
        void added_as_arc_without_cost() {
            arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));
            assertThat(minCostFlow.getUnitCost(0)).isEqualTo(0);
            assertThat(minCostFlow.getCapacity(0)).isEqualTo(25);
        }

        @Test
        void adds_uncertain_liquidity_as_second_arc() {
            ArcInitializer arcInitializer = new ArcInitializer(
                    minCostFlow,
                    integerMapping,
                    edgeMapping,
                    QUANTIZATION,
                    2,
                    FEE_RATE_FACTOR
            );
            arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));
            assertThat(minCostFlow.getUnitCost(1)).isEqualTo(1);
            assertThat(minCostFlow.getCapacity(1)).isEqualTo(75);
        }

        @Test
        void splits_uncertain_liquidity_as_additional_arcs() {
            ArcInitializer arcInitializer = new ArcInitializer(
                    minCostFlow,
                    integerMapping,
                    edgeMapping,
                    QUANTIZATION,
                    5,
                    FEE_RATE_FACTOR
            );
            arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));
            assertThat(minCostFlow.getNumArcs()).isEqualTo(5);
        }

        @Test
        void known_amount_matches_quantization() {
            ArcInitializer arcInitializer = new ArcInitializer(
                    minCostFlow,
                    integerMapping,
                    edgeMapping,
                    edgeWithLiquidityInformation.availableLiquidityLowerBound().satoshis(),
                    PIECEWISE_LINEAR_APPROXIMATIONS,
                    FEE_RATE_FACTOR
            );
            arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));
            assertThat(minCostFlow.getUnitCost(0)).isEqualTo(0);
        }

        @Test
        void known_amount_rounded_due_to_quantization() {
            ArcInitializer arcInitializer = new ArcInitializer(
                    minCostFlow,
                    integerMapping,
                    edgeMapping,
                    20,
                    PIECEWISE_LINEAR_APPROXIMATIONS,
                    FEE_RATE_FACTOR
            );
            arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));
            assertThat(minCostFlow.getCapacity(0)).isEqualTo(1);
        }

        @Test
        void splits_remaining_liquidity_after_rounding_known_liquidity() {
            ArcInitializer arcInitializer = new ArcInitializer(
                    minCostFlow,
                    integerMapping,
                    edgeMapping,
                    10,
                    2,
                    FEE_RATE_FACTOR
            );
            arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));
            // one arc for the known liquidity (25 / 10 = 2), 100 / 10 - 2 = 8 remaining
            assertThat(minCostFlow.getCapacity(1)).isEqualTo(8);
        }

        @Test
        void does_not_add_arcs_without_capacity() {
            ArcInitializer arcInitializer = new ArcInitializer(
                    minCostFlow,
                    integerMapping,
                    edgeMapping,
                    20,
                    6,
                    FEE_RATE_FACTOR
            );
            arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));
            // one arc for the known liquidity (25 / 20 = 1), 100 / 20 - 1 = 4 remaining: 4 < 5, no additional arc added
            assertThat(minCostFlow.getNumArcs()).isEqualTo(1);
        }
    }

    @Test
    void adds_edge_to_edgeMapping() {
        int piecesPerChannel = 2;
        ArcInitializer arcInitializer = new ArcInitializer(
                minCostFlow,
                integerMapping,
                edgeMapping,
                QUANTIZATION,
                piecesPerChannel,
                FEE_RATE_FACTOR
        );
        arcInitializer.addArcs(List.of(
                edge(EDGE, Coins.ofSatoshis(100)),
                edge(EDGE_2_3, Coins.ofSatoshis(200))
        ));
        assertThat(edgeMapping.get(0)).isEqualTo(EDGE.withCapacity(Coins.ofSatoshis(100)));
        assertThat(edgeMapping.get(piecesPerChannel)).isEqualTo(EDGE_2_3.withCapacity(Coins.ofSatoshis(200)));
    }

    @Test
    void ignores_edge_with_capacity_smaller_than_quantization_amount() {
        int quantization = 10_000;
        ArcInitializer arcInitializer = new ArcInitializer(
                minCostFlow,
                integerMapping,
                edgeMapping,
                quantization,
                PIECEWISE_LINEAR_APPROXIMATIONS,
                FEE_RATE_FACTOR
        );
        EdgeWithLiquidityInformation edgeWithLiquidityInformation =
                edge(EDGE, Coins.ofSatoshis(quantization - 1));
        arcInitializer.addArcs(Set.of(edgeWithLiquidityInformation));
        assertThat(minCostFlow.getNumArcs()).isZero();
    }

    @Test
    void uses_quantization_for_capacity() {
        int quantization = 100;
        ArcInitializer arcInitializer = new ArcInitializer(
                minCostFlow,
                integerMapping,
                edgeMapping,
                quantization,
                PIECEWISE_LINEAR_APPROXIMATIONS,
                FEE_RATE_FACTOR
        );
        arcInitializer.addArcs(Set.of(edge(EDGE, Coins.ofSatoshis(20_123))));
        assertThat(minCostFlow.getCapacity(0)).isEqualTo(201);
    }

    @Test
    void uses_quantization_for_unit_cost() {
        int quantization = 100;
        ArcInitializer arcInitializer = new ArcInitializer(
                minCostFlow,
                integerMapping,
                edgeMapping,
                quantization,
                PIECEWISE_LINEAR_APPROXIMATIONS,
                FEE_RATE_FACTOR
        );
        arcInitializer.addArcs(List.of(
                edge(EDGE, Coins.ofSatoshis(20_123)),
                edge(EDGE_3_4, Coins.ofSatoshis(1_000_000))
        ));
        assertThat(minCostFlow.getUnitCost(0)).isEqualTo(49);
    }

    @Test
    void one_edge() {
        arcInitializer.addArcs(Set.of(edge(EDGE, Coins.ofSatoshis(1))));
        assertThat(minCostFlow.getNumArcs()).isOne();
    }

    @Test
    @SuppressWarnings("PMD.JUnitTestContainsTooManyAsserts")
    void edges_with_piecewise_linear_approximation() {
        int piecewiseLinearApproximations = 5;
        ArcInitializer arcInitializer = new ArcInitializer(
                minCostFlow,
                integerMapping,
                edgeMapping,
                QUANTIZATION,
                piecewiseLinearApproximations,
                FEE_RATE_FACTOR
        );
        arcInitializer.addArcs(List.of(
                edge(EDGE, Coins.ofSatoshis(10_000)),
                edge(EDGE_2_3, Coins.ofSatoshis(30_000))
        ));
        assertThat(minCostFlow.getNumArcs()).isEqualTo(10);
        assertThat(minCostFlow.getUnitCost(0)).isEqualTo(3);
        assertThat(minCostFlow.getCapacity(0)).isEqualTo(2_000);
        assertThat(minCostFlow.getUnitCost(1)).isEqualTo(6);
        assertThat(minCostFlow.getCapacity(1)).isEqualTo(2_000);
        assertThat(minCostFlow.getUnitCost(2)).isEqualTo(9);
        assertThat(minCostFlow.getCapacity(2)).isEqualTo(2_000);
        assertThat(minCostFlow.getUnitCost(3)).isEqualTo(12);
        assertThat(minCostFlow.getCapacity(3)).isEqualTo(2_000);
        assertThat(minCostFlow.getUnitCost(4)).isEqualTo(15);
        assertThat(minCostFlow.getCapacity(4)).isEqualTo(2_000);
    }

    @Test
    void two_edges() {
        EdgeWithLiquidityInformation edge1 = edge(EDGE, Coins.ofSatoshis(1));
        EdgeWithLiquidityInformation edge2 = edge(EDGE_1_3, Coins.ofSatoshis(2));
        arcInitializer.addArcs(Set.of(edge1, edge2));
        assertThat(minCostFlow.getNumArcs()).isEqualTo(2);
    }

    @Test
    void parallel_edges_are_not_combined() {
        EdgeWithLiquidityInformation edge = edge(EDGE, Coins.ofSatoshis(1));
        arcInitializer.addArcs(List.of(edge, edge));
        assertThat(minCostFlow.getNumArcs()).isEqualTo(2);
    }

    @Test
    void computes_unit_cost_based_on_maximum_capacity() {
        EdgeWithLiquidityInformation edge1 = edge(EDGE, Coins.ofSatoshis(3));
        EdgeWithLiquidityInformation edge2 = edge(EDGE_1_3, Coins.ofSatoshis(21));
        arcInitializer.addArcs(List.of(edge1, edge2));
        assertThat(minCostFlow.getUnitCost(0)).isEqualTo(7L);
        assertThat(minCostFlow.getUnitCost(1)).isEqualTo(1L);
    }

    @Test
    void computes_unit_cost_based_on_maximum_capacity_even_if_upper_bound_is_lower() {
        EdgeWithLiquidityInformation edge1 = edge(EDGE, Coins.ofSatoshis(3));
        EdgeWithLiquidityInformation edge2 = EdgeWithLiquidityInformation.forUpperBound(
                EDGE_1_3.withCapacity(Coins.ofSatoshis(42)),
                Coins.ofSatoshis(8)
        );
        arcInitializer.addArcs(List.of(edge1, edge2));
        assertThat(minCostFlow.getUnitCost(0)).isEqualTo(42 / 3);
        assertThat(minCostFlow.getUnitCost(1)).isEqualTo(42 / 8);
    }

    @Test
    void unit_cost_is_rounded_down() {
        EdgeWithLiquidityInformation edge1 = edge(EDGE, Coins.ofSatoshis(3));
        EdgeWithLiquidityInformation edge2 = edge(EDGE_1_3, Coins.ofSatoshis(20));
        arcInitializer.addArcs(List.of(edge1, edge2));
        assertThat(minCostFlow.getUnitCost(0)).isEqualTo(6L);
    }

    @Test
    void computes_unit_cost_based_on_maximum_capacity_without_combining_parallel_edges() {
        EdgeWithLiquidityInformation edge1 = edge(EDGE, Coins.ofSatoshis(2));
        EdgeWithLiquidityInformation edge2 = edge(EDGE_1_3, Coins.ofSatoshis(20));
        EdgeWithLiquidityInformation edge3 = edge(EDGE_1_3, Coins.ofSatoshis(10));
        arcInitializer.addArcs(List.of(edge1, edge2, edge3));
        assertThat(minCostFlow.getUnitCost(0)).isEqualTo(10L);
    }

    @Test
    void computes_unit_cost_with_fee_rate() {
        int feeRateFactor = 1;
        ArcInitializer arcInitializer = new ArcInitializer(
                minCostFlow,
                integerMapping,
                edgeMapping,
                QUANTIZATION,
                PIECEWISE_LINEAR_APPROXIMATIONS,
                feeRateFactor
        );
        EdgeWithLiquidityInformation edge = edge(EDGE, Coins.ofSatoshis(2_000_000));
        arcInitializer.addArcs(List.of(edge));
        assertThat(minCostFlow.getUnitCost(0)).isEqualTo(201);
    }

    private EdgeWithLiquidityInformation edge(Edge edge, Coins capacity) {
        Edge edgeWithCapacity = edge.withCapacity(capacity);
        return EdgeWithLiquidityInformation.forUpperBound(edgeWithCapacity, capacity);
    }
}
