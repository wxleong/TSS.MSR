package test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import samples.CmdLine;

public class TSSMainTests {
    @Disabled("Need administrator access and behavior varies across different TPM")
    @Test
    public void main() {
        try {
            System.out.println("main");
            String[] args = {"tbs"};

            CmdLine.setArgs(args);

            SamplesTests s = new SamplesTests();
            s.doAll();

        } catch (Exception e) {
            Assertions.assertTrue(false);
        }
    }

    @Test
    public void clean() {
        try {
            System.out.println("main");
            String[] args = {"tbs", "clear"};

            CmdLine.setArgs(args);

            SamplesTests s = new SamplesTests();
            s.doClean();

        } catch (Exception e) {
            Assertions.assertTrue(false);
        }
    }
}