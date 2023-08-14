package mds;

import de.uni_leipzig.mds.io.ResultWriter;
import de.uni_leipzig.mds.io.proto.generated.rule_evaluation_result.RuleEvaluationResultProtoBuf;

public class AkiConsoleWriter implements ResultWriter {
    @Override
    public void write(RuleEvaluationResultProtoBuf result) {
        System.out.println(result.toString());
    }
}
