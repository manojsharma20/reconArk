package io.reconark.plugins.recon;

import io.reconark.kernel.api.ReconArkPlugin;
import io.reconark.spi.FieldComparator;
import io.reconark.testing.tck.FieldComparatorTck;
import io.reconark.testing.tck.PluginContractTck;
import java.util.Map;
import org.junit.jupiter.api.Nested;

class ReconStandardPluginTest extends PluginContractTck {

    @Override
    protected ReconArkPlugin plugin() {
        return new ReconStandardPlugin();
    }

    @Nested
    class Exact extends FieldComparatorTck {
        @Override
        protected FieldComparator comparator() {
            return new ExactComparator();
        }

        @Override
        protected Object[] equalPair() {
            return new Object[] {"TX-1", "TX-1"};
        }

        @Override
        protected Object[] differentPair() {
            return new Object[] {"TX-1", "tx-1"};
        }
    }

    @Nested
    class CaseInsensitive extends FieldComparatorTck {
        @Override
        protected FieldComparator comparator() {
            return new CaseInsensitiveComparator();
        }

        @Override
        protected Object[] equalPair() {
            return new Object[] {"Pos ", "POS"};
        }

        @Override
        protected Object[] differentPair() {
            return new Object[] {"POS", "ECOM"};
        }
    }

    @Nested
    class NumericTolerance extends FieldComparatorTck {
        @Override
        protected FieldComparator comparator() {
            return new NumericToleranceComparator();
        }

        @Override
        protected Map<String, Object> parameters() {
            return Map.of("scale", 2, "absolute", "0.01");
        }

        @Override
        protected Object[] equalPair() {
            return new Object[] {"125.505", "125.51"};
        }

        @Override
        protected Object[] differentPair() {
            return new Object[] {"125.50", "125.60"};
        }
    }

    @Nested
    class DateWindow extends FieldComparatorTck {
        @Override
        protected FieldComparator comparator() {
            return new DateWindowComparator();
        }

        @Override
        protected Object[] equalPair() {
            // 22:30 UTC on 30 Sep is 02:30 on 1 Oct in Dubai: same business day
            return new Object[] {"2026-10-01", "2026-09-30T22:30:00Z"};
        }

        @Override
        protected Object[] differentPair() {
            return new Object[] {"2026-10-01", "2026-10-02T08:00:00+04:00"};
        }
    }

    @Nested
    class ValueMap extends FieldComparatorTck {
        @Override
        protected FieldComparator comparator() {
            return new ValueMapComparator();
        }

        @Override
        protected Map<String, Object> parameters() {
            return Map.of("mapping", Map.of("00", "SUCCESS", "05", "DECLINED"));
        }

        @Override
        protected Object[] equalPair() {
            return new Object[] {"SUCCESS", "00"};
        }

        @Override
        protected Object[] differentPair() {
            return new Object[] {"SUCCESS", "05"};
        }
    }
}
