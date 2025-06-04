package csv.procedure;

import java.util.Comparator;

public final class ProcedureDataTimeComparator implements Comparator<ProcedureData> {
    @Override
    public int compare(ProcedureData o1, ProcedureData o2) {
        return o1.getPerformedOn().compareTo(o2.getPerformedOn());
    }
}
