package test;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import samples.CmdLine;

public class TSSMainTests {
    @Test
    public void main() {
        try {
            System.out.println("main");
            String[] args = {"tbs"};

            CmdLine.setArgs(args);

            SamplesTests s = new SamplesTests();
            s.doAll(args);

        } catch (Exception e) {
            Assertions.assertTrue(false);
        }
    }
}