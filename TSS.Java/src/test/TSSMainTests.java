package test;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class TSSMainTests {
    @Test
    public void main() {
        try {
            System.out.println("main");
            String[] args = {"tbs"};

            SamplesTests s = new SamplesTests();
            s.doAll(args);

        } catch (Exception e) {
            Assertions.assertTrue(false);
        }
    }
}