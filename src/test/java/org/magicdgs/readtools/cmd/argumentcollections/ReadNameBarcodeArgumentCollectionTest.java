package org.magicdgs.readtools.cmd.argumentcollections;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Gomez-Sanchez (magicDGS)
 */
public class ReadNameBarcodeArgumentCollectionTest {

    @DataProvider
    public Object[][] encodingData() throws Exception {
        return new Object[][] {
                {ReadNameBarcodeArgumentCollection.ReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/1",
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/1"},
                {ReadNameBarcodeArgumentCollection.ReadNameEncoding.ILLUMINA,
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/2",
                        "@HWUSI-EAS100R:6:73:941:1973#ACTG/2"},
                {ReadNameBarcodeArgumentCollection.ReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 1:N:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/1"},
                {ReadNameBarcodeArgumentCollection.ReadNameEncoding.CASAVA,
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573 2:N:0:NTGATTAC",
                        "@ST-E00169:175:HMTL3CCXX:7:1101:3457:1573#NTGATTAC/2"}
        };
    }

    @Test(dataProvider = "encodingData")
    public void testNormalizeName(final ReadNameBarcodeArgumentCollection.ReadNameEncoding encoding,
            final String readName, final String expectedNormalize) throws Exception {
        Assert.assertEquals(encoding.normalizeReadName(readName), expectedNormalize);
    }

}