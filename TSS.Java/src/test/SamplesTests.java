package test;

import samples.CmdLine;
import samples.DrsClient;
import samples.Samples;
import tss.*;
import tss.tpm.*;
import java.io.IOException;

public class SamplesTests extends Samples
{
    public SamplesTests()
    {
        super();
    }

    public void doAll(String[] args) 
    {
        // Remove dangling TPM handles in case the previous run was prematurely terminated
        cleanSlots(TPM_HT.TRANSIENT);
        cleanSlots(TPM_HT.LOADED_SESSION);
        
        DrsClient.runProvisioningSequence(tpm);

        random();
        hash();
        hmac();
        getCapability();
        pcr1();
        primaryKeys();
        childKeys();
        encryptDecrypt();
        ek();
        ek2();
        quote();
        nv();
        //duplication();
        softwareKeys();
        softwareECCKeys();
        if (!usesTbs)
            locality();
        counterTimer();
        assert(allSlotsEmpty());
        
        DrsClient.runProvisioningSequence(tpm);
        assert(allSlotsEmpty());

        try 
        {
            tpm.close();
        } catch (IOException e) 
        {
            // don't care...
        }
    }

}
