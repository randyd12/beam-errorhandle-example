package errorfilteringdemo;

import errorfilteringdemo.datum.Failure;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionTuple;
import org.apache.beam.sdk.values.TupleTag;
import errorfilteringdemo.datum.Audit;
import errorfilteringdemo.helpers.MiscHelpers;
import org.apache.beam.sdk.values.TupleTagList;


public class Raw2Audit extends DoFn<String, Audit> {

    public static TupleTag<Audit> validTag = new TupleTag<Audit>(){};
    public static TupleTag<String> failuresTag = new TupleTag<String>(){};

    public static PCollectionTuple process(PCollection<String> logStrings) {
        return logStrings.apply("Create PubSub objects", ParDo.of(new DoFn<String, Audit>() {
            @ProcessElement
            public void processElement(ProcessContext c) {
                String line = c.element();
                Audit auditd = new Audit();

                try {
                    auditd.type = MiscHelpers.regexHelper("type=(.*?)\\s", line);
                    auditd.pid = Integer.parseInt(MiscHelpers.regexHelper("pid=(.*?)\\s", line));
                    auditd.uid = Integer.parseInt(MiscHelpers.regexHelper("uid=(.*?)\\s", line));
                    // Max Integer value is 2,147,483,647. ;)
                    auditd.auid = Integer.parseInt(MiscHelpers.regexHelper("auid=(.*?)\\s", line));
                }
                catch (Throwable throwable) {
                    Failure failure = new Failure(auditd, throwable);
                    c.output(failuresTag, failure.toString());  // TODO: replace with objects.
                }

                c.output(auditd);
            }
        }).withOutputTags(
                validTag,
                TupleTagList.of(failuresTag)
        ));
    }

}
